package lk.booknplay.enums;

/**
 * Lifecycle states for a Customer account.
 */
public enum CustomerStatus {
    /** Account is active and can make bookings. */
    ACTIVE,
    /** Account is temporarily inactive (e.g., customer deactivated). */
    INACTIVE,
    /** Account is suspended by admin due to policy violations. */
    SUSPENDED,
    /** Soft-deleted — retained for audit purposes. */
    DELETED
}
