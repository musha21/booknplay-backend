package lk.booknplay.enums;

/**
 * Operational status of a court/facility.
 */
public enum CourtStatus {
    /** Court is open for bookings. */
    ACTIVE,
    /** Court is temporarily unavailable. */
    INACTIVE,
    /** Court is under maintenance — no bookings accepted. */
    UNDER_MAINTENANCE,
    /** Court has been permanently removed. */
    DELETED
}
