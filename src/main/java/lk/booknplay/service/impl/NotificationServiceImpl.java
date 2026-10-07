package lk.booknplay.service.impl;

import lk.booknplay.entity.Booking;
import lk.booknplay.entity.Customer;
import lk.booknplay.repository.BookingRepository;
import lk.booknplay.service.NotificationService;
import lk.booknplay.service.SmsLenzService;
import lk.booknplay.util.BookingTimeFormat;
import lk.booknplay.util.PhoneNumberUtil;
import lk.booknplay.util.SportCatalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ISO_LOCAL_DATE;

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final SmsLenzService smsLenzService;
    private final BookingRepository bookingRepository;

    public NotificationServiceImpl(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            SmsLenzService smsLenzService,
            BookingRepository bookingRepository) {

        this.mailSenderProvider = mailSenderProvider;
        this.smsLenzService = smsLenzService;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public void sendBookingNotification(
            Customer customer,
            Booking booking,
            String eventType) {

        if (booking == null) {
            log.warn("Cannot send notification: booking is null");
            return;
        }

        // Reload with discrete slots so after-commit notifications see gaps correctly.
        if (booking.getId() != null) {
            booking = bookingRepository.findByIdWithSlots(booking.getId()).orElse(booking);
        }

        log.info(
                "Processing booking notification. bookingRef={}, event={}",
                booking.getBookingRef(),
                eventType
        );

        /*
         * SMS first.
         *
         * SMS failure must NOT prevent email.
         * Email failure must NOT prevent SMS.
         */
        try {

            sendBookingSms(
                    customer,
                    booking,
                    eventType
            );

        } catch (Exception ex) {

            log.error(
                    "Unexpected SMS notification error. bookingRef={}, error={}",
                    booking.getBookingRef(),
                    ex.getMessage(),
                    ex
            );
        }

        /*
         * Email notification handled separately.
         */
        try {

            sendBookingEmail(
                    customer,
                    booking,
                    eventType
            );

        } catch (Exception ex) {

            log.error(
                    "Unexpected email notification error. bookingRef={}, error={}",
                    booking.getBookingRef(),
                    ex.getMessage(),
                    ex
            );
        }
    }

    /*
     * ============================================================
     * EMAIL
     * ============================================================
     */

    private void sendBookingEmail(
            Customer customer,
            Booking booking,
            String eventType) {

        if (customer == null) {

            log.info(
                    "Skipping email notification: no registered customer for booking {}",
                    booking.getBookingRef()
            );

            return;
        }

        if (customer.getUser() == null) {

            log.warn(
                    "Skipping email notification: user missing for booking {}",
                    booking.getBookingRef()
            );

            return;
        }

        String recipientEmail = customer.getUser().getEmail();

        if (recipientEmail == null || recipientEmail.isBlank()) {

            log.warn(
                    "Skipping email notification: customer email missing for booking {}",
                    booking.getBookingRef()
            );

            return;
        }

        JavaMailSender mailSender =
                mailSenderProvider.getIfAvailable();

        if (mailSender == null) {

            log.warn(
                    "JavaMailSender is not configured. Skipping email for booking {}",
                    booking.getBookingRef()
            );

            return;
        }

        try {

            String firstName =
                    customer.getFirstName() != null
                            && !customer.getFirstName().isBlank()
                            ? customer.getFirstName()
                            : "Valued Customer";

            BookingDetails details =
                    detailsOf(booking);

            SimpleMailMessage message =
                    new SimpleMailMessage();

            message.setTo(recipientEmail);

            message.setSubject(
                    buildEmailSubject(eventType)
            );

            message.setText(
                    buildEmailMessage(
                            firstName,
                            details,
                            eventType
                    )
            );

            mailSender.send(message);

            log.info(
                    "Email notification sent. bookingRef={}, event={}, email={}",
                    booking.getBookingRef(),
                    eventType,
                    maskEmail(recipientEmail)
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to send email notification. bookingRef={}, event={}, error={}",
                    booking.getBookingRef(),
                    eventType,
                    ex.getMessage(),
                    ex
            );
        }
    }

    /*
     * ============================================================
     * SMS
     * ============================================================
     */

    private void sendBookingSms(
            Customer customer,
            Booking booking,
            String eventType) {

        /*
         * IMPORTANT:
         *
         * We DO NOT skip WALK_IN bookings anymore.
         *
         * If an owner creates a booking and provides a valid phone,
         * the SMS is allowed to be sent.
         */

        if (!"BOOKING_CONFIRMED".equals(eventType)
                && !"BOOKING_CANCELLED".equals(eventType)) {

            log.info(
                    "SMS skipped because event is not supported. bookingRef={}, event={}",
                    booking.getBookingRef(),
                    eventType
            );

            return;
        }

        String phone =
                resolveSmsPhone(
                        customer,
                        booking
                );

        if (phone == null) {

            log.warn(
                    "SMS skipped: no valid phone number. bookingRef={}, event={}",
                    booking.getBookingRef(),
                    eventType
            );

            return;
        }

        try {

            String maskedPhone =
                    maskPhone(phone);

            log.info(
                    "Calling SMSlenz. bookingRef={}, event={}, recipient={}",
                    booking.getBookingRef(),
                    eventType,
                    maskedPhone
            );

            String body =
                    buildSmsMessage(
                            booking,
                            eventType
                    );

            smsLenzService.sendSms(
                    phone,
                    body
            );

            log.info(
                    "SMSlenz call completed. bookingRef={}, event={}, recipient={}",
                    booking.getBookingRef(),
                    eventType,
                    maskedPhone
            );

        } catch (Exception ex) {

            log.error(
                    "SMSlenz failed. bookingRef={}, event={}, error={}",
                    booking.getBookingRef(),
                    eventType,
                    ex.getMessage(),
                    ex
            );
        }
    }

    /*
     * ============================================================
     * PHONE RESOLUTION
     * ============================================================
     *
     * Priority:
     *
     * 1. booking.contactPhone
     * 2. booking.guestPhone
     * 3. customer.phone
     *
     */

    static String resolveSmsPhone(
            Customer customer,
            Booking booking) {

        if (booking == null) {
            return null;
        }

        String raw = null;
        String source = null;

        /*
         * 1. Booking contact phone
         */
        if (booking.getContactPhone() != null
                && !booking.getContactPhone().isBlank()) {

            raw = booking.getContactPhone();
            source = "booking.contactPhone";
        }

        /*
         * 2. Guest phone
         */
        else if (booking.getGuestPhone() != null
                && !booking.getGuestPhone().isBlank()) {

            raw = booking.getGuestPhone();
            source = "booking.guestPhone";
        }

        /*
         * 3. Registered customer phone
         */
        else if (customer != null
                && customer.getPhone() != null
                && !customer.getPhone().isBlank()) {

            raw = customer.getPhone();
            source = "customer.phone";
        }

        if (raw == null) {

            log.warn(
                    "No SMS phone found. bookingRef={}",
                    booking.getBookingRef()
            );

            return null;
        }

        try {

            String normalized =
                    PhoneNumberUtil.normalizeSriLanka(
                            raw.trim()
                    );

            if (normalized == null
                    || normalized.isBlank()) {

                log.warn(
                        "Phone normalization returned empty. bookingRef={}, source={}",
                        booking.getBookingRef(),
                        source
                );

                return null;
            }

            log.info(
                    "SMS phone resolved. bookingRef={}, source={}, normalized={}",
                    booking.getBookingRef(),
                    source,
                    maskPhone(normalized)
            );

            return normalized;

        } catch (Exception ex) {

            log.warn(
                    "Invalid SMS phone. bookingRef={}, source={}, raw={}, error={}",
                    booking.getBookingRef(),
                    source,
                    maskPhone(raw),
                    ex.getMessage()
            );

            return null;
        }
    }

    /*
     * ============================================================
     * SMS MESSAGE
     * ============================================================
     */

    static String buildSmsMessage(
            Booking booking,
            String eventType) {

        BookingDetails d =
                detailsOf(booking);

        String place =
                d.venue
                        + ", "
                        + d.sport
                        + " — "
                        + d.resourceName;

        if ("BOOKING_CANCELLED".equals(eventType)) {

            return "BooknPlay: Booking "
                    + d.ref
                    + " cancelled. "
                    + place
                    + ", "
                    + d.date
                    + " "
                    + d.times
                    + ".";
        }

        return "BooknPlay: Booking "
                + d.ref
                + " confirmed. "
                + place
                + ", "
                + d.date
                + " "
                + d.times
                + ". Amount LKR "
                + d.amount
                + ".";
    }

    /*
     * ============================================================
     * EMAIL MESSAGE
     * ============================================================
     */

    private static String buildEmailSubject(
            String eventType) {

        if ("BOOKING_CONFIRMED".equals(eventType)) {
            return "BooknPlay - Booking Confirmed";
        }

        if ("BOOKING_CANCELLED".equals(eventType)) {
            return "BooknPlay - Booking Cancelled";
        }

        return "BooknPlay Booking Notification";
    }

    private static String buildEmailMessage(
            String firstName,
            BookingDetails details,
            String eventType) {

        if ("BOOKING_CONFIRMED".equals(eventType)) {

            return "Dear "
                    + firstName
                    + ",\n\n"
                    + "Your BooknPlay booking has been confirmed.\n\n"
                    + "Booking Ref: "
                    + details.ref
                    + "\n"
                    + "Venue: "
                    + details.venue
                    + "\n"
                    + "Sport: "
                    + details.sport
                    + "\n"
                    + details.resourceLabel
                    + ": "
                    + details.court
                    + "\n"
                    + "Date: "
                    + details.date
                    + "\n"
                    + "Time: "
                    + details.times
                    + "\n"
                    + "Amount: LKR "
                    + details.amount
                    + "\n\n"
                    + "Thank you for using BooknPlay.";
        }

        if ("BOOKING_CANCELLED".equals(eventType)) {

            return "Dear "
                    + firstName
                    + ",\n\n"
                    + "Your BooknPlay booking has been cancelled.\n\n"
                    + "Booking Ref: "
                    + details.ref
                    + "\n"
                    + "Venue: "
                    + details.venue
                    + "\n"
                    + "Sport: "
                    + details.sport
                    + "\n"
                    + details.resourceLabel
                    + ": "
                    + details.court
                    + "\n"
                    + "Date: "
                    + details.date
                    + "\n"
                    + "Time: "
                    + details.times
                    + "\n\n"
                    + "Thank you for using BooknPlay.";
        }

        return "Dear "
                + firstName
                + ",\n\n"
                + "Your booking "
                + details.ref
                + " has an update: "
                + eventType
                + ".\n\n"
                + "Thank you for using BooknPlay.";
    }

    /*
     * ============================================================
     * BOOKING DETAILS
     * ============================================================
     */

    private static BookingDetails detailsOf(
            Booking booking) {

        String ref =
                booking.getBookingRef() != null
                        ? booking.getBookingRef()
                        : "N/A";

        String venue =
                booking.getVenue() != null
                        && booking.getVenue().getName() != null
                        ? booking.getVenue().getName()
                        : "Venue";

        String sport = resolveSportName(booking);
        String resourceLabel = SportCatalog.resourceLabel(sport);

        String court =
                booking.getCourt() != null
                        && booking.getCourt().getName() != null
                        ? booking.getCourt().getName()
                        : resourceLabel;

        String date =
                booking.getBookingDate() != null
                        ? booking.getBookingDate().format(DATE_FMT)
                        : "N/A";

        String times = BookingTimeFormat.formatBooking(booking);
        if (times == null || times.isBlank()) {
            times = "N/A";
        }

        String amount =
                booking.getTotalAmount() != null
                        ? booking.getTotalAmount().toPlainString()
                        : "0";

        return new BookingDetails(
                ref,
                venue,
                sport,
                resourceLabel,
                court,
                formatResourceName(resourceLabel, court),
                date,
                times,
                amount
        );
    }

    static String resolveSportName(Booking booking) {
        if (booking.getSport() != null && booking.getSport().getName() != null
                && !booking.getSport().getName().isBlank()) {
            return booking.getSport().getName().trim();
        }
        if (booking.getCourt() != null
                && booking.getCourt().getSport() != null
                && booking.getCourt().getSport().getName() != null
                && !booking.getCourt().getSport().getName().isBlank()) {
            return booking.getCourt().getSport().getName().trim();
        }
        return "Sport";
    }

    /**
     * Prefixes the resource label only when the court name does not already start with it.
     */
    static String formatResourceName(String resourceLabel, String courtName) {
        String label = resourceLabel != null && !resourceLabel.isBlank() ? resourceLabel.trim() : "Court";
        if (courtName == null || courtName.isBlank()) {
            return label;
        }
        String name = courtName.trim();
        if (name.regionMatches(true, 0, label, 0, label.length())) {
            return name;
        }
        return label + " " + name;
    }

    /*
     * ============================================================
     * MASKING
     * ============================================================
     */

    private static String maskPhone(
            String phone) {

        if (phone == null || phone.isBlank()) {
            return "(missing)";
        }

        String clean = phone.trim();

        if (clean.length() <= 4) {
            return "****";
        }

        String lastFour =
                clean.substring(
                        clean.length() - 4
                );

        if (clean.startsWith("+94")) {
            return "+94******" + lastFour;
        }

        return "******" + lastFour;
    }

    private static String maskEmail(
            String email) {

        if (email == null || email.isBlank()) {
            return "(missing)";
        }

        int at =
                email.indexOf('@');

        if (at <= 1) {
            return "***" + email.substring(Math.max(0, at));
        }

        return email.substring(0, 1)
                + "***"
                + email.substring(at);
    }

    /*
     * ============================================================
     * INTERNAL DTO
     * ============================================================
     */

    private record BookingDetails(
            String ref,
            String venue,
            String sport,
            String resourceLabel,
            String court,
            String resourceName,
            String date,
            String times,
            String amount
    ) {
    }
}