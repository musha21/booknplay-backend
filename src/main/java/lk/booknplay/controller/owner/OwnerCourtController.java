package lk.booknplay.controller.owner;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lk.booknplay.dto.request.CourtPricingUpdateRequest;
import lk.booknplay.dto.request.OwnerCourtRequest;
import lk.booknplay.dto.response.CourtPricingResponse;
import lk.booknplay.dto.response.CourtResponse;
import lk.booknplay.service.OwnerCourtService;
import lk.booknplay.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
@Tag(name = "Owner Courts")
public class OwnerCourtController {

    private final OwnerCourtService ownerCourtService;

    @GetMapping("/venues/{venueId}/courts")
    public ResponseEntity<ApiResponse<List<CourtResponse>>> listCourts(
            Authentication authentication,
            @PathVariable String venueId) {
        return ResponseEntity.ok(ApiResponse.success(ownerCourtService.listCourts(authentication.getName(), venueId)));
    }

    @PostMapping("/venues/{venueId}/courts")
    public ResponseEntity<ApiResponse<CourtResponse>> createCourt(
            Authentication authentication,
            @PathVariable String venueId,
            @Valid @RequestBody OwnerCourtRequest request) {
        CourtResponse response = ownerCourtService.createCourt(authentication.getName(), venueId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Court created", response));
    }

    @GetMapping("/courts/{courtId}")
    public ResponseEntity<ApiResponse<CourtResponse>> getCourt(
            Authentication authentication,
            @PathVariable String courtId) {
        return ResponseEntity.ok(ApiResponse.success(ownerCourtService.getCourt(authentication.getName(), courtId)));
    }

    @PutMapping("/courts/{courtId}")
    public ResponseEntity<ApiResponse<CourtResponse>> updateCourt(
            Authentication authentication,
            @PathVariable String courtId,
            @Valid @RequestBody OwnerCourtRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Court updated",
                ownerCourtService.updateCourt(authentication.getName(), courtId, request)));
    }

    @DeleteMapping("/courts/{courtId}")
    public ResponseEntity<ApiResponse<Void>> deleteCourt(
            Authentication authentication,
            @PathVariable String courtId) {
        ownerCourtService.deleteCourt(authentication.getName(), courtId);
        return ResponseEntity.ok(ApiResponse.success("Court deleted"));
    }

    @GetMapping("/courts/{courtId}/pricing")
    public ResponseEntity<ApiResponse<List<CourtPricingResponse>>> getPricing(
            Authentication authentication,
            @PathVariable String courtId) {
        return ResponseEntity.ok(ApiResponse.success(ownerCourtService.getPricing(authentication.getName(), courtId)));
    }

    @PutMapping("/courts/{courtId}/pricing")
    public ResponseEntity<ApiResponse<List<CourtPricingResponse>>> replacePricing(
            Authentication authentication,
            @PathVariable String courtId,
            @Valid @RequestBody CourtPricingUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Pricing updated",
                ownerCourtService.replacePricing(authentication.getName(), courtId, request)));
    }
}
