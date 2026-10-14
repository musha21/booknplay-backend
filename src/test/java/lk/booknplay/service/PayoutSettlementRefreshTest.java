package lk.booknplay.service;

import lk.booknplay.dto.response.AdminPayoutBusinessResponse;
import lk.booknplay.entity.Booking;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.Payment;
import lk.booknplay.entity.Payout;
import lk.booknplay.entity.PayoutItem;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.BookingSource;
import lk.booknplay.enums.PaymentStatus;
import lk.booknplay.enums.PayoutFrequency;
import lk.booknplay.enums.PayoutStatus;
import lk.booknplay.repository.AdminAuditLogRepository;
import lk.booknplay.repository.BusinessRepository;
import lk.booknplay.repository.PaymentRepository;
import lk.booknplay.repository.PayoutItemRepository;
import lk.booknplay.repository.PayoutRepository;
import lk.booknplay.repository.PlatformPayoutConfigRepository;
import lk.booknplay.repository.RefundRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.service.impl.PayoutSettlementServiceImpl;
import lk.booknplay.util.PayoutPeriodCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayoutSettlementRefreshTest {

    @Mock private BusinessRepository businessRepository;
    @Mock private PayoutRepository payoutRepository;
    @Mock private PayoutItemRepository payoutItemRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private PlatformPayoutConfigRepository configRepository;
    @Mock private AdminAuditLogRepository auditLogs;
    @Mock private UserRepository userRepository;
    @Mock private OwnerAccessService ownerAccessService;
    @Mock private PlanEntitlementService planEntitlementService;
    @Mock private FileStorageService fileStorageService;
    @Mock private SmsLenzService smsLenzService;

    @InjectMocks
    private PayoutSettlementServiceImpl service;

    private Business business;
    private LocalDate asOf;
    private PayoutPeriodCalculator.Period current;

    @BeforeEach
    void setUp() {
        asOf = LocalDate.of(2026, 10, 8); // Wednesday
        current = PayoutPeriodCalculator.currentPeriod(PayoutFrequency.WEEKLY, asOf);
        business = Business.builder()
                .id("biz-1")
                .name("Court Co")
                .ownerId("owner-1")
                .payoutFrequency(PayoutFrequency.WEEKLY)
                .commissionPercent(new BigDecimal("10.00"))
                .build();
    }

    @Test
    void generateSettlements_appendsNewPaymentOntoOpenCurrentPeriodPayout() {
        when(businessRepository.findByPayoutFrequency(PayoutFrequency.WEEKLY)).thenReturn(List.of(business));
        when(businessRepository.findByPayoutFrequency(PayoutFrequency.MONTHLY)).thenReturn(List.of());

        Payout open = Payout.builder()
                .id("payout-open")
                .business(business)
                .periodStart(current.start())
                .periodEnd(current.end())
                .frequency(PayoutFrequency.WEEKLY)
                .gross(new BigDecimal("3000.00"))
                .refundsTotal(BigDecimal.ZERO)
                .commission(new BigDecimal("300.00"))
                .gatewayFees(BigDecimal.ZERO)
                .net(new BigDecimal("2700.00"))
                .status(PayoutStatus.PENDING)
                .build();

        Venue venue = Venue.builder().id("v1").business(business).name("Arena").build();
        Booking booking = Booking.builder().id("book-2").venue(venue).source(BookingSource.ONLINE).build();
        Payment newPayment = Payment.builder()
                .id("pay-2")
                .booking(booking)
                .amount(new BigDecimal("2000.00"))
                .status(PaymentStatus.SUCCESS)
                .updatedAt(LocalDateTime.of(2026, 10, 8, 12, 0))
                .build();

        // completed period: none
        when(payoutRepository.findByBusinessIdAndPeriodStartAndPeriodEnd(
                eq("biz-1"), any(), any())).thenAnswer(inv -> {
            LocalDate start = inv.getArgument(1);
            if (start.equals(current.start())) {
                return Optional.of(open);
            }
            return Optional.empty();
        });
        when(paymentRepository.findEligibleForPayout(
                eq("biz-1"), eq(BookingSource.ONLINE), eq(PaymentStatus.SUCCESS), any(), any()))
                .thenAnswer(inv -> {
                    LocalDateTime from = inv.getArgument(3);
                    if (from.toLocalDate().equals(current.start())) {
                        return List.of(newPayment);
                    }
                    return List.of();
                });
        when(payoutItemRepository.findSettledPaymentIds(anyCollection())).thenReturn(Set.of());
        when(refundRepository.findSucceededByPaymentIds(any())).thenReturn(List.of());
        when(refundRepository.findSucceededForBusinessInWindow(eq("biz-1"), any(), any())).thenReturn(List.of());
        when(payoutItemRepository.findByPayoutIdOrderByCreatedAtAsc("payout-open")).thenReturn(List.of(
                PayoutItem.builder()
                        .id("item-1")
                        .paymentId("pay-1")
                        .bookingId("book-1")
                        .gross(new BigDecimal("3000.00"))
                        .refund(BigDecimal.ZERO)
                        .commission(new BigDecimal("300.00"))
                        .gatewayFee(BigDecimal.ZERO)
                        .net(new BigDecimal("2700.00"))
                        .adjustment(false)
                        .build()
        ));
        when(payoutItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(payoutRepository.save(any(Payout.class))).thenAnswer(inv -> inv.getArgument(0));

        int changed = service.generateSettlements(asOf);

        assertTrue(changed >= 1);
        ArgumentCaptor<Payout> payoutCaptor = ArgumentCaptor.forClass(Payout.class);
        verify(payoutRepository).save(payoutCaptor.capture());
        Payout saved = payoutCaptor.getValue();
        assertEquals(0, new BigDecimal("5000.00").compareTo(saved.getGross()));
        assertEquals(0, new BigDecimal("500.00").compareTo(saved.getCommission()));
        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getGatewayFees()));
        assertEquals(0, new BigDecimal("4500.00").compareTo(saved.getNet()));
    }

    @Test
    void generateSettlements_doesNotMutatePaidPayout() {
        when(businessRepository.findByPayoutFrequency(PayoutFrequency.WEEKLY)).thenReturn(List.of(business));
        when(businessRepository.findByPayoutFrequency(PayoutFrequency.MONTHLY)).thenReturn(List.of());

        Payout paid = Payout.builder()
                .id("payout-paid")
                .business(business)
                .periodStart(current.start())
                .periodEnd(current.end())
                .status(PayoutStatus.PAID)
                .net(new BigDecimal("2700.00"))
                .build();

        when(payoutRepository.findByBusinessIdAndPeriodStartAndPeriodEnd(eq("biz-1"), any(), any()))
                .thenReturn(Optional.of(paid));

        int changed = service.generateSettlements(asOf);

        assertEquals(0, changed);
        verify(payoutItemRepository, never()).saveAll(any());
        verify(payoutRepository, never()).save(any());
    }

    @Test
    void listBusinesses_includesUnsettledAccruedAndCommission() {
        when(businessRepository.findAll()).thenReturn(List.of(business));
        when(payoutRepository.sumNetByBusinessAndStatuses(eq("biz-1"), any())).thenReturn(BigDecimal.ZERO);
        when(payoutRepository.findByBusinessIdOrderByPeriodStartDesc("biz-1")).thenReturn(List.of());
        when(userRepository.findById("owner-1")).thenReturn(Optional.empty());

        Venue venue = Venue.builder().id("v1").business(business).name("Arena").build();
        Booking booking = Booking.builder().id("book-1").venue(venue).source(BookingSource.ONLINE).build();
        Payment payment = Payment.builder()
                .id("pay-1")
                .booking(booking)
                .amount(new BigDecimal("1000.00"))
                .status(PaymentStatus.SUCCESS)
                .build();
        when(paymentRepository.findByBusinessSourceAndStatus(
                "biz-1", BookingSource.ONLINE, PaymentStatus.SUCCESS)).thenReturn(List.of(payment));
        when(payoutItemRepository.findSettledPaymentIds(anyCollection())).thenReturn(Set.of());
        when(refundRepository.findSucceededByPaymentIds(any())).thenReturn(List.of());

        List<AdminPayoutBusinessResponse> rows = service.listBusinesses();

        assertEquals(1, rows.size());
        assertEquals(0, new BigDecimal("900.00").compareTo(rows.get(0).getUnsettledAccrued()));
        assertEquals(0, new BigDecimal("10.00").compareTo(rows.get(0).getCommissionPercent()));
        assertEquals(0, BigDecimal.ZERO.compareTo(rows.get(0).getOutstandingBalance()));
    }
}
