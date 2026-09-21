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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerCourtServiceImpl implements OwnerCourtService {

    private final OwnerAccessService ownerAccessService;
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
        Venue venue = ownerAccessService.requireVenue(ownerEmail, venueId);
        Sport sport = sportRepository.findById(request.getSportId())
                .orElseThrow(() -> new ResourceNotFoundException("Sport not found with id: " + request.getSportId()));
        Court court = Court.builder()
                .venue(venue)
                .sport(sport)
                .name(request.getName())
                .hourlyRate(request.getHourlyRate())
                .status(CourtStatus.ACTIVE)
                .build();
        return mapCourt(courtRepository.save(court));
    }

    @Override
    @Transactional
    public CourtResponse updateCourt(String ownerEmail, String courtId, OwnerCourtRequest request) {
        Court court = ownerAccessService.requireCourt(ownerEmail, courtId);
        Sport sport = sportRepository.findById(request.getSportId())
                .orElseThrow(() -> new ResourceNotFoundException("Sport not found with id: " + request.getSportId()));
        court.setName(request.getName());
        court.setSport(sport);
        court.setHourlyRate(request.getHourlyRate());
        return mapCourt(courtRepository.save(court));
    }

    @Override
    @Transactional
    public void deleteCourt(String ownerEmail, String courtId) {
        Court court = ownerAccessService.requireCourt(ownerEmail, courtId);
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
        Court court = ownerAccessService.requireCourt(ownerEmail, courtId);
        for (CourtPricingUpdateRequest.PricingRule rule : request.getRules()) {
            if (!rule.getStartTime().isBefore(rule.getEndTime())) {
                throw new BadRequestException("Pricing start time must be before end time");
            }
        }
        courtPricingRepository.deleteByCourtId(courtId);
        courtPricingRepository.flush();
        return request.getRules().stream()
                .map(rule -> CourtPricing.builder()
                        .court(court)
                        .dayOfWeek(rule.getDayOfWeek())
                        .startTime(rule.getStartTime())
                        .endTime(rule.getEndTime())
                        .price(rule.getPrice())
                        .build())
                .map(courtPricingRepository::save)
                .map(this::mapPricing)
                .toList();
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
                .build();
    }
}
