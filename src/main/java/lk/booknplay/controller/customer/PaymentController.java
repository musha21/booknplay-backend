package lk.booknplay.controller.customer;

import lk.booknplay.dto.response.InvoiceResponse;
import lk.booknplay.dto.response.PaymentResponse;
import lk.booknplay.service.PaymentService;
import lk.booknplay.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate/{bookingId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            Authentication authentication,
            @PathVariable String bookingId,
            @RequestParam(defaultValue = "PAYHERE") String gateway) {
        PaymentResponse response = paymentService.initiatePayment(authentication.getName(), bookingId, gateway);
        return ResponseEntity.ok(ApiResponse.success("Payment initiated", response));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatus(
            Authentication authentication,
            @PathVariable String bookingId) {
        PaymentResponse response = paymentService.getPaymentStatus(authentication.getName(), bookingId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/invoice/{bookingId}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(
            Authentication authentication,
            @PathVariable String bookingId) {
        InvoiceResponse response = paymentService.getInvoice(authentication.getName(), bookingId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/invoice/{bookingId}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            Authentication authentication,
            @PathVariable String bookingId) {
        byte[] pdfBytes = paymentService.generateInvoicePdf(authentication.getName(), bookingId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + bookingId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
