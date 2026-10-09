package lk.booknplay.controller.owner;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lk.booknplay.dto.request.CancellationPolicyRequest;
import lk.booknplay.dto.request.MediaOrderRequest;
import lk.booknplay.dto.request.OperatingHoursUpdateRequest;
import lk.booknplay.dto.request.OwnerVenueRequest;
import lk.booknplay.dto.request.VenueImagesRequest;
import lk.booknplay.dto.request.VenueOnboardRequest;
import lk.booknplay.dto.response.CancellationPolicyResponse;
import lk.booknplay.dto.response.OperatingHoursResponse;
import lk.booknplay.dto.response.VenueResponse;
import lk.booknplay.service.OwnerVenueService;
import lk.booknplay.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
@Tag(name = "Owner Venues")
public class OwnerVenueController {

    private final OwnerVenueService ownerVenueService;

    @GetMapping("/venues")
    public ResponseEntity<ApiResponse<List<VenueResponse>>> listVenues(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean archived) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerVenueService.listVenues(authentication.getName(), archived)));
    }

    @PostMapping("/venues/onboard")
    public ResponseEntity<ApiResponse<VenueResponse>> onboardVenue(
            Authentication authentication,
            @Valid @RequestBody VenueOnboardRequest request) {
        VenueResponse response = ownerVenueService.onboardVenue(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Venue created. Upload photos and publish to go live, or finish from the owner wizard.",
                        response));
    }

    @PostMapping("/venues")
    public ResponseEntity<ApiResponse<VenueResponse>> createVenue(
            Authentication authentication,
            @Valid @RequestBody OwnerVenueRequest request) {
        VenueResponse response = ownerVenueService.createVenue(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Venue draft created. Complete setup and publish when ready.",
                        response));
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

    @PostMapping("/venues/{venueId}/submit")
    public ResponseEntity<ApiResponse<VenueResponse>> submitVenue(
            Authentication authentication,
            @PathVariable String venueId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Venue is live",
                ownerVenueService.submitVenue(authentication.getName(), venueId)));
    }

    @PutMapping("/venues/{venueId}/images")
    public ResponseEntity<ApiResponse<VenueResponse>> replaceImages(
            Authentication authentication,
            @PathVariable String venueId,
            @Valid @RequestBody VenueImagesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerVenueService.replaceImages(authentication.getName(), venueId, request)));
    }

    @PostMapping(value = "/venues/{venueId}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VenueResponse>> uploadMedia(
            Authentication authentication,
            @PathVariable String venueId,
            @RequestParam("images") List<MultipartFile> images) {
        return ResponseEntity.ok(ApiResponse.success(
                "Photos uploaded",
                ownerVenueService.uploadMedia(authentication.getName(), venueId, images)));
    }

    @PatchMapping("/venues/{venueId}/media/order")
    public ResponseEntity<ApiResponse<VenueResponse>> reorderMedia(
            Authentication authentication,
            @PathVariable String venueId,
            @Valid @RequestBody MediaOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Photo order updated",
                ownerVenueService.reorderMedia(authentication.getName(), venueId, request.getMediaIds())));
    }

    @DeleteMapping("/venues/{venueId}/media/{mediaId}")
    public ResponseEntity<ApiResponse<VenueResponse>> deleteMedia(
            Authentication authentication,
            @PathVariable String venueId,
            @PathVariable String mediaId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Photo removed",
                ownerVenueService.deleteMedia(authentication.getName(), venueId, mediaId)));
    }

    @DeleteMapping("/venues/{venueId}")
    public ResponseEntity<ApiResponse<VenueResponse>> archiveVenue(
            Authentication authentication,
            @PathVariable String venueId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Venue archived",
                ownerVenueService.archiveVenue(authentication.getName(), venueId)));
    }

    @PostMapping("/venues/{venueId}/restore")
    public ResponseEntity<ApiResponse<VenueResponse>> restoreVenue(
            Authentication authentication,
            @PathVariable String venueId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Venue restored",
                ownerVenueService.restoreVenue(authentication.getName(), venueId)));
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
