package lk.booknplay.service.impl;

import lk.booknplay.entity.Booking;
import lk.booknplay.entity.Customer;
import lk.booknplay.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public NotificationServiceImpl(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
        // #region agent log
        try {
            String payload = "{\"sessionId\":\"5b0412\",\"runId\":\"post-fix\",\"hypothesisId\":\"B\",\"location\":\"NotificationServiceImpl.java:<init>\",\"message\":\"notification ctor succeeded\",\"data\":{\"hasProvider\":" + (mailSenderProvider != null) + "},\"timestamp\":" + System.currentTimeMillis() + "}\n";
            java.nio.file.Files.writeString(java.nio.file.Path.of("c:/Users/Musharaf/Downloads/booknplay/booknplay/debug-5b0412.log"), payload, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
        // #endregion
    }

    @Override
    @Async
    public void sendBookingNotification(Customer customer, Booking booking, String eventType) {
        if (customer == null || customer.getUser() == null || customer.getUser().getEmail() == null) {
            log.warn("Cannot send notification: Customer or target email address is missing");
            return;
        }

        if (booking == null) {
            log.warn("Cannot send notification: Booking details are null");
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender is not configured. Skipping email notification to {}", customer.getUser().getEmail());
            return;
        }

        try {
            String recipientEmail = customer.getUser().getEmail();
            String firstName = customer.getFirstName() != null ? customer.getFirstName() : "Valued Customer";
            String bookingRef = booking.getBookingRef() != null ? booking.getBookingRef() : "N/A";
            String venueName = (booking.getVenue() != null && booking.getVenue().getName() != null) ? booking.getVenue().getName() : "Venue";
            String courtName = (booking.getCourt() != null && booking.getCourt().getName() != null) ? booking.getCourt().getName() : "Court";
            String bookingDate = booking.getBookingDate() != null ? booking.getBookingDate().toString() : "N/A";
            String startTime = booking.getStartTime() != null ? booking.getStartTime().toString() : "";
            String endTime = booking.getEndTime() != null ? booking.getEndTime().toString() : "";
            String totalAmount = booking.getTotalAmount() != null ? booking.getTotalAmount().toString() : "0";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject("BooknPlay.lk Notification: " + eventType);
            message.setText("Dear " + firstName + ",\n\n" +
                    "Your booking (" + bookingRef + ") at " + venueName +
                    " for " + courtName + " on " + bookingDate +
                    " (" + startTime + " - " + endTime + ") has event: " + eventType + ".\n\n" +
                    "Total Amount: LKR " + totalAmount + "\n\n" +
                    "Thank you for using BooknPlay.lk!");

            mailSender.send(message);
            log.info("Sent email notification to {} for booking {}", recipientEmail, bookingRef);
        } catch (Exception e) {
            log.error("Failed to send email notification to {}: {}", customer.getUser().getEmail(), e.getMessage());
        }
    }
}
