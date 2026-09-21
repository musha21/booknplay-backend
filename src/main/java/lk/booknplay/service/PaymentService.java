package lk.booknplay.service;

import lk.booknplay.dto.response.InvoiceResponse;
import lk.booknplay.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse initiatePayment(String customerEmail, String bookingId, String gateway);
    PaymentResponse processWebhook(String gateway, String bookingId, String gatewayReference, String status);
    PaymentResponse getPaymentStatus(String customerEmail, String bookingId);
    InvoiceResponse getInvoice(String customerEmail, String bookingId);
    byte[] generateInvoicePdf(String customerEmail, String bookingId);
}
