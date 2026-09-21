package lk.booknplay.service;

import lk.booknplay.dto.request.CancellationPolicyRequest;
import lk.booknplay.dto.request.OperatingHoursUpdateRequest;
import lk.booknplay.dto.request.OwnerVenueRequest;
import lk.booknplay.dto.request.VenueOnboardRequest;
import lk.booknplay.dto.request.VenueImagesRequest;
import lk.booknplay.dto.response.CancellationPolicyResponse;
import lk.booknplay.dto.response.OperatingHoursResponse;
import lk.booknplay.dto.response.VenueResponse;

import java.util.List;

public interface OwnerVenueService {
    List<VenueResponse> listVenues(String ownerEmail);
    VenueResponse getVenue(String ownerEmail, String venueId);
    VenueResponse createVenue(String ownerEmail, OwnerVenueRequest request);
    VenueResponse updateVenue(String ownerEmail, String venueId, OwnerVenueRequest request);
    VenueResponse replaceImages(String ownerEmail, String venueId, VenueImagesRequest request);
    List<OperatingHoursResponse> getOperatingHours(String ownerEmail, String venueId);
    List<OperatingHoursResponse> replaceOperatingHours(String ownerEmail, String venueId, OperatingHoursUpdateRequest request);
    CancellationPolicyResponse getCancellationPolicy(String ownerEmail);
    CancellationPolicyResponse upsertCancellationPolicy(String ownerEmail, CancellationPolicyRequest request);
    VenueResponse onboardVenue(String ownerEmail, VenueOnboardRequest request);
}
