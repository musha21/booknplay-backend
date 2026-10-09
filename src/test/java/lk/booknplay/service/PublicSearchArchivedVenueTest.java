package lk.booknplay.service;

import lk.booknplay.entity.Business;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.repository.BusinessRepository;
import lk.booknplay.repository.CourtRepository;
import lk.booknplay.repository.VenueRepository;
import lk.booknplay.service.impl.PublicSearchServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicSearchArchivedVenueTest {

    @Mock private VenueRepository venueRepository;
    @Mock private BusinessRepository businessRepository;
    @Mock private CourtRepository courtRepository;
    @Mock private lk.booknplay.repository.SportRepository sportRepository;
    @Mock private lk.booknplay.repository.OperatingHoursRepository operatingHoursRepository;
    @Mock private lk.booknplay.repository.ReviewRepository reviewRepository;
    @Mock private lk.booknplay.repository.PromotionRepository promotionRepository;
    @Mock private AvailabilityService availabilityService;

    @InjectMocks
    private PublicSearchServiceImpl publicSearchService;

    @Test
    void getVenue_ExcludesArchivedVenues() {
        Venue archived = Venue.builder()
                .id("venue-1")
                .name("Hidden")
                .status(VenueStatus.DELETED)
                .business(Business.builder().id("biz-1").name("Arena").build())
                .build();
        when(venueRepository.findById("venue-1")).thenReturn(Optional.of(archived));

        assertThrows(ResourceNotFoundException.class,
                () -> publicSearchService.getVenueDetails("venue-1"));
    }
}
