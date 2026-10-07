package lk.booknplay.service;

import java.math.BigDecimal;

/**
 * Calls PayHere's merchant Refund API (OAuth + POST /payment/refund).
 */
public interface PayHereRefundClient {

    /**
     * @param paymentId   PayHere {@code payment_id} from the notify callback
     * @param refundAmount amount to refund; when equal to the original payment, a full refund is requested
     * @param paidAmount  original paid amount (to decide full vs partial)
     * @param description reason shown to PayHere
     * @return PayHere refund reference number (as string)
     */
    String refund(String paymentId, BigDecimal refundAmount, BigDecimal paidAmount, String description);
}
