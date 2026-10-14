package lk.booknplay.service.impl;

import lk.booknplay.entity.Booking;
import lk.booknplay.entity.Payment;
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
import java.util.Optional;

/**
 * Releases unpaid soft-holds so abandoned PayHere checkouts free the court again.
 * Never downgrades a booking that already has a successful payment (notify vs expiry race).
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
            @Value("${booknplay.booking.hold-ttl-minutes:10}") int holdTtlMinutes) {
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
        int skippedPaid = 0;
        for (Booking booking : expired) {
            Optional<Payment> paymentOpt = paymentRepository.findByBookingId(booking.getId());
            if (paymentOpt.isPresent()) {
                PaymentStatus payStatus = paymentOpt.get().getStatus();
                if (payStatus == PaymentStatus.SUCCESS
                        || payStatus == PaymentStatus.PAID
                        || payStatus == PaymentStatus.REFUNDED
                        || payStatus == PaymentStatus.PARTIALLY_REFUNDED) {
                    skippedPaid++;
                    log.warn(
                            "Skipping hold expiry for booking {} — payment already {}",
                            booking.getBookingRef(),
                            payStatus
                    );
                    continue;
                }
            }

            int updated = bookingRepository.markFailedIfPending(booking.getId());
            if (updated == 0) {
                continue;
            }
            paymentOpt.ifPresent(payment -> {
                if (payment.getStatus() == PaymentStatus.INITIATED
                        || payment.getStatus() == PaymentStatus.PROCESSING) {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                }
            });
            released++;
        }
        if (released > 0 || skippedPaid > 0) {
            log.info(
                    "Expired {} unpaid PENDING hold(s); skipped {} paid; TTL={} minutes",
                    released,
                    skippedPaid,
                    holdTtlMinutes
            );
        }
    }

    /** Package-visible for unit tests. */
    int holdTtlMinutes() {
        return holdTtlMinutes;
    }
}
