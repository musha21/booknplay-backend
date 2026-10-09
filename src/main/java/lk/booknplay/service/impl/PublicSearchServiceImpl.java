package lk.booknplay.service.impl;

import lk.booknplay.dto.response.AvailabilityResponse;
import lk.booknplay.dto.response.AvailabilitySlotResponse;
import lk.booknplay.dto.response.BusinessResponse;
import lk.booknplay.dto.response.CourtResponse;
import lk.booknplay.dto.response.OperatingHoursResponse;
import lk.booknplay.dto.response.OwnerReviewResponse;
import lk.booknplay.dto.response.PromotionResponse;
import lk.booknplay.dto.response.SportResponse;
import lk.booknplay.dto.response.VenueResponse;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.OperatingHours;
import lk.booknplay.entity.Promotion;
import lk.booknplay.entity.Review;
import lk.booknplay.entity.Sport;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.CourtStatus;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.repository.BusinessRepository;
import lk.booknplay.repository.CourtRepository;
import lk.booknplay.repository.OperatingHoursRepository;
import lk.booknplay.repository.PromotionRepository;
import lk.booknplay.repository.ReviewRepository;
import lk.booknplay.repository.SportRepository;
import lk.booknplay.repository.VenueRepository;
import lk.booknplay.service.AvailabilityService;
import lk.booknplay.service.PublicSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PublicSearchServiceImpl implements PublicSearchService {

    private static final double EARTH_KM = 6371.0;
    private static final double DEFAULT_RADIUS_KM = 25.0;

    private final VenueRepository venueRepository;
    private final BusinessRepository businessRepository;
    private final CourtRepository courtRepository;
    private final SportRepository sportRepository;
    private final OperatingHoursRepository operatingHoursRepository;
    private final ReviewRepository reviewRepository;
    private final PromotionRepository promotionRepository;
    private final AvailabilityService availabilityService;

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
    public Page<VenueResponse> searchVenues(
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
            Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted()
                ? PageRequest.of(0, 500, pageable.getSort())
                : PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "createdAt"));
        String cityFilter = (lat != null && lng != null) ? null : blankToNull(city);
        List<VenueResponse> mapped = venueRepository.searchVenues(
                        List.of(VenueStatus.ACTIVE, VenueStatus.APPROVED),
                        cityFilter,
                        blankToNull(sportId),
                        blankToNull(name),
                        sorted)
                .getContent()
                .stream()
                .map(venue -> mapVenueToResponse(venue, date, time, blankToNull(sportId)))
                .filter(venue -> matchesPrice(venue, minPrice, maxPrice))
                .filter(venue -> matchesAmenity(venue, amenity))
                .filter(venue -> matchesIndoorOutdoor(venue, indoorOutdoor))
                .filter(venue -> matchesRating(venue, minRating))
                .filter(venue -> matchesRadius(venue, lat, lng, radiusKm))
                .filter(venue -> date == null || (venue.getAvailableCourtCount() != null && venue.getAvailableCourtCount() > 0))
                .toList();

        int page = Math.max(pageable.getPageNumber(), 0);
        int size = Math.max(pageable.getPageSize(), 1);
        int from = Math.min(page * size, mapped.size());
        int to = Math.min(from + size, mapped.size());
        return new PageImpl<>(mapped.subList(from, to), PageRequest.of(page, size, sorted.getSort()), mapped.size());
    }

    @Override
    @Transactional(readOnly = true)
    public VenueResponse getVenueDetails(String id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + id));
        if (venue.getStatus() != VenueStatus.ACTIVE && venue.getStatus() != VenueStatus.APPROVED) {
            throw new ResourceNotFoundException("Venue not found with id: " + id);
        }

        VenueResponse response = mapVenueToResponse(venue, null, null, null);
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

    @Override
    @Transactional(readOnly = true)
    public List<OwnerReviewResponse> getVenueReviews(String venueId) {
        getVenueDetails(venueId);
        return reviewRepository.findByVenueIdOrderByCreatedAtDesc(venueId, PageRequest.of(0, 50))
                .getContent()
                .stream()
                .map(this::mapReview)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponse> getPublicPromotions() {
        return promotionRepository.findActivePublic(LocalDate.now()).stream()
                .map(this::mapPromotion)
                .toList();
    }

    private VenueResponse mapVenueToResponse(Venue venue, LocalDate date, LocalTime time, String sportId) {
        List<Court> courts = courtRepository.findByVenueId(venue.getId());
        List<Court> active = courts.stream().filter(court -> court.getStatus() == CourtStatus.ACTIVE).toList();
        java.math.BigDecimal starting = active.stream()
                .map(Court::getHourlyRate)
                .min(java.math.BigDecimal::compareTo)
                .orElse(null);
        String sportName = active.stream()
                .map(court -> court.getSport() != null ? court.getSport().getName() : null)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(venue.getVenueType());
        String businessLogo = venue.getBusiness() != null ? venue.getBusiness().getLogoUrl() : null;
        List<lk.booknplay.entity.VenueImage> orderedImages = venue.getImages() == null
                ? List.of()
                : venue.getImages().stream()
                        .filter(img -> img.getUrl() != null && !img.getUrl().isBlank())
                        .sorted(Comparator.comparingInt(lk.booknplay.entity.VenueImage::getSortOrder))
                        .toList();
        List<String> imageUrls = orderedImages.stream().map(lk.booknplay.entity.VenueImage::getUrl).toList();
        String cover = venue.getCoverImageUrl();
        if (cover == null || cover.isBlank()) {
            cover = imageUrls.isEmpty() ? businessLogo : imageUrls.get(0);
        }
        Double avgRating = reviewRepository.averageRatingByVenueId(venue.getId());
        long reviews = reviewRepository.countByVenueId(venue.getId());
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
                .businessImageUrl(businessLogo)
                .businessLogoUrl(businessLogo)
                .startingPrice(starting)
                .currency("LKR")
                .rating(avgRating)
                .reviewCount(reviews)
                .availableCourtCount(countAvailableCourts(active, date, time, sportId))
                .operatingHours(operatingHoursRepository.findByVenueId(venue.getId()).stream()
                        .sorted(Comparator.comparing(OperatingHours::getDayOfWeek))
                        .map(this::mapHours)
                        .toList())
                .amenities(copyList(venue.getAmenities()))
                .rules(copyList(venue.getRules()))
                .additionalRules(venue.getAdditionalRules())
                .images(imageUrls)
                .media(orderedImages.stream()
                        .map(img -> lk.booknplay.dto.response.MediaResponse.builder()
                                .id(img.getId())
                                .url(img.getUrl())
                                .sortOrder(img.getSortOrder())
                                .build())
                        .toList())
                .build();
    }

    private int countAvailableCourts(List<Court> courts, LocalDate date, LocalTime time, String sportId) {
        List<Court> relevant = courts.stream()
                .filter(court -> sportId == null || (court.getSport() != null && sportId.equals(court.getSport().getId())))
                .toList();
        if (date == null) {
            return relevant.size();
        }
        int count = 0;
        for (Court court : relevant) {
            try {
                AvailabilityResponse availability = availabilityService.getAvailability(court.getId(), date);
                List<AvailabilitySlotResponse> slots = availability.getSlots() == null ? List.of() : availability.getSlots();
                boolean open = slots.stream().anyMatch(slot -> {
                    if (!slot.isAvailable()) return false;
                    if (time == null) return true;
                    return !time.isBefore(slot.getStartTime()) && time.isBefore(slot.getEndTime());
                });
                if (open) count++;
            } catch (RuntimeException ignored) {
                // Past dates or malformed hours: treat as unavailable.
            }
        }
        return count;
    }

    private boolean matchesPrice(VenueResponse venue, BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null && maxPrice == null) return true;
        BigDecimal price = venue.getStartingPrice();
        if (price == null) return false;
        if (minPrice != null && price.compareTo(minPrice) < 0) return false;
        return maxPrice == null || price.compareTo(maxPrice) <= 0;
    }

    private boolean matchesAmenity(VenueResponse venue, String amenity) {
        String needle = blankToNull(amenity);
        if (needle == null) return true;
        String target = needle.toLowerCase(Locale.ROOT);
        return venue.getAmenities() != null && venue.getAmenities().stream()
                .anyMatch(item -> item != null && item.toLowerCase(Locale.ROOT).contains(target));
    }

    private boolean matchesIndoorOutdoor(VenueResponse venue, String indoorOutdoor) {
        String needle = blankToNull(indoorOutdoor);
        if (needle == null) return true;
        String type = String.valueOf(venue.getVenueType() == null ? "" : venue.getVenueType()).toLowerCase(Locale.ROOT);
        String want = needle.toLowerCase(Locale.ROOT);
        if (want.contains("indoor")) return type.contains("indoor");
        if (want.contains("outdoor")) return type.contains("outdoor");
        return type.contains(want);
    }

    private boolean matchesRating(VenueResponse venue, Double minRating) {
        if (minRating == null) return true;
        double rating = venue.getRating() == null ? 0 : venue.getRating();
        return rating + 1e-6 >= minRating;
    }

    private boolean matchesRadius(VenueResponse venue, Double lat, Double lng, Double radiusKm) {
        if (lat == null || lng == null) return true;
        if (venue.getLatitude() == null || venue.getLongitude() == null) return false;
        double radius = radiusKm == null || radiusKm <= 0 ? DEFAULT_RADIUS_KM : radiusKm;
        return haversineKm(lat, lng, venue.getLatitude(), venue.getLongitude()) <= radius;
    }

    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_KM * Math.asin(Math.min(1, Math.sqrt(a)));
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

    private OwnerReviewResponse mapReview(Review review) {
        String customerName = review.getCustomer() != null
                ? review.getCustomer().getFirstName() + " " + review.getCustomer().getLastName()
                : "Customer";
        return OwnerReviewResponse.builder()
                .id(review.getId())
                .venueId(review.getVenue().getId())
                .venueName(review.getVenue().getName())
                .customerName(customerName)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private PromotionResponse mapPromotion(Promotion promotion) {
        return PromotionResponse.builder()
                .id(promotion.getId())
                .code(promotion.getCode())
                .name(promotion.getName())
                .type(promotion.getType())
                .value(promotion.getValue())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .venueIds(splitIds(promotion.getVenueIds()))
                .sportIds(splitIds(promotion.getSportIds()))
                .maxRedemptions(promotion.getMaxRedemptions())
                .redemptionCount(promotion.getRedemptionCount())
                .active(promotion.isActive())
                .build();
    }

    private static List<String> splitIds(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(",")).map(String::trim).filter(item -> !item.isEmpty()).toList();
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
