package lk.booknplay.service.impl;

import lk.booknplay.dto.response.CourtResponse;
import lk.booknplay.dto.response.BusinessResponse;
import lk.booknplay.dto.response.SportResponse;
import lk.booknplay.dto.response.VenueResponse;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.Sport;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.CourtStatus;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.repository.CourtRepository;
import lk.booknplay.repository.BusinessRepository;
import lk.booknplay.repository.SportRepository;
import lk.booknplay.repository.VenueRepository;
import lk.booknplay.service.PublicSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicSearchServiceImpl implements PublicSearchService {

    private final VenueRepository venueRepository;
    private final BusinessRepository businessRepository;
    private final CourtRepository courtRepository;
    private final SportRepository sportRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BusinessResponse> getPublicBusinesses() {
        List<VenueStatus> visibleStatuses = List.of(VenueStatus.ACTIVE, VenueStatus.APPROVED);
        return businessRepository.findPublicBusinesses(visibleStatuses).stream()
                .map(business -> mapBusinessToResponse(business, visibleStatuses))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VenueResponse> searchVenues(String city, String sportId, String name, Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt"));
        return venueRepository.searchVenues(
                        List.of(VenueStatus.ACTIVE, VenueStatus.APPROVED),
                        blankToNull(city),
                        blankToNull(sportId),
                        blankToNull(name),
                        sorted)
                .map(this::mapVenueToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public VenueResponse getVenueDetails(String id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + id));
        if (venue.getStatus() != VenueStatus.ACTIVE && venue.getStatus() != VenueStatus.APPROVED) {
            throw new ResourceNotFoundException("Venue not found with id: " + id);
        }

        VenueResponse response = mapVenueToResponse(venue);
        List<CourtResponse> courts = courtRepository.findByVenueIdAndStatus(id, CourtStatus.ACTIVE)
                .stream()
                .map(this::mapCourtToResponse)
                .toList();
        response.setCourts(courts);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportResponse> getAllActiveSports() {
        return sportRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapSportToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourtResponse> getCourtsByVenue(String venueId) {
        return courtRepository.findByVenueIdAndStatus(venueId, CourtStatus.ACTIVE)
                .stream()
                .map(this::mapCourtToResponse)
                .toList();
    }

    private VenueResponse mapVenueToResponse(Venue venue) {
        List<Court> courts = courtRepository.findByVenueId(venue.getId());
        java.math.BigDecimal starting = courts.stream()
                .map(Court::getHourlyRate)
                .min(java.math.BigDecimal::compareTo)
                .orElse(null);
        String sportName = courts.stream()
                .map(court -> court.getSport() != null ? court.getSport().getName() : null)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(venue.getVenueType());
        String businessImage = venue.getBusiness() != null ? venue.getBusiness().getLogoUrl() : null;
        List<String> imageUrls = venue.getImages() == null
                ? List.of()
                : venue.getImages().stream().map(img -> img.getUrl()).filter(url -> url != null && !url.isBlank()).toList();
        String cover = venue.getCoverImageUrl();
        if (cover == null || cover.isBlank()) {
            cover = imageUrls.isEmpty() ? businessImage : imageUrls.get(0);
        }
        return VenueResponse.builder()
                .id(venue.getId())
                .businessId(venue.getBusiness() != null ? venue.getBusiness().getId() : null)
                .businessName(venue.getBusiness() != null ? venue.getBusiness().getName() : null)
                .name(venue.getName())
                .address(venue.getAddress())
                .city(venue.getCity())
                .formattedAddress(venue.getFormattedAddress())
                .venueType(venue.getVenueType())
                .sportName(sportName)
                .latitude(venue.getLatitude())
                .longitude(venue.getLongitude())
                .description(venue.getDescription())
                .status(venue.getStatus())
                .coverImageUrl(cover)
                .businessImageUrl(businessImage)
                .startingPrice(starting)
                .currency("LKR")
                .amenities(copyList(venue.getAmenities()))
                .rules(copyList(venue.getRules()))
                .additionalRules(venue.getAdditionalRules())
                .images(imageUrls)
                .build();
    }

    private BusinessResponse mapBusinessToResponse(Business business, List<VenueStatus> visibleStatuses) {
        List<Venue> publicVenues = venueRepository.findByBusinessId(business.getId()).stream()
                .filter(venue -> visibleStatuses.contains(venue.getStatus()))
                .toList();
        String imageUrl = business.getImages() == null ? null : business.getImages().stream()
                .filter(image -> !image.isLogo() && image.getUrl() != null && !image.getUrl().isBlank())
                .map(image -> image.getUrl())
                .findFirst()
                .orElse(null);
        List<String> sportNames = publicVenues.stream()
                .flatMap(venue -> courtRepository.findByVenueId(venue.getId()).stream())
                .map(court -> court.getSport() != null ? court.getSport().getName() : null)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();
        return BusinessResponse.builder()
                .id(business.getId())
                .name(business.getName())
                .address(business.getAddress())
                .logoUrl(business.getLogoUrl())
                .imageUrl(imageUrl)
                .venueCount(publicVenues.size())
                .cities(publicVenues.stream().map(Venue::getCity).filter(city -> city != null && !city.isBlank()).distinct().toList())
                .sports(sportNames)
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<String> copyList(List<String> values) {
        return values == null ? List.of() : new ArrayList<>(values);
    }

    private CourtResponse mapCourtToResponse(Court court) {
        return CourtResponse.builder()
                .id(court.getId())
                .venueId(court.getVenue().getId())
                .venueName(court.getVenue().getName())
                .sportId(court.getSport().getId())
                .sportName(court.getSport().getName())
                .name(court.getName())
                .hourlyRate(court.getHourlyRate())
                .durationMinutes(court.getDurationMinutes())
                .status(court.getStatus())
                .build();
    }

    private SportResponse mapSportToResponse(Sport sport) {
        return SportResponse.builder()
                .id(sport.getId())
                .name(sport.getName())
                .icon(sport.getIcon())
                .description(sport.getDescription())
                .isActive(sport.isActive())
                .build();
    }
}
