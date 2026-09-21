package lk.booknplay.enums;

/**
 * Payment transaction states.
 *
 * Flow:
 *   INITIATED → PROCESSING → SUCCESS
 *                          ↘ FAILED
 *                          ↘ REFUNDED (full or partial)
 */
public enum PaymentStatus {
    /** Payment session opened, redirect URL returned. */
    INITIATED,
    /** Gateway is processing. */
    PROCESSING,
    /** Payment verified by backend via webhook. */
    SUCCESS,
    /** Payment was declined or timed out. */
    FAILED,
    /** Full or partial refund issued. */
    REFUNDED,
    /** Partial refund issued. */
    PARTIALLY_REFUNDED,
    /** Cash / walk-in payment recorded by the venue owner. */
    PAID
}
