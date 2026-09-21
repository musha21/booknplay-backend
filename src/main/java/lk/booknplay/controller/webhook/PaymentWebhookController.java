package lk.booknplay.controller.webhook;

import lk.booknplay.dto.response.PaymentResponse;
import lk.booknplay.service.PaymentService;
import lk.booknplay.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhook/payment")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/{gateway}")
    public ResponseEntity<ApiResponse<PaymentResponse>> processWebhook(
            @PathVariable String gateway,
            @RequestParam String bookingId,
            @RequestParam String gatewayReference,
            @RequestParam String status) {

        PaymentResponse response = paymentService.processWebhook(gateway, bookingId, gatewayReference, status);
        return ResponseEntity.ok(ApiResponse.success("Webhook processed", response));
    }
}
