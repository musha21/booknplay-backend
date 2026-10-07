package lk.booknplay.service;

import lk.booknplay.dto.request.OwnerVenueRequest;
import lk.booknplay.dto.response.VenueResponse;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.OperatingHours;
import lk.booknplay.entity.Sport;
import lk.booknplay.entity.Venue;
import lk.booknplay.entity.VenueImage;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.exception.ConflictException;
import lk.booknplay.repository.BookingRepository;
import lk.booknplay.repository.CancellationPolicyRepository;
import lk.booknplay.repository.CourtRepository;
import lk.booknplay.repository.OperatingHoursRepository;
import lk.booknplay.repository.SportRepository;
import lk.booknplay.repository.VenueImageRepository;
import lk.booknplay.repository.VenueRepository;
import lk.booknplay.service.impl.OwnerVenueServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerVenueServiceImplTest {

    @Mock private OwnerAccessService ownerAccessService;
    @Mock private VenueRepository venueRepository;
    @Mock private CourtRepository courtRepository;
    @Mock private VenueImageRepository venueImageRepository;
    @Mock private OperatingHoursRepository operatingHoursRepository;
    @Mock private CancellationPolicyRepository cancellationPolicyRepository;
    @Mock private SportRepository sportRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks
    private OwnerVenueServiceImpl ownerVenueService;

    private Business business;
    private Venue venue;

    @BeforeEach
    void setUp() {
        business = Business.builder()
                .id("biz-1")
                .name("Arena Co")
                .logoUrl("https://cdn.example/logo.png")
                .ownerId("u-owner")
                .build();
        venue = Venue.builder()
                .id("venue-1")
                .business(business)
                .name("Court A")
                .address("1 Main St")
                .city("Kandy")
                .latitude(7.29)
                .longitude(80.63)
                .status(VenueStatus.DRAFT)
                .amenities(new ArrayList<>())
                .rules(new ArrayList<>())
                .build();
    }

    @Test
    void submitVenue_RequiresPhotoAndSpaces() {
        when(ownerAccessService.requireOwner("owner@example.com")).thenReturn(business);
        when(ownerAccessService.requireVenue("owner@example.com", "venue-1")).thenReturn(venue);
        when(courtRepository.findByVenueId("venue-1")).thenReturn(List.of());

        assertThrows(BadRequestException.class,
                () -> ownerVenueService.submitVenue("owner@example.com", "venue-1"));
    }

    @Test
    void submitVenue_MovesDraftToPendingApproval() {
        when(ownerAccessService.requireOwner("owner@example.com")).thenReturn(business);
        when(ownerAccessService.requireVenue("owner@example.com", "venue-1")).thenReturn(venue);
        List<Court> courts = List.of(
                Court.builder().id("c1").venue(venue).name("Court 1")
                        .sport(Sport.builder().id("s1").name("Futsal").build())
                        .hourlyRate(new BigDecimal("2000")).durationMinutes(60).build());
        List<OperatingHours> hours = List.of(
                OperatingHours.builder().id("h1").venue(venue).dayOfWeek(DayOfWeek.MONDAY).build());
        when(courtRepository.findByVenueId("venue-1")).thenReturn(courts);
        when(operatingHoursRepository.findByVenueId("venue-1")).thenReturn(hours);
        when(venueImageRepository.countByVenueId("venue-1")).thenReturn(1L);
        when(venueImageRepository.findByVenueIdOrderBySortOrderAsc("venue-1")).thenReturn(List.of(
                VenueImage.builder().id("m1").venue(venue).url("https://cdn.example/v1.jpg").sortOrder(0).build()));
        when(venueRepository.save(venue)).thenReturn(venue);

        VenueResponse response = ownerVenueService.submitVenue("owner@example.com", "venue-1");

        assertEquals(VenueStatus.APPROVED, venue.getStatus());
        assertEquals(VenueStatus.APPROVED, response.getStatus());
        assertTrue(response.getImages().contains("https://cdn.example/v1.jpg"));
    }

    @Test
    void uploadMedia_RejectsMoreThanSixPhotos() {
        when(ownerAccessService.requireOwner("owner@example.com")).thenReturn(business);
        when(ownerAccessService.requireVenue("owner@example.com", "venue-1")).thenReturn(venue);
        when(venueImageRepository.countByVenueId("venue-1")).thenReturn(6L);

        MockMultipartFile image = new MockMultipartFile("images", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});
        assertThrows(BadRequestException.class,
                () -> ownerVenueService.uploadMedia("owner@example.com", "venue-1", List.of(image)));
        verify(fileStorageService, never()).store(any(), any());
    }

    @Test
    void uploadMedia_SetsCoverFromFirstOrderedPhoto() {
        when(ownerAccessService.requireOwner("owner@example.com")).thenReturn(business);
        when(ownerAccessService.requireVenue("owner@example.com", "venue-1")).thenReturn(venue);
        when(venueImageRepository.countByVenueId("venue-1")).thenReturn(0L);
        when(fileStorageService.store(any(), eq("venue/venue-1"))).thenReturn("https://cdn.example/v1.jpg");
        when(venueImageRepository.save(any(VenueImage.class))).thenAnswer(invocation -> {
            VenueImage image = invocation.getArgument(0);
            image.setId("m1");
            return image;
        });
        when(venueImageRepository.findByVenueIdOrderBySortOrderAsc("venue-1"))
                .thenReturn(List.of(VenueImage.builder().id("m1").venue(venue).url("https://cdn.example/v1.jpg").sortOrder(0).build()));
        when(venueRepository.save(venue)).thenReturn(venue);
        when(courtRepository.findByVenueId("venue-1")).thenReturn(List.of());
        when(operatingHoursRepository.findByVenueId("venue-1")).thenReturn(List.of());

        VenueResponse response = ownerVenueService.uploadMedia(
                "owner@example.com",
                "venue-1",
                List.of(new MockMultipartFile("images", "a.jpg", "image/jpeg", new byte[]{1})));

        assertEquals("https://cdn.example/v1.jpg", venue.getCoverImageUrl());
        assertEquals("https://cdn.example/v1.jpg", response.getCoverImageUrl());
        assertEquals("https://cdn.example/logo.png", response.getBusinessLogoUrl());
    }

    @Test
    void archiveVenue_BlocksWhenFutureBookingsExist() {
        when(ownerAccessService.requireOwner("owner@example.com")).thenReturn(business);
        when(ownerAccessService.requireVenue("owner@example.com", "venue-1")).thenReturn(venue);
        when(bookingRepository.existsFutureBooking(
                eq("venue-1"),
                eq(List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)),
                any(LocalDate.class),
                any(LocalTime.class))).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> ownerVenueService.archiveVenue("owner@example.com", "venue-1"));
        assertEquals("VENUE_HAS_FUTURE_BOOKINGS", ex.getCode());
        verify(venueRepository, never()).save(any());
    }

    @Test
    void archiveAndRestore_PreservesPreviousStatus() {
        venue.setStatus(VenueStatus.APPROVED);
        when(ownerAccessService.requireOwner("owner@example.com")).thenReturn(business);
        when(ownerAccessService.requireVenue("owner@example.com", "venue-1")).thenReturn(venue);
        when(bookingRepository.existsFutureBooking(any(), anyList(), any(), any())).thenReturn(false);
        when(venueRepository.save(venue)).thenReturn(venue);
        stubMapVenue();

        ownerVenueService.archiveVenue("owner@example.com", "venue-1");
        assertEquals(VenueStatus.DELETED, venue.getStatus());
        assertEquals(VenueStatus.APPROVED, venue.getArchivedFromStatus());

        ownerVenueService.restoreVenue("owner@example.com", "venue-1");
        assertEquals(VenueStatus.APPROVED, venue.getStatus());
        assertNull(venue.getArchivedFromStatus());
    }

    @Test
    void listVenues_ArchivedFlagFiltersDeletedStatus() {
        when(ownerAccessService.requireBusiness("owner@example.com")).thenReturn(business);
        when(venueRepository.findByBusinessIdAndStatus("biz-1", VenueStatus.DELETED))
                .thenReturn(List.of(venue));
        venue.setStatus(VenueStatus.DELETED);
        stubMapVenue();

        List<VenueResponse> archived = ownerVenueService.listVenues("owner@example.com", true);
        assertEquals(1, archived.size());
        verify(venueRepository).findByBusinessIdAndStatus("biz-1", VenueStatus.DELETED);
        verify(venueRepository, never()).findByBusinessIdAndStatusNot(any(), any());
    }

    @Test
    void updateVenue_DoesNotClearCoverWhenOmitted() {
        venue.setCoverImageUrl("https://cdn.example/keep.jpg");
        when(ownerAccessService.requireOwner("owner@example.com")).thenReturn(business);
        when(ownerAccessService.requireVenue("owner@example.com", "venue-1")).thenReturn(venue);
        when(venueRepository.save(venue)).thenReturn(venue);
        stubMapVenue();

        OwnerVenueRequest request = OwnerVenueRequest.builder()
                .name("Court A")
                .address("1 Main St")
                .city("Kandy")
                .build();
        ownerVenueService.updateVenue("owner@example.com", "venue-1", request);

        ArgumentCaptor<Venue> captor = ArgumentCaptor.forClass(Venue.class);
        verify(venueRepository).save(captor.capture());
        assertEquals("https://cdn.example/keep.jpg", captor.getValue().getCoverImageUrl());
    }

    private void stubMapVenue() {
        when(venueImageRepository.findByVenueIdOrderBySortOrderAsc("venue-1")).thenReturn(List.of());
        when(courtRepository.findByVenueId("venue-1")).thenReturn(List.of());
        when(operatingHoursRepository.findByVenueId("venue-1")).thenReturn(List.of());
    }
}
