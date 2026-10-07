package lk.booknplay.service;

import lk.booknplay.dto.response.BookingCancellationResponse;
import lk.booknplay.dto.response.CancellationPreviewResponse;
import lk.booknplay.entity.*;
import lk.booknplay.enums.BookingSource;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.enums.PaymentStatus;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.repository.*;
import lk.booknplay.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayHereRefundCancelTest {

    @Mock BookingRepository bookingRepository;
    @Mock CustomerRepository customerRepository;
    @Mock CourtRepository courtRepository;
    @Mock SportRepository sportRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock BookingQuoteRepository quoteRepository;
    @Mock RefundRepository refundRepository;
    @Mock CancellationPolicyRepository cancellationPolicyRepository;
    @Mock BookingRulesService bookingRulesService;
    @Mock NotificationService notificationService;
    @Mock PayHereRefundClient payHereRefundClient;
    @Mock com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock OwnerPortalService ownerPortalService;

    @InjectMocks BookingServiceImpl bookingService;

    private Customer customer;
    private Booking booking;
    private Payment payment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "paymentMode", "PAYHERE");

        User user = User.builder().id("user-1").email("maya@example.com").build();
        customer = Customer.builder().id("cust-1").user(user).firstName("Maya").lastName("Perera").build();

        Business business = Business.builder().id("biz-1").name("Arena").build();
        Venue venue = Venue.builder().id("venue-1").business(business).name("PlayArena").build();
        Sport sport = Sport.builder().id("sport-1").name("Badminton").build();
        Court court = Court.builder().id("court-1").venue(venue).sport(sport).name("Court 1").build();

        booking = Booking.builder()
                .id("booking-1")
                .bookingRef("BNP-20261001-ABC123")
                .customer(customer)
                .court(court)
                .venue(venue)
                .sport(sport)
                .bookingDate(LocalDate.now().plusDays(3))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .totalAmount(new BigDecimal("3000.00"))
                .status(BookingStatus.CONFIRMED)
                .cancellationAllowed(true)
                .cancellationDeadline(LocalDateTime.now().plusDays(2))
                .lateRefundPercentage(new BigDecimal("50"))
                .policySnapshotSource("CURRENT_BUSINESS_POLICY")
                .build();

        payment = Payment.builder()
                .id("pay-1")
                .booking(booking)
                .customer(customer)
                .amount(new BigDecimal("3000.00"))
                .currency("LKR")
                .paymentGateway("PAYHERE")
                .gatewayReference("320027150501")
                .status(PaymentStatus.SUCCESS)
                .build();
    }

    @Test
    void cancelIssuesFullPayHereRefundBeforeDeadline() {
        when(customerRepository.findByUser_Email("maya@example.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId("booking-1")).thenReturn(Optional.of(payment));
        when(refundRepository.findByBookingId("booking-1")).thenReturn(Optional.empty());
        when(payHereRefundClient.refund(
                eq("320027150501"),
                eq(new BigDecimal("3000.00")),
                eq(new BigDecimal("3000.00")),
                any()
        )).thenReturn("560034010257");
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> {
            Refund saved = inv.getArgument(0);
            when(refundRepository.findByBookingId("booking-1")).thenReturn(Optional.of(saved));
            return saved;
        });

        BookingCancellationResponse response = bookingService.cancelBooking("maya@example.com", "booking-1");

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertEquals("PAYHERE", response.getRefund().getMode());
        assertEquals("560034010257", response.getRefund().getReference());
        assertEquals(0, new BigDecimal("3000.00").compareTo(response.getRefundAmount()));

        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository).save(refundCaptor.capture());
        assertEquals("PAYHERE", refundCaptor.getValue().getMode());
        assertEquals("SUCCEEDED", refundCaptor.getValue().getStatus());
    }

    @Test
    void cancelIssuesPartialPayHereRefundAfterDeadline() {
        booking.setCancellationDeadline(LocalDateTime.now().minusHours(1));
        when(customerRepository.findByUser_Email("maya@example.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId("booking-1")).thenReturn(Optional.of(payment));
        when(refundRepository.findByBookingId("booking-1")).thenReturn(Optional.empty());
        when(payHereRefundClient.refund(
                eq("320027150501"),
                eq(new BigDecimal("1500.00")),
                eq(new BigDecimal("3000.00")),
                any()
        )).thenReturn("560034010999");
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> {
            Refund saved = inv.getArgument(0);
            when(refundRepository.findByBookingId("booking-1")).thenReturn(Optional.of(saved));
            return saved;
        });

        BookingCancellationResponse response = bookingService.cancelBooking("maya@example.com", "booking-1");

        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, payment.getStatus());
        assertEquals(0, new BigDecimal("1500.00").compareTo(response.getRefundAmount()));
        assertEquals("PAYHERE", response.getRefund().getMode());
    }

    @Test
    void cancelFailsAndLeavesBookingConfirmedWhenPayHereRefundFails() {
        when(customerRepository.findByUser_Email("maya@example.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId("booking-1")).thenReturn(Optional.of(payment));
        when(payHereRefundClient.refund(any(), any(), any(), any()))
                .thenThrow(new BadRequestException("PayHere refund failed. The booking was not cancelled."));

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> bookingService.cancelBooking("maya@example.com", "booking-1"));

        assertTrue(error.getMessage().contains("not cancelled"));
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(bookingRepository, never()).save(any());
        verify(refundRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void cancelFailsWhenGatewayReferenceMissing() {
        payment.setGatewayReference(null);
        when(customerRepository.findByUser_Email("maya@example.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId("booking-1")).thenReturn(Optional.of(payment));

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> bookingService.cancelBooking("maya@example.com", "booking-1"));

        assertTrue(error.getMessage().toLowerCase().contains("payment id"));
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(payHereRefundClient, never()).refund(any(), any(), any(), any());
    }

    @Test
    void previewDescribesPayHereRefund() {
        when(customerRepository.findByUser_Email("maya@example.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId("booking-1")).thenReturn(Optional.of(payment));

        CancellationPreviewResponse preview = bookingService.previewCancellation("maya@example.com", "booking-1");

        assertEquals("PAYHERE", preview.getRefundMode());
        assertTrue(preview.getRefundMethod().contains("PayHere"));
        assertTrue(preview.getMessage().toLowerCase().contains("payhere"));
    }

    @Test
    void ownerCancelWalkInMarksCancelledWithoutPayHereRefund() {
        booking.setCustomer(null);
        booking.setSource(BookingSource.WALK_IN);
        booking.setGuestName("Walk In Guest");
        booking.setGuestPhone("+94770001111");
        booking.setContactName("Walk In Guest");
        booking.setContactPhone("+94770001111");
        booking.setCancellationAllowed(null);
        booking.setCancellationDeadline(null);
        booking.setLateRefundPercentage(null);

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId("booking-1")).thenReturn(Optional.empty());
        when(refundRepository.findByBookingId("booking-1")).thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingCancellationResponse response = bookingService.cancelBookingAsOwner("booking-1", "Venue owner cancelled booking");

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals("OWNER", booking.getCancelledBy());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getRefundAmount()));
        assertEquals("NONE", response.getRefund().getMode());
        assertNull(booking.getCustomer());
        assertNull(response.getBooking().getPaymentStatus());
        verify(payHereRefundClient, never()).refund(any(), any(), any(), any());
        verify(refundRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }
}
