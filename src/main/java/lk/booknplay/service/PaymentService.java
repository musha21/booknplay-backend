package lk.booknplay.service;

import lk.booknplay.dto.response.InvoiceResponse;
import lk.booknplay.dto.response.PaymentResponse;

import java.util.Map;

public interface PaymentService {
    PaymentResponse initiatePayment(String customerEmail, String bookingId, String gateway);
    PaymentResponse processWebhook(String gateway, String bookingId, String gatewayReference, String status);
    PaymentResponse processPayHereNotify(Map<String, String> fields);
    PayHereCheckoutStore.Session requireCheckoutSession(String token);
    String payHereCheckoutUrl();
    String buildReturnUrl(String bookingId);
    String buildCancelUrl(String bookingId);
    String payHereNotifyUrl();
    String payHereMerchantId();
    PaymentResponse getPaymentStatus(String customerEmail, String bookingId);
    InvoiceResponse getInvoice(String customerEmail, String bookingId);
    byte[] generateInvoicePdf(String customerEmail, String bookingId);
}
