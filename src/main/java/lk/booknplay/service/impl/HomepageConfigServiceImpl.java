package lk.booknplay.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.booknplay.dto.request.HomepageConfigRequest;
import lk.booknplay.dto.request.PremiumSlideRequest;
import lk.booknplay.dto.response.HomepageConfigResponse;
import lk.booknplay.dto.response.PremiumSlideResponse;
import lk.booknplay.entity.*;
import lk.booknplay.enums.HomepageConfigStatus;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.repository.*;
import lk.booknplay.service.HomepageConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HomepageConfigServiceImpl implements HomepageConfigService {
    private static final List<String> DEFAULT_ORDER = List.of("sports", "businesses", "venues", "cities", "howItWorks", "ownerPromotion", "trust");
    private static final Set<String> ALLOWED_SECTIONS = Set.copyOf(DEFAULT_ORDER);
    private static final Set<String> INTENSITIES = Set.of("NONE", "SUBTLE", "ENERGETIC");
    private static final List<VenueStatus> LIVE_STATUSES = List.of(VenueStatus.ACTIVE, VenueStatus.APPROVED);

    private final HomepageConfigRepository configs;
    private final BusinessRepository businesses;
    private final VenueRepository venues;
    private final SportRepository sports;
    private final UserRepository users;
    private final CourtRepository courts;
    private final AdminAuditLogRepository audit;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public HomepageConfigResponse publicConfig() {
        return configs.findFirstByStatusOrderByVersionDesc(HomepageConfigStatus.PUBLISHED)
                .map(config -> response(config, true))
                .orElseGet(() -> response(defaultConfig(0, HomepageConfigStatus.PUBLISHED), true));
    }

    @Override
    @Transactional
    public HomepageConfigResponse draft() {
        return response(getOrCreateDraft(), false);
    }

    @Override
    @Transactional
    public HomepageConfigResponse saveDraft(HomepageConfigRequest request, String adminEmail) {
        validate(request);
        HomepageConfig draft = getOrCreateDraft();
        apply(draft, request);
        draft.setUpdatedBy(adminEmail);
        audit(adminEmail, "HOMEPAGE_DRAFT_SAVED", draft.getId(), null);
        return response(configs.save(draft), false);
    }

    @Override
    @Transactional
    public HomepageConfigResponse publish(String reason, String adminEmail) {
        HomepageConfig draft = getOrCreateDraft();
        configs.findFirstByStatusOrderByVersionDesc(HomepageConfigStatus.PUBLISHED).ifPresent(current -> {
            current.setStatus(HomepageConfigStatus.ARCHIVED);
            configs.save(current);
        });
        draft.setStatus(HomepageConfigStatus.PUBLISHED);
        draft.setPublishedAt(LocalDateTime.now());
        draft.setUpdatedBy(adminEmail);
        configs.save(draft);
        audit(adminEmail, "HOMEPAGE_PUBLISHED", draft.getId(), reason);
        HomepageConfig nextDraft = copy(draft, draft.getVersion() + 1, HomepageConfigStatus.DRAFT);
        nextDraft.setPublishedAt(null);
        configs.save(nextDraft);
        return response(draft, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomepageConfigResponse> versions() {
        return configs.findAllByOrderByVersionDesc().stream().map(config -> response(config, false)).toList();
    }

    @Override
    @Transactional
    public HomepageConfigResponse restore(String id, String reason, String adminEmail) {
        HomepageConfig source = configs.findById(id).orElseThrow(() -> new ResourceNotFoundException("Homepage version not found"));
        HomepageConfig draft = getOrCreateDraft();
        copyValues(source, draft);
        draft.setUpdatedBy(adminEmail);
        configs.save(draft);
        audit(adminEmail, "HOMEPAGE_VERSION_RESTORED", source.getId(), reason);
        return publish(reason, adminEmail);
    }

    private HomepageConfig getOrCreateDraft() {
        return configs.findFirstByStatusOrderByVersionDesc(HomepageConfigStatus.DRAFT).orElseGet(() -> {
            HomepageConfig published = configs.findFirstByStatusOrderByVersionDesc(HomepageConfigStatus.PUBLISHED).orElse(null);
            return configs.save(published == null
                    ? defaultConfig(1, HomepageConfigStatus.DRAFT)
                    : copy(published, published.getVersion() + 1, HomepageConfigStatus.DRAFT));
        });
    }

    private HomepageConfig defaultConfig(int version, HomepageConfigStatus status) {
        return HomepageConfig.builder()
                .version(version)
                .status(status)
                .eyebrow("Sports venues across Sri Lanka")
                .heading("Find the right place for your next game.")
                .description("Explore trusted sports businesses, compare their venues and book a time that works for your team.")
                .sectionOrder(csv(DEFAULT_ORDER))
                .premiumSliderPauseOnHover(true)
                .animationIntensity("SUBTLE")
                .build();
    }

    private HomepageConfig copy(HomepageConfig source, int version, HomepageConfigStatus status) {
        HomepageConfig target = HomepageConfig.builder().version(version).status(status).build();
        copyValues(source, target);
        return target;
    }

    private void copyValues(HomepageConfig s, HomepageConfig t) {
        t.setEyebrow(s.getEyebrow());
        t.setHeading(s.getHeading());
        t.setDescription(s.getDescription());
        t.setShowSearch(s.isShowSearch());
        t.setShowSports(s.isShowSports());
        t.setShowBusinesses(s.isShowBusinesses());
        t.setShowVenues(s.isShowVenues());
        t.setShowCities(s.isShowCities());
        t.setShowHowItWorks(s.isShowHowItWorks());
        t.setShowOwnerPromotion(s.isShowOwnerPromotion());
        t.setShowTrust(s.isShowTrust());
        t.setSectionOrder(s.getSectionOrder());
        t.setFeaturedBusinessIds(s.getFeaturedBusinessIds());
        t.setFeaturedVenueIds(s.getFeaturedVenueIds());
        t.setFeaturedSportIds(s.getFeaturedSportIds());
        t.setPremiumSliderEnabled(s.isPremiumSliderEnabled());
        t.setPremiumSliderAutoplay(s.isPremiumSliderAutoplay());
        t.setPremiumSliderSeconds(s.getPremiumSliderSeconds());
        t.setPremiumSliderArrows(s.isPremiumSliderArrows());
        t.setPremiumSliderIndicators(s.isPremiumSliderIndicators());
        t.setPremiumSliderPauseOnHover(s.isPremiumSliderPauseOnHover());
        t.setAnimationIntensity(s.getAnimationIntensity());
        t.setPremiumBusinessIds(s.getPremiumBusinessIds());
        t.setPremiumSlides(s.getPremiumSlides());
    }

    private void apply(HomepageConfig c, HomepageConfigRequest r) {
        List<PremiumSlideRequest> slides = normalizeSlides(r);
        c.setEyebrow(r.getEyebrow());
        c.setHeading(r.getHeading().trim());
        c.setDescription(r.getDescription());
        c.setShowSearch(r.isShowSearch());
        c.setShowSports(r.isShowSports());
        c.setShowBusinesses(r.isShowBusinesses());
        c.setShowVenues(r.isShowVenues());
        c.setShowCities(r.isShowCities());
        c.setShowHowItWorks(r.isShowHowItWorks());
        c.setShowOwnerPromotion(r.isShowOwnerPromotion());
        c.setShowTrust(r.isShowTrust());
        c.setSectionOrder(csv(r.getSectionOrder()));
        c.setFeaturedBusinessIds(csv(r.getFeaturedBusinessIds()));
        c.setFeaturedVenueIds(csv(r.getFeaturedVenueIds()));
        c.setFeaturedSportIds(csv(r.getFeaturedSportIds()));
        c.setPremiumSliderEnabled(r.isPremiumSliderEnabled());
        c.setPremiumSliderAutoplay(r.isPremiumSliderAutoplay());
        c.setPremiumSliderSeconds(r.getPremiumSliderSeconds() == 0 ? 6 : r.getPremiumSliderSeconds());
        c.setPremiumSliderArrows(r.isPremiumSliderArrows());
        c.setPremiumSliderIndicators(r.isPremiumSliderIndicators());
        c.setPremiumSliderPauseOnHover(r.isPremiumSliderPauseOnHover());
        c.setAnimationIntensity(intensity(r.getAnimationIntensity()));
        c.setPremiumBusinessIds(csv(slides.stream().filter(PremiumSlideRequest::isEnabled).map(PremiumSlideRequest::getBusinessId).toList()));
        c.setPremiumSlides(writeSlides(slides));
    }

    private void validate(HomepageConfigRequest r) {
        List<String> order = safe(r.getSectionOrder());
        if (order.size() != new HashSet<>(order).size() || !ALLOWED_SECTIONS.containsAll(order)) {
            throw new BadRequestException("Invalid homepage section order");
        }
        safe(r.getFeaturedBusinessIds()).forEach(id -> {
            if (!businesses.existsById(id)) throw new BadRequestException("Unknown business: " + id);
        });
        safe(r.getFeaturedVenueIds()).forEach(id -> {
            if (!venues.existsById(id)) throw new BadRequestException("Unknown venue: " + id);
        });
        safe(r.getFeaturedSportIds()).forEach(id -> {
            if (!sports.existsById(id)) throw new BadRequestException("Unknown sport: " + id);
        });
        int seconds = r.getPremiumSliderSeconds() == 0 ? 6 : r.getPremiumSliderSeconds();
        if (seconds < 4 || seconds > 15) {
            throw new BadRequestException("Premium slider timing must be between 4 and 15 seconds");
        }
        if (r.getAnimationIntensity() != null && !r.getAnimationIntensity().isBlank()
                && !INTENSITIES.contains(r.getAnimationIntensity().trim().toUpperCase())) {
            throw new BadRequestException("Animation intensity must be NONE, SUBTLE or ENERGETIC");
        }
        List<PremiumSlideRequest> slides = normalizeSlides(r);
        if (slides.size() > 5) throw new BadRequestException("Select no more than 5 premium businesses");
        Set<String> ids = new HashSet<>();
        for (PremiumSlideRequest slide : slides) {
            if (slide.getBusinessId() == null || slide.getBusinessId().isBlank()) {
                throw new BadRequestException("Each premium slide needs a business");
            }
            if (!ids.add(slide.getBusinessId())) {
                throw new BadRequestException("A business can only appear once in the premium slider");
            }
            requireEligibleBusiness(slide);
        }
    }

    private void requireEligibleBusiness(PremiumSlideRequest slide) {
        Business business = businesses.findById(slide.getBusinessId())
                .orElseThrow(() -> new BadRequestException("Unknown premium business: " + slide.getBusinessId()));
        User owner = users.findById(business.getOwnerId()).orElse(null);
        if (owner == null || !owner.isEnabled() || owner.isLocked()) {
            throw new BadRequestException(business.getName() + " is suspended and cannot be featured");
        }
        List<Venue> liveVenues = liveVenues(business.getId());
        if (liveVenues.isEmpty()) {
            throw new BadRequestException(business.getName() + " has no live venues");
        }
        String imageUrl = blank(slide.getImageUrl());
        if (imageUrl != null && !allowedImages(business).contains(imageUrl)) {
            throw new BadRequestException("Premium slide image must be one of the business uploads");
        }
    }

    private HomepageConfigResponse response(HomepageConfig c, boolean publicView) {
        List<PremiumSlideResponse> slides = publicView && !c.isPremiumSliderEnabled()
                ? List.of()
                : resolveSlides(c, publicView);
        return HomepageConfigResponse.builder()
                .id(c.getId())
                .version(c.getVersion())
                .status(c.getStatus())
                .eyebrow(c.getEyebrow())
                .heading(c.getHeading())
                .description(c.getDescription())
                .showSearch(c.isShowSearch())
                .showSports(c.isShowSports())
                .showBusinesses(c.isShowBusinesses())
                .showVenues(c.isShowVenues())
                .showCities(c.isShowCities())
                .showHowItWorks(c.isShowHowItWorks())
                .showOwnerPromotion(c.isShowOwnerPromotion())
                .showTrust(c.isShowTrust())
                .sectionOrder(list(c.getSectionOrder()))
                .featuredBusinessIds(list(c.getFeaturedBusinessIds()))
                .featuredVenueIds(list(c.getFeaturedVenueIds()))
                .featuredSportIds(list(c.getFeaturedSportIds()))
                .premiumSliderEnabled(c.isPremiumSliderEnabled())
                .premiumSliderAutoplay(c.isPremiumSliderAutoplay())
                .premiumSliderSeconds(c.getPremiumSliderSeconds())
                .premiumSliderArrows(c.isPremiumSliderArrows())
                .premiumSliderIndicators(c.isPremiumSliderIndicators())
                .premiumSliderPauseOnHover(c.isPremiumSliderPauseOnHover())
                .animationIntensity(intensity(c.getAnimationIntensity()))
                .premiumBusinessIds(slides.stream().map(PremiumSlideResponse::getBusinessId).toList())
                .premiumSlides(slides)
                .updatedBy(c.getUpdatedBy())
                .updatedAt(c.getUpdatedAt())
                .publishedAt(c.getPublishedAt())
                .build();
    }

    private List<PremiumSlideResponse> resolveSlides(HomepageConfig config, boolean publicView) {
        List<PremiumSlideRequest> stored = readSlides(config);
        if (stored.isEmpty() && !list(config.getPremiumBusinessIds()).isEmpty()) {
            stored = list(config.getPremiumBusinessIds()).stream().map(id -> {
                PremiumSlideRequest slide = new PremiumSlideRequest();
                slide.setId(id);
                slide.setBusinessId(id);
                slide.setEnabled(true);
                return slide;
            }).toList();
        }
        List<PremiumSlideResponse> resolved = new ArrayList<>();
        int order = 0;
        for (PremiumSlideRequest slide : stored) {
            Business business = businesses.findById(slide.getBusinessId()).orElse(null);
            if (business == null) {
                if (!publicView) continue;
                continue;
            }
            List<Venue> liveVenues = liveVenues(business.getId());
            User owner = users.findById(business.getOwnerId()).orElse(null);
            boolean eligible = slide.isEnabled()
                    && owner != null && owner.isEnabled() && !owner.isLocked()
                    && !liveVenues.isEmpty()
                    && withinWindow(slide);
            if (publicView && !eligible) continue;
            resolved.add(toSlideResponse(slide, business, liveVenues, order++));
        }
        return resolved;
    }

    private PremiumSlideResponse toSlideResponse(PremiumSlideRequest slide, Business business, List<Venue> liveVenues, int order) {
        List<String> allowed = allowedImages(business);
        String image = blank(slide.getImageUrl());
        if (image == null || !allowed.contains(image)) {
            image = allowed.isEmpty() ? business.getLogoUrl() : allowed.get(0);
        }
        return PremiumSlideResponse.builder()
                .id(slide.getId() == null || slide.getId().isBlank() ? business.getId() : slide.getId())
                .businessId(business.getId())
                .enabled(slide.isEnabled())
                .sortOrder(order)
                .headline(blank(slide.getHeadline()) != null ? slide.getHeadline() : business.getName())
                .description(blank(slide.getDescription()) != null ? slide.getDescription() : business.getAddress())
                .badge(blank(slide.getBadge()) != null ? slide.getBadge() : "Premium partner")
                .imageUrl(image)
                .primaryActionLabel(blank(slide.getPrimaryActionLabel()) != null ? slide.getPrimaryActionLabel() : "Explore venues")
                .startsAt(slide.getStartsAt())
                .expiresAt(slide.getExpiresAt())
                .name(business.getName())
                .logoUrl(business.getLogoUrl())
                .venueCount(liveVenues.size())
                .cities(liveVenues.stream().map(Venue::getCity).filter(city -> city != null && !city.isBlank()).distinct().toList())
                .sports(sportsFor(liveVenues))
                .build();
    }

    private List<String> sportsFor(List<Venue> liveVenues) {
        return liveVenues.stream()
                .flatMap(venue -> courts.findByVenueId(venue.getId()).stream())
                .map(court -> court.getSport() != null ? court.getSport().getName() : null)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();
    }

    private List<Venue> liveVenues(String businessId) {
        return venues.findByBusinessId(businessId).stream()
                .filter(venue -> LIVE_STATUSES.contains(venue.getStatus()))
                .toList();
    }

    private List<String> allowedImages(Business business) {
        List<String> urls = new ArrayList<>();
        if (business.getLogoUrl() != null && !business.getLogoUrl().isBlank()) urls.add(business.getLogoUrl());
        if (business.getImages() != null) {
            business.getImages().stream()
                    .map(BusinessImage::getUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .forEach(urls::add);
        }
        return urls.stream().distinct().toList();
    }

    private boolean withinWindow(PremiumSlideRequest slide) {
        LocalDate today = LocalDate.now();
        LocalDate start = parseDate(slide.getStartsAt());
        LocalDate end = parseDate(slide.getExpiresAt());
        if (start != null && today.isBefore(start)) return false;
        return end == null || !today.isAfter(end);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<PremiumSlideRequest> normalizeSlides(HomepageConfigRequest request) {
        if (request.getPremiumSlides() != null && !request.getPremiumSlides().isEmpty()) {
            List<PremiumSlideRequest> slides = new ArrayList<>(request.getPremiumSlides());
            slides.sort(Comparator.comparingInt(PremiumSlideRequest::getSortOrder));
            int index = 0;
            for (PremiumSlideRequest slide : slides) {
                if (slide.getId() == null || slide.getId().isBlank()) {
                    slide.setId(UUID.randomUUID().toString());
                }
                slide.setSortOrder(index++);
                if (slide.getBusinessId() != null) slide.setBusinessId(slide.getBusinessId().trim());
            }
            return slides;
        }
        List<PremiumSlideRequest> fromIds = new ArrayList<>();
        int index = 0;
        for (String id : safe(request.getPremiumBusinessIds())) {
            PremiumSlideRequest slide = new PremiumSlideRequest();
            slide.setId(UUID.randomUUID().toString());
            slide.setBusinessId(id);
            slide.setEnabled(true);
            slide.setSortOrder(index++);
            fromIds.add(slide);
        }
        return fromIds;
    }

    private List<PremiumSlideRequest> readSlides(HomepageConfig config) {
        if (config.getPremiumSlides() == null || config.getPremiumSlides().isBlank()) return List.of();
        try {
            return objectMapper.readValue(config.getPremiumSlides(), new TypeReference<>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String writeSlides(List<PremiumSlideRequest> slides) {
        try {
            return objectMapper.writeValueAsString(slides);
        } catch (Exception error) {
            throw new BadRequestException("Could not save premium slides");
        }
    }

    private String intensity(String value) {
        if (value == null || value.isBlank()) return "SUBTLE";
        return value.trim().toUpperCase();
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void audit(String email, String action, String id, String reason) {
        audit.save(AdminAuditLog.builder().adminEmail(email).action(action).resourceType("HOMEPAGE").resourceId(id).reason(reason).build());
    }

    private List<String> safe(List<String> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).map(String::trim).filter(v -> !v.isBlank()).distinct().toList();
    }

    private String csv(List<String> values) {
        return String.join(",", safe(values));
    }

    private List<String> list(String value) {
        return value == null || value.isBlank() ? List.of() : Arrays.stream(value.split(",")).map(String::trim).filter(v -> !v.isBlank()).toList();
    }
}
