package lk.booknplay.controller.owner;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lk.booknplay.dto.request.OwnerRefundRequest;
import lk.booknplay.dto.request.OwnerSettingsUpdateRequest;
import lk.booknplay.dto.request.PromotionRequest;
import lk.booknplay.dto.response.BookingCancellationResponse;
import lk.booknplay.dto.response.BookingResponse;
import lk.booknplay.dto.response.OwnerActivityResponse;
import lk.booknplay.dto.response.OwnerCustomerResponse;
import lk.booknplay.dto.response.OwnerPaymentResponse;
import lk.booknplay.dto.response.OwnerRefundListItemResponse;
import lk.booknplay.dto.response.OwnerReviewResponse;
import lk.booknplay.dto.response.OwnerSettingsResponse;
import lk.booknplay.dto.response.OwnerSportResponse;
import lk.booknplay.dto.response.PromotionResponse;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.service.OwnerPortalService;
import lk.booknplay.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
@Tag(name = "Owner Portal")
public class OwnerPortalController {

    private final OwnerPortalService ownerPortalService;

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> listBookings(
            Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String venueId,
            @RequestParam(required = false) String courtId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerPortalService.listBookings(auth.getName(), from, to, venueId, courtId, status, q, pageable)));
    }

    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            Authentication auth, @PathVariable String bookingId) {
        return ResponseEntity.ok(ApiResponse.success(ownerPortalService.getBooking(auth.getName(), bookingId)));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<Page<OwnerPaymentResponse>>> listPayments(
            Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String venueId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerPortalService.listPayments(auth.getName(), from, to, venueId, pageable)));
    }

    @GetMapping("/refunds")
    public ResponseEntity<ApiResponse<Page<OwnerRefundListItemResponse>>> listRefunds(
            Authentication auth, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(ownerPortalService.listRefunds(auth.getName(), pageable)));
    }

    @PostMapping("/refunds")
    public ResponseEntity<ApiResponse<BookingCancellationResponse>> requestRefund(
            Authentication auth, @Valid @RequestBody OwnerRefundRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ownerPortalService.requestRefund(auth.getName(), request)));
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<List<OwnerCustomerResponse>>> listCustomers(
            Authentication auth, @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.success(ownerPortalService.listCustomers(auth.getName(), q)));
    }

    @GetMapping("/sports")
    public ResponseEntity<ApiResponse<List<OwnerSportResponse>>> listSports(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(ownerPortalService.listSports(auth.getName())));
    }

    @GetMapping("/promotions")
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> listPromotions(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(ownerPortalService.listPromotions(auth.getName())));
    }

    @PostMapping("/promotions")
    public ResponseEntity<ApiResponse<PromotionResponse>> createPromotion(
            Authentication auth, @Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ownerPortalService.createPromotion(auth.getName(), request)));
    }

    @PutMapping("/promotions/{promotionId}")
    public ResponseEntity<ApiResponse<PromotionResponse>> updatePromotion(
            Authentication auth, @PathVariable String promotionId, @Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerPortalService.updatePromotion(auth.getName(), promotionId, request)));
    }

    @DeleteMapping("/promotions/{promotionId}")
    public ResponseEntity<ApiResponse<Void>> deletePromotion(
            Authentication auth, @PathVariable String promotionId) {
        ownerPortalService.deletePromotion(auth.getName(), promotionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<OwnerSettingsResponse>> getSettings(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(ownerPortalService.getSettings(auth.getName())));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<OwnerSettingsResponse>> updateSettings(
            Authentication auth, @RequestBody OwnerSettingsUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ownerPortalService.updateSettings(auth.getName(), request)));
    }

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<Page<OwnerReviewResponse>>> listReviews(
            Authentication auth,
            @RequestParam(required = false) String venueId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerPortalService.listReviews(auth.getName(), venueId, pageable)));
    }

    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<List<OwnerActivityResponse>>> listActivity(
            Authentication auth, @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success(ownerPortalService.listActivity(auth.getName(), limit)));
    }
}
