package lk.booknplay.service.impl;

import lk.booknplay.entity.Booking;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.enums.PaymentStatus;
import lk.booknplay.repository.BookingRepository;
import lk.booknplay.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Releases unpaid soft-holds so abandoned PayHere checkouts free the court again.
 */
@Component
public class BookingHoldExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(BookingHoldExpiryJob.class);

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final int holdTtlMinutes;

    public BookingHoldExpiryJob(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            @Value("${booknplay.booking.hold-ttl-minutes:5}") int holdTtlMinutes) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.holdTtlMinutes = Math.max(1, holdTtlMinutes);
    }

    @Scheduled(fixedDelayString = "${booknplay.booking.hold-expiry-interval-ms:60000}")
    @Transactional
    public void expireUnpaidHolds() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(holdTtlMinutes);
        List<Booking> expired = bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.PENDING, cutoff);
        if (expired.isEmpty()) {
            return;
        }
        int released = 0;
        for (Booking booking : expired) {
            booking.setStatus(BookingStatus.FAILED);
            bookingRepository.save(booking);
            paymentRepository.findByBookingId(booking.getId()).ifPresent(payment -> {
                if (payment.getStatus() == PaymentStatus.INITIATED
                        || payment.getStatus() == PaymentStatus.PROCESSING) {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                }
            });
            released++;
        }
        log.info("Expired {} unpaid PENDING booking hold(s) older than {} minutes", released, holdTtlMinutes);
    }

    /** Package-visible for unit tests. */
    int holdTtlMinutes() {
        return holdTtlMinutes;
    }
}
