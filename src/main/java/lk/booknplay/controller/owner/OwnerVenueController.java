package lk.booknplay.controller.owner;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lk.booknplay.dto.request.CancellationPolicyRequest;
import lk.booknplay.dto.request.OperatingHoursUpdateRequest;
import lk.booknplay.dto.request.VenueOnboardRequest;
import lk.booknplay.dto.request.OwnerVenueRequest;
import lk.booknplay.dto.request.VenueImagesRequest;
import lk.booknplay.dto.response.CancellationPolicyResponse;
import lk.booknplay.dto.response.OperatingHoursResponse;
import lk.booknplay.dto.response.VenueResponse;
import lk.booknplay.service.OwnerVenueService;
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
@Tag(name = "Owner Venues")
public class OwnerVenueController {

    private final OwnerVenueService ownerVenueService;

    @GetMapping("/venues")
    public ResponseEntity<ApiResponse<List<VenueResponse>>> listVenues(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(ownerVenueService.listVenues(authentication.getName())));
    }

    @PostMapping("/venues/onboard")
    public ResponseEntity<ApiResponse<VenueResponse>> onboardVenue(
            Authentication authentication,
            @Valid @RequestBody VenueOnboardRequest request) {
        VenueResponse response = ownerVenueService.onboardVenue(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Venue created and is now live on the landing page", response));
    }

    @PostMapping("/venues")
    public ResponseEntity<ApiResponse<VenueResponse>> createVenue(
            Authentication authentication,
            @Valid @RequestBody OwnerVenueRequest request) {
        VenueResponse response = ownerVenueService.createVenue(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Venue created and is now live on the landing page", response));
    }

    @GetMapping("/venues/{venueId}")
    public ResponseEntity<ApiResponse<VenueResponse>> getVenue(
            Authentication authentication,
            @PathVariable String venueId) {
        return ResponseEntity.ok(ApiResponse.success(ownerVenueService.getVenue(authentication.getName(), venueId)));
    }

    @PutMapping("/venues/{venueId}")
    public ResponseEntity<ApiResponse<VenueResponse>> updateVenue(
            Authentication authentication,
            @PathVariable String venueId,
            @Valid @RequestBody OwnerVenueRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Venue updated",
                ownerVenueService.updateVenue(authentication.getName(), venueId, request)));
    }

    @PutMapping("/venues/{venueId}/images")
    public ResponseEntity<ApiResponse<VenueResponse>> replaceImages(
            Authentication authentication,
            @PathVariable String venueId,
            @Valid @RequestBody VenueImagesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerVenueService.replaceImages(authentication.getName(), venueId, request)));
    }

    @GetMapping("/venues/{venueId}/operating-hours")
    public ResponseEntity<ApiResponse<List<OperatingHoursResponse>>> getHours(
            Authentication authentication,
            @PathVariable String venueId) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerVenueService.getOperatingHours(authentication.getName(), venueId)));
    }

    @PutMapping("/venues/{venueId}/operating-hours")
    public ResponseEntity<ApiResponse<List<OperatingHoursResponse>>> replaceHours(
            Authentication authentication,
            @PathVariable String venueId,
            @Valid @RequestBody OperatingHoursUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Operating hours updated",
                ownerVenueService.replaceOperatingHours(authentication.getName(), venueId, request)));
    }

    @GetMapping("/cancellation-policy")
    public ResponseEntity<ApiResponse<CancellationPolicyResponse>> getPolicy(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(ownerVenueService.getCancellationPolicy(authentication.getName())));
    }

    @PutMapping("/venues/{venueId}/cancellation-policy")
    public ResponseEntity<ApiResponse<CancellationPolicyResponse>> upsertPolicy(
            Authentication authentication,
            @PathVariable String venueId,
            @Valid @RequestBody CancellationPolicyRequest request) {
        ownerVenueService.getVenue(authentication.getName(), venueId);
        return ResponseEntity.ok(ApiResponse.success(
                "Cancellation policy saved",
                ownerVenueService.upsertCancellationPolicy(authentication.getName(), request)));
    }
}
