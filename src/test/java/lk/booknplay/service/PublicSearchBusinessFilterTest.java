package lk.booknplay.service;

import lk.booknplay.entity.Business;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.Sport;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.CourtStatus;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.repository.BusinessRepository;
import lk.booknplay.repository.CourtRepository;
import lk.booknplay.repository.OperatingHoursRepository;
import lk.booknplay.repository.PromotionRepository;
import lk.booknplay.repository.ReviewRepository;
import lk.booknplay.repository.SportRepository;
import lk.booknplay.repository.VenueRepository;
import lk.booknplay.service.impl.PublicSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicSearchBusinessFilterTest {

    @Mock private VenueRepository venueRepository;
    @Mock private CourtRepository courtRepository;
    @Mock private SportRepository sportRepository;
    @Mock private BusinessRepository businessRepository;
    @Mock private OperatingHoursRepository operatingHoursRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private PromotionRepository promotionRepository;
    @Mock private AvailabilityService availabilityService;

    @InjectMocks
    private PublicSearchServiceImpl publicSearchService;

    private Venue venueA;

    @BeforeEach
    void setUp() {
        Business businessA = Business.builder().id("biz-a").name("Arena A").build();
        Sport sport = Sport.builder().id("sport-1").name("Futsal").build();

        venueA = Venue.builder()
                .id("venue-a")
                .business(businessA)
                .name("Court A")
                .city("Colombo")
                .status(VenueStatus.APPROVED)
                .build();

        Court courtA = Court.builder()
                .id("court-a")
                .venue(venueA)
                .sport(sport)
                .status(CourtStatus.ACTIVE)
                .hourlyRate(new BigDecimal("2000"))
                .build();

        when(courtRepository.findByVenueId("venue-a")).thenReturn(List.of(courtA));
    }

    @Test
    void searchVenues_filtersByBusinessIdAndSkipsCityLock() {
        when(venueRepository.searchVenues(
                eq(List.of(VenueStatus.ACTIVE, VenueStatus.APPROVED)),
                eq("biz-a"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(venueA)));
        when(operatingHoursRepository.findByVenueId("venue-a")).thenReturn(List.of());
        when(reviewRepository.averageRatingByVenueId("venue-a")).thenReturn(null);
        when(reviewRepository.countByVenueId("venue-a")).thenReturn(0L);

        var page = publicSearchService.searchVenues(
                "biz-a",
                "Kandy",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertEquals(1, page.getTotalElements());
        assertEquals("venue-a", page.getContent().get(0).getId());
        assertEquals("biz-a", page.getContent().get(0).getBusinessId());
        verify(venueRepository).searchVenues(
                eq(List.of(VenueStatus.ACTIVE, VenueStatus.APPROVED)),
                eq("biz-a"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        );
    }
}
