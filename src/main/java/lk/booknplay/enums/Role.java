package lk.booknplay.enums;

/**
 * Roles assigned to all users in the BooknPlay platform.
 * Used by Spring Security for access control decisions.
 */
public enum Role {
    SUPER_ADMIN,
    BUSINESS_OWNER,
    STAFF,
    CUSTOMER
}
