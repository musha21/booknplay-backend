package lk.booknplay.service;

import lk.booknplay.dto.response.CourtResponse;
import lk.booknplay.dto.response.BusinessResponse;
import lk.booknplay.dto.response.SportResponse;
import lk.booknplay.dto.response.VenueResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PublicSearchService {
    List<BusinessResponse> getPublicBusinesses();
    Page<VenueResponse> searchVenues(String city, String sportId, String name, Pageable pageable);
    VenueResponse getVenueDetails(String id);
    List<SportResponse> getAllActiveSports();
    List<CourtResponse> getCourtsByVenue(String venueId);
}
