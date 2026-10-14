package lk.booknplay.service;

import lk.booknplay.dto.request.AdminPayoutSettleRequest;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.Payout;
import lk.booknplay.enums.BankVerificationStatus;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayoutPaidSmsTest {

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
    private PayoutSettlementServiceImpl payoutSettlementService;

    @Test
    void settle_SendsSmsToBusinessContactPhone() {
        Business business = Business.builder()
                .id("biz-1")
                .name("Court Co")
                .contactPhone("+94771234567")
                .bankVerificationStatus(BankVerificationStatus.VERIFIED)
                .build();
        Payout payout = Payout.builder()
                .id("payout-1")
                .business(business)
                .periodStart(LocalDate.of(2026, 9, 28))
                .periodEnd(LocalDate.of(2026, 10, 4))
                .frequency(PayoutFrequency.WEEKLY)
                .gross(new BigDecimal("100000.00"))
                .refundsTotal(BigDecimal.ZERO)
                .commission(new BigDecimal("10000.00"))
                .gatewayFees(BigDecimal.ZERO)
                .net(new BigDecimal("90000.00"))
                .status(PayoutStatus.PROCESSING)
                .build();
        when(payoutRepository.findById("payout-1")).thenReturn(Optional.of(payout));
        when(payoutRepository.save(any(Payout.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payoutItemRepository.findByPayoutIdOrderByCreatedAtAsc("payout-1")).thenReturn(java.util.List.of());
        when(auditLogs.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AdminPayoutSettleRequest request = AdminPayoutSettleRequest.builder()
                .transferReference("TRX-99")
                .transferDate(LocalDate.of(2026, 10, 12))
                .transferAmount(new BigDecimal("90000.00"))
                .build();

        payoutSettlementService.settle("admin@booknplay.lk", "payout-1", request, null);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsLenzService).sendSms(eq("+94771234567"), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("90000"));
        assertTrue(messageCaptor.getValue().contains("TRX-99"));
        assertTrue(messageCaptor.getValue().contains("paid"));
    }
}
