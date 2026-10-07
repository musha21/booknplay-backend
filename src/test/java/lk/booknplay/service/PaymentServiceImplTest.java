package lk.booknplay.service;

import lk.booknplay.dto.response.PaymentResponse;
import lk.booknplay.entity.Booking;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.Customer;
import lk.booknplay.entity.Payment;
import lk.booknplay.entity.Sport;
import lk.booknplay.entity.User;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.BookingSource;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.enums.PaymentStatus;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.repository.BookingRepository;
import lk.booknplay.repository.CustomerRepository;
import lk.booknplay.repository.InvoiceRepository;
import lk.booknplay.repository.PaymentRepository;
import lk.booknplay.service.impl.PaymentServiceImpl;
import lk.booknplay.util.PayHereHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository payments;
    @Mock private BookingRepository bookings;
    @Mock private CustomerRepository customers;
    @Mock private InvoiceRepository invoices;
    @Mock private NotificationService notifications;
    @Mock private PayHereRefundClient refundClient;
    @Mock private OwnerSubscriptionService ownerSubscriptions;

    private final PayHereCheckoutStore checkoutStore = new PayHereCheckoutStore();

    private Customer customer;
    private Booking booking;
    private Payment payment;
    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        User user = User.builder().id("user-1").email("player@example.com").build();
        customer = Customer.builder().id("cust-1").user(user).firstName("Amal").lastName("Perera").phone("0771234567").build();
        Venue venue = Venue.builder().id("venue-1").name("Kandy Arena").build();
        Sport sport = Sport.builder().id("sport-1").name("Futsal").build();
        Court court = Court.builder().id("court-1").name("Court A").venue(venue).sport(sport).build();
        booking = Booking.builder()
                .id("book-1")
                .bookingRef("BNP-TEST")
                .customer(customer)
                .venue(venue)
                .court(court)
                .sport(sport)
                .bookingDate(LocalDate.of(2026, 10, 10))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .totalAmount(new BigDecimal("2500.00"))
                .status(BookingStatus.PENDING)
                .contactName("Amal Perera")
                .contactEmail("player@example.com")
                .contactPhone("0771234567")
                .build();
        payment = Payment.builder()
                .id("pay-1")
                .booking(booking)
                .customer(customer)
                .amount(new BigDecimal("2500.00"))
                .currency("LKR")
                .status(PaymentStatus.INITIATED)
                .build();
        service = new PaymentServiceImpl(
                payments, bookings, customers, invoices, notifications, checkoutStore, refundClient,
                ownerSubscriptions,
                "PAYHERE",
                "http://localhost:8080",
                "1238375",
                "test-secret",
                "https://sandbox.payhere.lk/pay/checkout",
                "http://localhost:5173/payment/return",
                "http://localhost:5173/payment/return",
                "http://localhost:8080/api/v1/webhook/payhere/notify"
        );
    }

    @Test
    void payhereInitiate_staysProcessingWithCheckoutUrl() {
        when(customers.findByUser_Email("player@example.com")).thenReturn(Optional.of(customer));
        when(bookings.findById("book-1")).thenReturn(Optional.of(booking));
        when(payments.findByBookingId("book-1")).thenReturn(Optional.of(payment));
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.initiatePayment("player@example.com", "book-1", "PAYHERE");

        assertEquals(PaymentStatus.PROCESSING, response.getStatus());
        assertEquals("PAYHERE", response.getPaymentGateway());
        assertNotNull(response.getPaymentUrl());
        assertTrue(response.getPaymentUrl().contains("/api/v1/public/payhere/checkout/"));
        assertEquals(BookingStatus.PENDING, booking.getStatus());
        verify(invoices, never()).save(any());
        verify(notifications, never()).sendBookingNotification(any(), any(), any());
    }

    @Test
    void payhereInitiate_rejectsWalkInBooking() {
        booking.setSource(BookingSource.WALK_IN);
        booking.setCustomer(null);
        booking.setStatus(BookingStatus.CONFIRMED);
        when(customers.findByUser_Email("player@example.com")).thenReturn(Optional.of(customer));
        when(bookings.findById("book-1")).thenReturn(Optional.of(booking));

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.initiatePayment("player@example.com", "book-1", "PAYHERE"));

        assertTrue(error.getMessage().toLowerCase().contains("walk-in"));
        verify(payments, never()).save(any());
    }

    @Test
    void payhereNotify_rejectsBadSignature() {
        assertThrows(BadRequestException.class, () -> service.processPayHereNotify(Map.of(
                "merchant_id", "1238375",
                "order_id", "BNP-TEST",
                "payhere_amount", "2500.00",
                "payhere_currency", "LKR",
                "status_code", "2",
                "md5sig", "DEADBEEF"
        )));
    }

    @Test
    void payhereNotify_confirmsOnValidSuccess() {
        when(bookings.findByBookingRef("BNP-TEST")).thenReturn(Optional.of(booking));
        when(payments.findByBookingId("book-1")).thenReturn(Optional.of(payment));
        when(invoices.findByBookingId("book-1")).thenReturn(Optional.empty());
        when(bookings.existsOverlappingBookingExcluding(
                eq("court-1"), any(), any(), any(), eq("book-1"))).thenReturn(false);
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String md5sig = PayHereHash.notifySignature(
                "1238375", "BNP-TEST", "2500.00", "LKR", "2", "test-secret");

        PaymentResponse response = service.processPayHereNotify(Map.of(
                "merchant_id", "1238375",
                "order_id", "BNP-TEST",
                "payment_id", "320027150501",
                "payhere_amount", "2500.00",
                "payhere_currency", "LKR",
                "status_code", "2",
                "md5sig", md5sig
        ));

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(invoices).save(any());
        verify(notifications).sendBookingNotification(customer, booking, "BOOKING_CONFIRMED");
    }

    @Test
    void payhereNotify_failStatusMarksBookingFailed() {
        when(bookings.findByBookingRef("BNP-TEST")).thenReturn(Optional.of(booking));
        when(payments.findByBookingId("book-1")).thenReturn(Optional.of(payment));
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String md5sig = PayHereHash.notifySignature(
                "1238375", "BNP-TEST", "2500.00", "LKR", "-1", "test-secret");

        PaymentResponse response = service.processPayHereNotify(Map.of(
                "merchant_id", "1238375",
                "order_id", "BNP-TEST",
                "payment_id", "320027150501",
                "payhere_amount", "2500.00",
                "payhere_currency", "LKR",
                "status_code", "-1",
                "md5sig", md5sig
        ));

        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertEquals(BookingStatus.FAILED, booking.getStatus());
        verify(notifications, never()).sendBookingNotification(any(), any(), any());
        verify(refundClient, never()).refund(any(), any(), any(), any());
    }

    @Test
    void payhereNotify_conflictFailsBookingAndRefunds() {
        when(bookings.findByBookingRef("BNP-TEST")).thenReturn(Optional.of(booking));
        when(payments.findByBookingId("book-1")).thenReturn(Optional.of(payment));
        when(bookings.existsOverlappingBookingExcluding(
                eq("court-1"), any(), any(), any(), eq("book-1"))).thenReturn(true);
        when(refundClient.refund(eq("320027150501"), any(), any(), any())).thenReturn("REF-1");
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String md5sig = PayHereHash.notifySignature(
                "1238375", "BNP-TEST", "2500.00", "LKR", "2", "test-secret");

        PaymentResponse response = service.processPayHereNotify(Map.of(
                "merchant_id", "1238375",
                "order_id", "BNP-TEST",
                "payment_id", "320027150501",
                "payhere_amount", "2500.00",
                "payhere_currency", "LKR",
                "status_code", "2",
                "md5sig", md5sig
        ));

        assertEquals(BookingStatus.FAILED, booking.getStatus());
        assertEquals(PaymentStatus.REFUNDED, response.getStatus());
        verify(refundClient).refund(eq("320027150501"), any(), any(), any());
        verify(invoices, never()).save(any());
        verify(notifications, never()).sendBookingNotification(any(), any(), any());
    }
}
