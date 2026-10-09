package lk.booknplay.service;

import lk.booknplay.dto.response.CourtResponse;
import lk.booknplay.dto.response.BusinessResponse;
import lk.booknplay.dto.response.OwnerReviewResponse;
import lk.booknplay.dto.response.PromotionResponse;
import lk.booknplay.dto.response.SportResponse;
import lk.booknplay.dto.response.VenueResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface PublicSearchService {
    List<BusinessResponse> getPublicBusinesses();
    Page<VenueResponse> searchVenues(
            String city,
            String sportId,
            String name,
            LocalDate date,
            LocalTime time,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String amenity,
            String indoorOutdoor,
            Double minRating,
            Double lat,
            Double lng,
            Double radiusKm,
            Pageable pageable);
    VenueResponse getVenueDetails(String id);
    List<SportResponse> getAllActiveSports();
    List<CourtResponse> getCourtsByVenue(String venueId);
    List<OwnerReviewResponse> getVenueReviews(String venueId);
    List<PromotionResponse> getPublicPromotions();
}
