package lk.booknplay.service.impl;

import lk.booknplay.dto.request.OwnerCourtRequest;
import lk.booknplay.dto.response.CourtResponse;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.Sport;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.CourtStatus;
import lk.booknplay.enums.FacilityType;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.repository.CourtPricingRepository;
import lk.booknplay.repository.CourtRepository;
import lk.booknplay.repository.SportRepository;
import lk.booknplay.service.OwnerAccessService;
import lk.booknplay.service.PlanEntitlementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerCourtServiceImplTest {

    @Mock private OwnerAccessService ownerAccessService;
    @Mock private PlanEntitlementService planEntitlementService;
    @Mock private CourtRepository courtRepository;
    @Mock private SportRepository sportRepository;
    @Mock private CourtPricingRepository courtPricingRepository;

    @InjectMocks
    private OwnerCourtServiceImpl service;

    private Venue venue() {
        return Venue.builder()
                .id("venue-1")
                .name("Kandy Sports Hub")
                .business(Business.builder().id("biz-1").build())
                .build();
    }

    @Test
    void createCourtWithExistingSportPersistsFacilityType() {
        when(ownerAccessService.requireMutableVenue("o@x.com", "venue-1")).thenReturn(venue());
        Sport cricket = Sport.builder().id("sport-1").name("Cricket").isActive(true).build();
        when(sportRepository.findById("sport-1")).thenReturn(Optional.of(cricket));
        when(courtRepository.save(any(Court.class))).thenAnswer(inv -> inv.getArgument(0));

        OwnerCourtRequest request = OwnerCourtRequest.builder()
                .name("Indoor Court")
                .sportId("sport-1")
                .facilityType(FacilityType.INDOOR)
                .hourlyRate(new BigDecimal("2500"))
                .build();

        CourtResponse response = service.createCourt("o@x.com", "venue-1", request);

        ArgumentCaptor<Court> captor = ArgumentCaptor.forClass(Court.class);
        verify(courtRepository).save(captor.capture());
        assertEquals(FacilityType.INDOOR, captor.getValue().getFacilityType());
        assertEquals(CourtStatus.ACTIVE, captor.getValue().getStatus());
        assertEquals(FacilityType.INDOOR, response.getFacilityType());
        assertEquals("sport-1", response.getSportId());
    }

    @Test
    void createCourtWithNewSportNameCreatesSportOnce() {
        when(ownerAccessService.requireMutableVenue("o@x.com", "venue-1")).thenReturn(venue());
        when(sportRepository.findByNameIgnoreCase("Carrom")).thenReturn(Optional.empty());
        Sport created = Sport.builder().id("sport-2").name("Carrom").isActive(true).build();
        when(sportRepository.saveAndFlush(any(Sport.class))).thenReturn(created);
        when(courtRepository.save(any(Court.class))).thenAnswer(inv -> inv.getArgument(0));

        OwnerCourtRequest request = OwnerCourtRequest.builder()
                .name("Carrom Board")
                .sportName("Carrom")
                .facilityType(FacilityType.OUTDOOR)
                .hourlyRate(new BigDecimal("1500"))
                .build();

        CourtResponse response = service.createCourt("o@x.com", "venue-1", request);

        verify(sportRepository).saveAndFlush(any(Sport.class));
        assertEquals("Carrom", response.getSportName());
        assertEquals(FacilityType.OUTDOOR, response.getFacilityType());
    }

    @Test
    void createCourtReusesExistingSportCaseInsensitively() {
        when(ownerAccessService.requireMutableVenue("o@x.com", "venue-1")).thenReturn(venue());
        Sport existing = Sport.builder().id("sport-3").name("Carrom").isActive(true).build();
        when(sportRepository.findByNameIgnoreCase("carrom")).thenReturn(Optional.of(existing));
        when(courtRepository.save(any(Court.class))).thenAnswer(inv -> inv.getArgument(0));

        OwnerCourtRequest request = OwnerCourtRequest.builder()
                .name("Board")
                .sportName("carrom")
                .facilityType(FacilityType.INDOOR)
                .hourlyRate(new BigDecimal("1000"))
                .build();

        service.createCourt("o@x.com", "venue-1", request);

        verify(sportRepository, never()).saveAndFlush(any(Sport.class));
        verify(sportRepository, never()).save(any(Sport.class));
    }

    @Test
    void createCourtRejectsWhenNoSportProvided() {
        when(ownerAccessService.requireMutableVenue("o@x.com", "venue-1")).thenReturn(venue());
        OwnerCourtRequest request = OwnerCourtRequest.builder()
                .name("No Sport")
                .facilityType(FacilityType.INDOOR)
                .hourlyRate(new BigDecimal("1000"))
                .build();

        assertThrows(BadRequestException.class, () -> service.createCourt("o@x.com", "venue-1", request));
        verify(courtRepository, never()).save(any(Court.class));
    }
}
