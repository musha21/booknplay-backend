package lk.booknplay.enums;

/**
 * Types of notifications sent to customers.
 * Used to route notifications to the correct template and channel.
 */
public enum NotificationType {
    BOOKING_CREATED,
    BOOKING_CONFIRMED,
    BOOKING_CANCELLED,
    BOOKING_REMINDER,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    REFUND_INITIATED,
    REFUND_COMPLETED,
    ACCOUNT_CREATED,
    GENERAL
}
