package lk.booknplay.controller.public_api;

import lk.booknplay.dto.response.CourtResponse;
import lk.booknplay.dto.response.BusinessResponse;
import lk.booknplay.dto.response.SportResponse;
import lk.booknplay.dto.response.VenueResponse;
import lk.booknplay.dto.response.HomepageConfigResponse;
import lk.booknplay.service.PublicSearchService;
import lk.booknplay.service.HomepageConfigService;
import lk.booknplay.util.ApiResponse;
import lk.booknplay.util.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicVenueController {

    private final PublicSearchService publicSearchService;
    private final HomepageConfigService homepageConfigService;

    @GetMapping("/homepage")
    public ResponseEntity<ApiResponse<HomepageConfigResponse>> getHomepage() {
        return ResponseEntity.ok(ApiResponse.success(homepageConfigService.publicConfig()));
    }

    @GetMapping("/businesses")
    public ResponseEntity<ApiResponse<List<BusinessResponse>>> getPublicBusinesses() {
        return ResponseEntity.ok(ApiResponse.success(publicSearchService.getPublicBusinesses()));
    }

    @GetMapping("/venues")
    public ResponseEntity<ApiResponse<PageResponse<VenueResponse>>> searchVenues(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String sportId,
            @RequestParam(required = false) String name,
            Pageable pageable) {
        PageResponse<VenueResponse> response = PageResponse.from(
                publicSearchService.searchVenues(city, sportId, name, pageable)
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/venues/{id}")
    public ResponseEntity<ApiResponse<VenueResponse>> getVenueDetails(@PathVariable String id) {
        VenueResponse response = publicSearchService.getVenueDetails(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sports")
    public ResponseEntity<ApiResponse<List<SportResponse>>> getAllActiveSports() {
        List<SportResponse> response = publicSearchService.getAllActiveSports();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/venues/{venueId}/courts")
    public ResponseEntity<ApiResponse<List<CourtResponse>>> getCourtsByVenue(@PathVariable String venueId) {
        List<CourtResponse> response = publicSearchService.getCourtsByVenue(venueId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
