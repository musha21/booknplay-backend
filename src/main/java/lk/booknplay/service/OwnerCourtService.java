package lk.booknplay.service;

import lk.booknplay.dto.request.CourtPricingUpdateRequest;
import lk.booknplay.dto.request.OwnerCourtRequest;
import lk.booknplay.dto.response.CourtPricingResponse;
import lk.booknplay.dto.response.CourtResponse;

import java.util.List;

public interface OwnerCourtService {
    List<CourtResponse> listCourts(String ownerEmail, String venueId);
    CourtResponse getCourt(String ownerEmail, String courtId);
    CourtResponse createCourt(String ownerEmail, String venueId, OwnerCourtRequest request);
    CourtResponse updateCourt(String ownerEmail, String courtId, OwnerCourtRequest request);
    void deleteCourt(String ownerEmail, String courtId);
    List<CourtPricingResponse> getPricing(String ownerEmail, String courtId);
    List<CourtPricingResponse> replacePricing(String ownerEmail, String courtId, CourtPricingUpdateRequest request);
}
