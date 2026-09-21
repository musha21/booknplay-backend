package lk.booknplay.service;

import lk.booknplay.dto.response.PaymentResponse;
import lk.booknplay.entity.Booking;
import lk.booknplay.entity.Customer;
import lk.booknplay.entity.Payment;
import lk.booknplay.entity.User;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.enums.PaymentStatus;
import lk.booknplay.repository.BookingRepository;
import lk.booknplay.repository.CustomerRepository;
import lk.booknplay.repository.InvoiceRepository;
import lk.booknplay.repository.PaymentRepository;
import lk.booknplay.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
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

    private Customer customer;
    private Booking booking;
    private Payment payment;

    @BeforeEach
    void setUp() {
        User user = User.builder().id("user-1").email("player@example.com").build();
        customer = Customer.builder().id("cust-1").user(user).firstName("Amal").lastName("Perera").build();
        booking = Booking.builder()
                .id("book-1")
                .bookingRef("BNP-TEST")
                .customer(customer)
                .totalAmount(new BigDecimal("2500"))
                .status(BookingStatus.PENDING)
                .build();
        payment = Payment.builder()
                .id("pay-1")
                .booking(booking)
                .customer(customer)
                .amount(new BigDecimal("2500"))
                .currency("LKR")
                .status(PaymentStatus.INITIATED)
                .build();
    }

    @Test
    void dummyInitiate_confirmsBookingAndCreatesInvoice() {
        PaymentServiceImpl service = new PaymentServiceImpl(payments, bookings, customers, invoices, notifications, "DUMMY");
        when(customers.findByUser_Email("player@example.com")).thenReturn(Optional.of(customer));
        when(bookings.findById("book-1")).thenReturn(Optional.of(booking));
        when(payments.findByBookingId("book-1")).thenReturn(Optional.of(payment));
        when(invoices.findByBookingId("book-1")).thenReturn(Optional.empty());
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.initiatePayment("player@example.com", "book-1", "PAYHERE");

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals("DUMMY", response.getPaymentGateway());
        assertEquals("DEV-BNP-TEST", response.getGatewayReference());
        assertNull(response.getPaymentUrl());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(invoices).save(any());
        verify(notifications).sendBookingNotification(customer, booking, "BOOKING_CONFIRMED");
    }

    @Test
    void payhereInitiate_staysProcessingWithoutInvoice() {
        PaymentServiceImpl service = new PaymentServiceImpl(payments, bookings, customers, invoices, notifications, "PAYHERE");
        when(customers.findByUser_Email("player@example.com")).thenReturn(Optional.of(customer));
        when(bookings.findById("book-1")).thenReturn(Optional.of(booking));
        when(payments.findByBookingId("book-1")).thenReturn(Optional.of(payment));
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.initiatePayment("player@example.com", "book-1", "PAYHERE");

        assertEquals(PaymentStatus.PROCESSING, response.getStatus());
        assertEquals("PAYHERE", response.getPaymentGateway());
        assertNotNull(response.getPaymentUrl());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
        verify(invoices, never()).save(any());
        verify(notifications, never()).sendBookingNotification(any(), any(), any());
    }
}
