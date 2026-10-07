package lk.booknplay.controller.owner;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lk.booknplay.dto.request.SubscriptionCheckoutRequest;
import lk.booknplay.dto.response.SubscriptionCheckoutResponse;
import lk.booknplay.dto.response.SubscriptionPlanResponse;
import lk.booknplay.dto.response.SubscriptionResponse;
import lk.booknplay.service.OwnerSubscriptionService;
import lk.booknplay.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/owner/subscription")
@RequiredArgsConstructor
@Tag(name = "Owner Subscription")
public class OwnerSubscriptionController {

    private final OwnerSubscriptionService ownerSubscriptionService;

    @GetMapping
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscription(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerSubscriptionService.getSubscription(authentication.getName())));
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> listPlans() {
        return ResponseEntity.ok(ApiResponse.success(ownerSubscriptionService.listPlans()));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<SubscriptionCheckoutResponse>> checkout(
            Authentication authentication,
            @Valid @RequestBody SubscriptionCheckoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Checkout started",
                ownerSubscriptionService.checkout(authentication.getName(), request)));
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<ApiResponse<SubscriptionCheckoutResponse>> paymentStatus(
            Authentication authentication,
            @PathVariable String paymentId) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerSubscriptionService.getPaymentStatus(authentication.getName(), paymentId)));
    }
}
