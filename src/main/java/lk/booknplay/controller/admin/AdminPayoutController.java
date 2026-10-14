package lk.booknplay.controller.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import lk.booknplay.dto.request.AdminBankVerificationRequest;
import lk.booknplay.dto.request.AdminPayoutConfigUpdateRequest;
import lk.booknplay.dto.request.AdminPayoutSettleRequest;
import lk.booknplay.dto.request.AdminPayoutStatusUpdateRequest;
import lk.booknplay.dto.response.AdminPayoutBusinessResponse;
import lk.booknplay.dto.response.PlatformPayoutConfigResponse;
import lk.booknplay.dto.response.PayoutResponse;
import lk.booknplay.enums.PayoutFrequency;
import lk.booknplay.enums.PayoutStatus;
import lk.booknplay.service.PayoutSettlementService;
import lk.booknplay.util.ApiResponse;
import lk.booknplay.util.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Payouts")
public class AdminPayoutController {

    private final PayoutSettlementService payoutSettlementService;

    @GetMapping("/payouts/businesses")
    public ResponseEntity<ApiResponse<List<AdminPayoutBusinessResponse>>> businesses() {
        return ResponseEntity.ok(ApiResponse.success(payoutSettlementService.listBusinesses()));
    }

    @GetMapping("/payouts")
    public ResponseEntity<ApiResponse<PageResponse<PayoutResponse>>> list(
            @RequestParam(required = false) String businessId,
            @RequestParam(required = false) PayoutStatus status,
            @RequestParam(required = false) PayoutFrequency frequency,
            Pageable pageable) {
        var page = payoutSettlementService.listAdminPayouts(businessId, status, frequency, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    @GetMapping("/payouts/{id}")
    public ResponseEntity<ApiResponse<PayoutResponse>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(payoutSettlementService.getAdminPayout(id)));
    }

    @PatchMapping("/payouts/{id}/status")
    public ResponseEntity<ApiResponse<PayoutResponse>> updateStatus(
            Authentication auth,
            @PathVariable String id,
            @RequestBody AdminPayoutStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                payoutSettlementService.updateStatus(auth.getName(), id, request)));
    }

    @PostMapping(value = "/payouts/{id}/settle", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PayoutResponse>> settleMultipart(
            Authentication auth,
            @PathVariable String id,
            @RequestParam String transferReference,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate transferDate,
            @RequestParam BigDecimal transferAmount,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) String overrideReason,
            @RequestParam(required = false) MultipartFile receipt) {
        AdminPayoutSettleRequest request = AdminPayoutSettleRequest.builder()
                .transferReference(transferReference)
                .transferDate(transferDate)
                .transferAmount(transferAmount)
                .note(note)
                .overrideReason(overrideReason)
                .build();
        return ResponseEntity.ok(ApiResponse.success(
                payoutSettlementService.settle(auth.getName(), id, request, receipt)));
    }

    @PostMapping(value = "/payouts/{id}/settle", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PayoutResponse>> settleJson(
            Authentication auth,
            @PathVariable String id,
            @RequestBody AdminPayoutSettleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                payoutSettlementService.settle(auth.getName(), id, request, null)));
    }

    @PostMapping("/payouts/generate")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> generate(
            Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        int created = payoutSettlementService.generateSettlements(asOf);
        return ResponseEntity.ok(ApiResponse.success(Map.of("created", created)));
    }

    @PatchMapping("/businesses/{id}/bank-verification")
    public ResponseEntity<ApiResponse<Void>> verifyBank(
            Authentication auth,
            @PathVariable String id,
            @RequestBody AdminBankVerificationRequest request) {
        payoutSettlementService.verifyBank(auth.getName(), id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/payout-config")
    public ResponseEntity<ApiResponse<PlatformPayoutConfigResponse>> getConfig() {
        return ResponseEntity.ok(ApiResponse.success(payoutSettlementService.getConfig()));
    }

    @PatchMapping("/payout-config")
    public ResponseEntity<ApiResponse<PlatformPayoutConfigResponse>> updateConfig(
            Authentication auth,
            @RequestBody AdminPayoutConfigUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                payoutSettlementService.updateConfig(auth.getName(), request)));
    }
}
