package lk.booknplay.service.impl;

import lk.booknplay.dto.request.CancellationPolicyRequest;
import lk.booknplay.dto.request.OperatingHoursUpdateRequest;
import lk.booknplay.dto.request.VenueOnboardRequest;
import lk.booknplay.dto.request.OwnerVenueRequest;
import lk.booknplay.dto.request.VenueImagesRequest;
import lk.booknplay.dto.response.CancellationPolicyResponse;
import lk.booknplay.dto.response.CourtResponse;
import lk.booknplay.dto.response.OperatingHoursResponse;
import lk.booknplay.dto.response.VenueResponse;
import lk.booknplay.entity.*;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.repository.*;
import lk.booknplay.service.OwnerAccessService;
import lk.booknplay.service.OwnerVenueService;
import lk.booknplay.util.SportCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerVenueServiceImpl implements OwnerVenueService {

    private final OwnerAccessService ownerAccessService;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final VenueImageRepository venueImageRepository;
    private final OperatingHoursRepository operatingHoursRepository;
    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final SportRepository sportRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VenueResponse> listVenues(String ownerEmail) {
        Business business = ownerAccessService.requireBusiness(ownerEmail);
        return venueRepository.findByBusinessId(business.getId()).stream()
                .map(this::mapVenue)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VenueResponse getVenue(String ownerEmail, String venueId) {
        Venue venue = ownerAccessService.requireVenue(ownerEmail, venueId);
        VenueResponse response = mapVenue(venue);
        response.setCourts(courtRepository.findByVenueId(venueId).stream().map(this::mapCourt).toList());
        return response;
    }

    @Override
    @Transactional
    public VenueResponse createVenue(String ownerEmail, OwnerVenueRequest request) {
        Business business = ownerAccessService.requireBusiness(ownerEmail);
        Venue venue = Venue.builder()
                .business(business)
                .name(defaultVenueName(business, null, request.getName()))
                .address(request.getAddress())
                .city(request.getCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .description(request.getDescription())
                .coverImageUrl(request.getCoverImageUrl())
                .amenities(request.getAmenities() != null ? new ArrayList<>(request.getAmenities()) : new ArrayList<>())
                .status(VenueStatus.ACTIVE)
                .build();
        Venue saved = venueRepository.save(venue);
        replaceVenueImages(saved, request.getImageUrls());
        return mapVenue(saved);
    }

    @Override
    @Transactional
    public VenueResponse updateVenue(String ownerEmail, String venueId, OwnerVenueRequest request) {
        Venue venue = ownerAccessService.requireVenue(ownerEmail, venueId);
        venue.setName(request.getName());
        venue.setAddress(request.getAddress());
        venue.setCity(request.getCity());
        venue.setLatitude(request.getLatitude());
        venue.setLongitude(request.getLongitude());
        venue.setDescription(request.getDescription());
        venue.setCoverImageUrl(request.getCoverImageUrl());
        venue.getAmenities().clear();
        if (request.getAmenities() != null) {
            venue.getAmenities().addAll(request.getAmenities());
        }
        if (request.getImageUrls() != null) {
            replaceVenueImages(venue, request.getImageUrls());
        }
        return mapVenue(venueRepository.save(venue));
    }

    @Override
    @Transactional
    public VenueResponse replaceImages(String ownerEmail, String venueId, VenueImagesRequest request) {
        Venue venue = ownerAccessService.requireVenue(ownerEmail, venueId);
        replaceVenueImages(venue, request.getImageUrls());
        return mapVenue(venue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperatingHoursResponse> getOperatingHours(String ownerEmail, String venueId) {
        ownerAccessService.requireVenue(ownerEmail, venueId);
        return operatingHoursRepository.findByVenueId(venueId).stream().map(this::mapHours).toList();
    }

    @Override
    @Transactional
    public List<OperatingHoursResponse> replaceOperatingHours(String ownerEmail, String venueId, OperatingHoursUpdateRequest request) {
        Venue venue = ownerAccessService.requireVenue(ownerEmail, venueId);
        operatingHoursRepository.deleteByVenueId(venueId);
        operatingHoursRepository.flush();
        List<OperatingHours> saved = request.getDays().stream()
                .map(day -> OperatingHours.builder()
                        .venue(venue)
                        .dayOfWeek(day.getDayOfWeek())
                        .openTime(day.getOpenTime() != null ? day.getOpenTime() : java.time.LocalTime.of(6, 0))
                        .closeTime(day.getCloseTime() != null ? day.getCloseTime() : java.time.LocalTime.of(22, 0))
                        .isClosed(day.isClosed())
                        .build())
                .map(operatingHoursRepository::save)
                .toList();
        return saved.stream().map(this::mapHours).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CancellationPolicyResponse getCancellationPolicy(String ownerEmail) {
        Business business = ownerAccessService.requireBusiness(ownerEmail);
        CancellationPolicy policy = cancellationPolicyRepository.findByBusinessId(business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cancellation policy not set"));
        return mapPolicy(policy);
    }

    @Override
    @Transactional
    public CancellationPolicyResponse upsertCancellationPolicy(String ownerEmail, CancellationPolicyRequest request) {
        Business business = ownerAccessService.requireBusiness(ownerEmail);
        CancellationPolicy policy = cancellationPolicyRepository.findByBusinessId(business.getId())
                .orElse(CancellationPolicy.builder().business(business).build());
        policy.setHoursBeforeDeadline(request.getHoursBeforeDeadline());
        policy.setRefundPercentage(request.getRefundPercentage());
        return mapPolicy(cancellationPolicyRepository.save(policy));
    }

    @Override
    @Transactional
    public VenueResponse onboardVenue(String ownerEmail, VenueOnboardRequest request) {
        Business business = ownerAccessService.requireBusiness(ownerEmail);
        String city = request.getCity();
        if (city == null || city.isBlank()) {
            String[] parts = request.getFormattedAddress().split(",");
            city = parts[parts.length - 1].trim();
        }
        if (city == null || city.isBlank() || city.length() > 80) {
            city = "Colombo";
        }

        String venueType = request.getVenueType();
        if (venueType == null || venueType.isBlank()) {
            venueType = request.getFacilities().size() > 1
                    ? "Multi-sport"
                    : request.getFacilities().get(0).getSportName();
        }

        Venue venue = venueRepository.save(Venue.builder()
                .business(business)
                .name(defaultVenueName(business, venueType, request.getName()))
                .address(request.getFormattedAddress())
                .formattedAddress(request.getFormattedAddress())
                .city(city)
                .venueType(venueType)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .description(request.getDescription())
                .amenities(request.getAmenities() != null ? new ArrayList<>(request.getAmenities()) : new ArrayList<>())
                .rules(request.getRulePresets() != null ? new ArrayList<>(request.getRulePresets()) : new ArrayList<>())
                .additionalRules(request.getAdditionalRules())
                .status(VenueStatus.ACTIVE)
                .build());

        for (var facility : request.getFacilities()) {
            Sport sport = resolveSport(facility.getSportName());
            int quantity = Math.max(facility.getQuantity(), 1);
            for (int i = 0; i < quantity; i++) {
                String courtName = (facility.getCourtNames() != null && i < facility.getCourtNames().size()
                        && facility.getCourtNames().get(i) != null && !facility.getCourtNames().get(i).isBlank())
                        ? facility.getCourtNames().get(i)
                        : SportCatalog.unitName(facility.getSportName(), i);
                courtRepository.save(Court.builder()
                        .venue(venue)
                        .sport(sport)
                        .name(courtName)
                        .hourlyRate(facility.getPrice())
                        .durationMinutes(facility.getDurationMinutes() != null ? facility.getDurationMinutes() : 60)
                        .status(lk.booknplay.enums.CourtStatus.ACTIVE)
                        .build());
            }
        }

        operatingHoursRepository.deleteByVenueId(venue.getId());
        for (var day : request.getHours()) {
            operatingHoursRepository.save(OperatingHours.builder()
                    .venue(venue)
                    .dayOfWeek(day.getDayOfWeek())
                    .openTime(day.getOpenTime() != null ? day.getOpenTime() : java.time.LocalTime.of(6, 0))
                    .closeTime(day.getCloseTime() != null ? day.getCloseTime() : java.time.LocalTime.of(23, 0))
                    .isClosed(day.isClosed())
                    .build());
        }

        return getVenue(ownerEmail, venue.getId());
    }

    private String defaultVenueName(Business business, String venueType, String name) {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        String businessName = business.getName() != null && !business.getName().isBlank()
                ? business.getName().trim()
                : "Venue";
        if (venueType != null && !venueType.isBlank()) {
            return businessName + " - " + venueType.trim();
        }
        return businessName;
    }

    private Sport resolveSport(String rawName) {
        String canonical = SportCatalog.canonicalName(rawName);
        return sportRepository.findByNameIgnoreCase(canonical)
                .orElseGet(() -> sportRepository.save(Sport.builder()
                        .name(canonical)
                        .isActive(true)
                        .build()));
    }

    private void replaceVenueImages(Venue venue, List<String> imageUrls) {
        venueImageRepository.deleteByVenueId(venue.getId());
        venueImageRepository.flush();
        if (imageUrls == null) {
            return;
        }
        int order = 0;
        for (String url : imageUrls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            venueImageRepository.save(VenueImage.builder()
                    .venue(venue)
                    .url(url)
                    .sortOrder(order++)
                    .build());
        }
    }

    private VenueResponse mapVenue(Venue venue) {
        List<VenueImage> images = venueImageRepository.findByVenueIdOrderBySortOrderAsc(venue.getId());
        List<Court> courts = courtRepository.findByVenueId(venue.getId());
        List<OperatingHours> hours = operatingHoursRepository.findByVenueId(venue.getId());
        java.math.BigDecimal starting = courts.stream()
                .map(Court::getHourlyRate)
                .min(java.math.BigDecimal::compareTo)
                .orElse(null);
        int filled = 0;
        if (venue.getDescription() != null && !venue.getDescription().isBlank()) filled++;
        if (venue.getAmenities() != null && !venue.getAmenities().isEmpty()) filled++;
        if (venue.getRules() != null && !venue.getRules().isEmpty()) filled++;
        if (hours.size() >= 7) filled++;
        if (!courts.isEmpty()) filled++;
        int setup = Math.round((filled / 5f) * 100);

        String businessImage = null;
        if (venue.getBusiness() != null) {
            businessImage = venue.getBusiness().getLogoUrl();
        }
        if (businessImage == null) {
            businessImage = images.isEmpty() ? venue.getCoverImageUrl() : images.get(0).getUrl();
        }

        String sportName = courts.stream()
                .map(court -> court.getSport() != null ? court.getSport().getName() : null)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(venue.getVenueType());

        return VenueResponse.builder()
                .id(venue.getId())
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
                .coverImageUrl(venue.getCoverImageUrl())
                .businessImageUrl(businessImage)
                .startingPrice(starting)
                .currency("LKR")
                .amenities(venue.getAmenities() == null ? List.of() : new java.util.ArrayList<>(venue.getAmenities()))
                .rules(venue.getRules() == null ? List.of() : new java.util.ArrayList<>(venue.getRules()))
                .additionalRules(venue.getAdditionalRules())
                .images(images.stream().map(VenueImage::getUrl).toList())
                .courts(courts.stream().map(this::mapCourt).toList())
                .setupPercent(setup)
                .build();
    }

    private CourtResponse mapCourt(Court court) {
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

    private OperatingHoursResponse mapHours(OperatingHours hours) {
        return OperatingHoursResponse.builder()
                .id(hours.getId())
                .dayOfWeek(hours.getDayOfWeek())
                .openTime(hours.getOpenTime())
                .closeTime(hours.getCloseTime())
                .closed(hours.isClosed())
                .build();
    }

    private CancellationPolicyResponse mapPolicy(CancellationPolicy policy) {
        return CancellationPolicyResponse.builder()
                .id(policy.getId())
                .hoursBeforeDeadline(policy.getHoursBeforeDeadline())
                .refundPercentage(policy.getRefundPercentage())
                .build();
    }
}
