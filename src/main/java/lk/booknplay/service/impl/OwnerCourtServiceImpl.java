package lk.booknplay.service.impl;

import lk.booknplay.dto.request.CourtPricingUpdateRequest;
import lk.booknplay.dto.request.OwnerCourtRequest;
import lk.booknplay.dto.response.CourtPricingResponse;
import lk.booknplay.dto.response.CourtResponse;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.CourtPricing;
import lk.booknplay.entity.Sport;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.CourtStatus;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.repository.CourtPricingRepository;
import lk.booknplay.repository.CourtRepository;
import lk.booknplay.repository.SportRepository;
import lk.booknplay.service.OwnerAccessService;
import lk.booknplay.service.OwnerCourtService;
import lk.booknplay.service.PlanEntitlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerCourtServiceImpl implements OwnerCourtService {

    private final OwnerAccessService ownerAccessService;
    private final PlanEntitlementService planEntitlementService;
    private final CourtRepository courtRepository;
    private final SportRepository sportRepository;
    private final CourtPricingRepository courtPricingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CourtResponse> listCourts(String ownerEmail, String venueId) {
        ownerAccessService.requireVenue(ownerEmail, venueId);
        return courtRepository.findByVenueId(venueId).stream()
                .filter(c -> c.getStatus() != CourtStatus.DELETED)
                .map(this::mapCourt)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourtResponse getCourt(String ownerEmail, String courtId) {
        return mapCourt(ownerAccessService.requireCourt(ownerEmail, courtId));
    }

    @Override
    @Transactional
    public CourtResponse createCourt(String ownerEmail, String venueId, OwnerCourtRequest request) {
        Venue venue = ownerAccessService.requireMutableVenue(ownerEmail, venueId);
        planEntitlementService.assertCanCreateCourt(venue.getBusiness(), venueId);
        Sport sport = resolveSport(request);
        Court court = Court.builder()
                .venue(venue)
                .sport(sport)
                .name(request.getName())
                .hourlyRate(request.getHourlyRate())
                .facilityType(request.getFacilityType())
                .status(CourtStatus.ACTIVE)
                .build();
        return mapCourt(courtRepository.save(court));
    }

    @Override
    @Transactional
    public CourtResponse updateCourt(String ownerEmail, String courtId, OwnerCourtRequest request) {
        Court court = ownerAccessService.requireMutableCourt(ownerEmail, courtId);
        Sport sport = resolveSport(request);
        court.setName(request.getName());
        court.setSport(sport);
        court.setHourlyRate(request.getHourlyRate());
        court.setFacilityType(request.getFacilityType());
        return mapCourt(courtRepository.save(court));
    }

    @Override
    @Transactional
    public void deleteCourt(String ownerEmail, String courtId) {
        Court court = ownerAccessService.requireMutableCourt(ownerEmail, courtId);
        court.setStatus(CourtStatus.DELETED);
        courtRepository.save(court);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourtPricingResponse> getPricing(String ownerEmail, String courtId) {
        ownerAccessService.requireCourt(ownerEmail, courtId);
        return courtPricingRepository.findByCourtId(courtId).stream().map(this::mapPricing).toList();
    }

    @Override
    @Transactional
    public List<CourtPricingResponse> replacePricing(String ownerEmail, String courtId, CourtPricingUpdateRequest request) {
        Court court = ownerAccessService.requireMutableCourt(ownerEmail, courtId);
        List<CourtPricingUpdateRequest.PricingRule> rules =
                request.getRules() != null ? request.getRules() : List.of();
        for (CourtPricingUpdateRequest.PricingRule rule : rules) {
            if (!rule.getStartTime().isBefore(rule.getEndTime())) {
                throw new BadRequestException("Pricing start time must be before end time");
            }
        }
        courtPricingRepository.deleteByCourtId(courtId);
        courtPricingRepository.flush();
        return rules.stream()
                .map(rule -> {
                    var ruleType = lk.booknplay.util.CourtPriceResolver.inferType(
                            rule.getDayOfWeek(), rule.getRuleType());
                    Integer priority = rule.getPriority() != null
                            ? rule.getPriority()
                            : lk.booknplay.util.CourtPriceResolver.defaultPriority(ruleType);
                    return CourtPricing.builder()
                            .court(court)
                            .dayOfWeek(rule.getDayOfWeek())
                            .startTime(rule.getStartTime())
                            .endTime(rule.getEndTime())
                            .price(rule.getPrice())
                            .ruleType(ruleType)
                            .priority(priority)
                            .label(rule.getLabel())
                            .build();
                })
                .map(courtPricingRepository::save)
                .map(this::mapPricing)
                .toList();
    }

    /**
     * Resolves the sport for a court from either an existing {@code sportId} or a new
     * {@code sportName}. New sports are created case-insensitively and reuse any
     * existing row. Concurrent creations are handled by catching the unique-constraint
     * violation and re-reading the winning row.
     */
    private Sport resolveSport(OwnerCourtRequest request) {
        String sportId = request.getSportId();
        if (sportId != null && !sportId.isBlank()) {
            return sportRepository.findById(sportId.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Sport not found with id: " + sportId));
        }
        String sportName = request.getSportName();
        if (sportName != null && !sportName.isBlank()) {
            String normalized = sportName.trim().replaceAll("\\s+", " ");
            return findOrCreateSport(normalized);
        }
        throw new BadRequestException("sportId or sportName is required");
    }

    private Sport findOrCreateSport(String normalized) {
        return sportRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> {
                    try {
                        return sportRepository.saveAndFlush(
                                Sport.builder().name(normalized).isActive(true).build());
                    } catch (DataIntegrityViolationException race) {
                        // Another request created the same sport first; reuse it.
                        return sportRepository.findByNameIgnoreCase(normalized)
                                .orElseThrow(() -> new BadRequestException(
                                        "Could not resolve sport: " + normalized));
                    }
                });
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
                .facilityType(court.getFacilityType())
                .status(court.getStatus())
                .build();
    }

    private CourtPricingResponse mapPricing(CourtPricing pricing) {
        return CourtPricingResponse.builder()
                .id(pricing.getId())
                .dayOfWeek(pricing.getDayOfWeek())
                .startTime(pricing.getStartTime())
                .endTime(pricing.getEndTime())
                .price(pricing.getPrice())
                .ruleType(lk.booknplay.util.CourtPriceResolver.effectiveType(pricing))
                .priority(lk.booknplay.util.CourtPriceResolver.effectivePriority(pricing))
                .label(pricing.getLabel())
                .build();
    }
}
