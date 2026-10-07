package lk.booknplay.service.impl;

import lk.booknplay.entity.Booking;
import lk.booknplay.entity.Payment;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.enums.PaymentStatus;
import lk.booknplay.repository.BookingRepository;
import lk.booknplay.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingHoldExpiryJobTest {

    @Mock private BookingRepository bookings;
    @Mock private PaymentRepository payments;

    private BookingHoldExpiryJob job;

    @BeforeEach
    void setUp() {
        job = new BookingHoldExpiryJob(bookings, payments, 5);
    }

    @Test
    void expireUnpaidHoldsMarksPendingBookingAndOpenPaymentFailed() {
        Booking booking = Booking.builder()
                .id("book-old")
                .bookingRef("BNP-OLD")
                .status(BookingStatus.PENDING)
                .totalAmount(new BigDecimal("1000.00"))
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();
        Payment payment = Payment.builder()
                .id("pay-old")
                .booking(booking)
                .amount(new BigDecimal("1000.00"))
                .currency("LKR")
                .status(PaymentStatus.PROCESSING)
                .build();

        when(bookings.findByStatusAndCreatedAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(payments.findByBookingId("book-old")).thenReturn(Optional.of(payment));
        when(bookings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        job.expireUnpaidHolds();

        assertEquals(BookingStatus.FAILED, booking.getStatus());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(bookings).save(booking);
        verify(payments).save(payment);
    }

    @Test
    void expireUnpaidHoldsDoesNotDowngradeSuccessfulPayment() {
        Booking booking = Booking.builder()
                .id("book-paid")
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();
        Payment payment = Payment.builder()
                .id("pay-ok")
                .booking(booking)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(bookings.findByStatusAndCreatedAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(payments.findByBookingId("book-paid")).thenReturn(Optional.of(payment));
        when(bookings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        job.expireUnpaidHolds();

        assertEquals(BookingStatus.FAILED, booking.getStatus());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        verify(payments, never()).save(any());
    }

    @Test
    void holdTtlDefaultsToConfiguredMinutes() {
        assertEquals(5, job.holdTtlMinutes());
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        when(bookings.findByStatusAndCreatedAtBefore(eq(BookingStatus.PENDING), cutoff.capture()))
                .thenReturn(List.of());

        LocalDateTime before = LocalDateTime.now().minusMinutes(5).minusSeconds(2);
        job.expireUnpaidHolds();
        LocalDateTime after = LocalDateTime.now().minusMinutes(5).plusSeconds(2);

        LocalDateTime used = cutoff.getValue();
        assertTrue(!used.isBefore(before));
        assertTrue(!used.isAfter(after));
    }
}
