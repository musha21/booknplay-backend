package lk.booknplay.enums;

/**
 * Lifecycle states of a court booking.
 *
 * Flow:
 *   PENDING → CONFIRMED → COMPLETED
 *          ↘ CANCELLED
 *          ↘ FAILED (payment failed)
 *   CONFIRMED → NO_SHOW (customer did not arrive)
 */
public enum BookingStatus {
    /** Created but awaiting payment. */
    PENDING,
    /** Payment received — court is reserved. */
    CONFIRMED,
    /** Booking time has passed and court was used. */
    COMPLETED,
    /** Cancelled by customer or admin. */
    CANCELLED,
    /** Payment failed — booking never activated. */
    FAILED,
    /** Customer did not show up. */
    NO_SHOW
}
