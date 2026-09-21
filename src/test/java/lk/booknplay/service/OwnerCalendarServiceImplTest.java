package lk.booknplay.service;

import lk.booknplay.dto.request.WalkInBookingRequest;
import lk.booknplay.dto.response.AvailabilityResponse;
import lk.booknplay.dto.response.OwnerCalendarResponse;
import lk.booknplay.entity.Booking;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.OperatingHours;
import lk.booknplay.entity.Sport;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.enums.CourtStatus;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.exception.ConflictException;
import lk.booknplay.repository.BlockedSlotRepository;
import lk.booknplay.repository.BookingRepository;
import lk.booknplay.repository.CourtPricingRepository;
import lk.booknplay.repository.CourtRepository;
import lk.booknplay.repository.MaintenanceWindowRepository;
import lk.booknplay.repository.OperatingHoursRepository;
import lk.booknplay.repository.PaymentRepository;
import lk.booknplay.service.impl.OwnerCalendarServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerCalendarServiceImplTest {

    private static final String OWNER_EMAIL = "owner@example.com";
    private static final LocalDate DATE = LocalDate.of(2026, 9, 21); // Monday

    @Mock private OwnerAccessService ownerAccessService;
    @Mock private CourtRepository courtRepository;
    @Mock private OperatingHoursRepository operatingHoursRepository;
    @Mock private CourtPricingRepository courtPricingRepository;
    @Mock private BlockedSlotRepository blockedSlotRepository;
    @Mock private MaintenanceWindowRepository maintenanceWindowRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks private OwnerCalendarServiceImpl ownerCalendarService;

    private Venue venue;
    private Court court;
    private WalkInBookingRequest walkInRequest;

    @BeforeEach
    void setUp() {
        venue = Venue.builder().id("v-1").name("PlayArena").build();
        Sport sport = Sport.builder().id("s-1").name("Futsal").build();
        court = court("crt-1", sport);
        walkInRequest = WalkInBookingRequest.builder()
                .courtId("crt-1")
                .date(DATE)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .guestName("Walk In Guest")
                .guestPhone("+94770001111")
                .build();
    }

    @Test
    void normalOperatingHoursGenerateFiniteHourlySlots() {
        stubCalendar(List.of(court), hours(8, 0, 18, 0));

        OwnerCalendarResponse response = ownerCalendarService.getCalendar(OWNER_EMAIL, venue.getId(), DATE, null);

        assertEquals(10, response.getCourts().get(0).getSlots().size());
        assertEquals(LocalTime.of(8, 0), response.getCourts().get(0).getSlots().get(0).getStartTime());
        assertEquals(LocalTime.of(18, 0), response.getCourts().get(0).getSlots().get(9).getEndTime());
    }

    @Test
    void nonHourAlignedClosingTimeDoesNotWrapAndLoopForever() {
        stubCalendar(List.of(court), hours(6, 0, 23, 59));

        OwnerCalendarResponse response = ownerCalendarService.getCalendar(OWNER_EMAIL, venue.getId(), DATE, null);

        assertEquals(17, response.getCourts().get(0).getSlots().size());
        assertEquals(LocalTime.of(23, 0), response.getCourts().get(0).getSlots().get(16).getEndTime());
    }

    @Test
    void overnightOperatingHoursUseNextDayBoundary() {
        stubCalendar(List.of(court), hours(18, 0, 2, 0));

        OwnerCalendarResponse response = ownerCalendarService.getCalendar(OWNER_EMAIL, venue.getId(), DATE, null);

        assertEquals(8, response.getCourts().get(0).getSlots().size());
        assertEquals(LocalTime.MIDNIGHT, response.getCourts().get(0).getSlots().get(5).getEndTime());
        assertEquals(LocalTime.of(2, 0), response.getCourts().get(0).getSlots().get(7).getEndTime());
        verify(bookingRepository).findByCourtIdInAndBookingDateBetweenAndStatusNot(
                List.of("crt-1"), DATE, DATE.plusDays(1), BookingStatus.CANCELLED);
    }

    @Test
    void multipleCourtsAreGeneratedWithOneBulkQueryPerDataType() {
        Court secondCourt = court("crt-2", Sport.builder().id("s-2").name("Badminton").build());
        stubCalendar(List.of(court, secondCourt), hours(9, 0, 12, 0));

        OwnerCalendarResponse response = ownerCalendarService.getCalendar(OWNER_EMAIL, venue.getId(), DATE, null);

        assertEquals(2, response.getCourts().size());
        assertTrue(response.getCourts().stream().allMatch(day -> day.getSlots().size() == 3));
        verify(bookingRepository, times(1)).findByCourtIdInAndBookingDateBetweenAndStatusNot(
                anyList(), any(), any(), any());
        verify(blockedSlotRepository, times(1)).findByCourtIdInAndDateBetween(anyList(), any(), any());
        verify(maintenanceWindowRepository, times(1)).findOverlappingMaintenanceForCourts(anyList(), any(), any());
        verify(courtPricingRepository, times(1)).findByCourtIdInAndDayOfWeek(anyList(), any());
    }

    @Test
    void adjacentBookingBlocksOnlyItsOwnInterval() {
        stubCalendar(List.of(court), hours(17, 0, 19, 0));
        when(bookingRepository.findByCourtIdInAndBookingDateBetweenAndStatusNot(anyList(), any(), any(), any()))
                .thenReturn(List.of(booking(court, DATE, 17, 0, 18, 0)));

        AvailabilityResponse day = ownerCalendarService.getCalendar(OWNER_EMAIL, venue.getId(), DATE, null)
                .getCourts().get(0);

        assertFalse(day.getSlots().get(0).isAvailable());
        assertEquals("BOOKED", day.getSlots().get(0).getReason());
        assertTrue(day.getSlots().get(1).isAvailable());
    }

    @Test
    void fullyBookedDayHasNoAvailableSlots() {
        stubCalendar(List.of(court), hours(17, 0, 19, 0));
        when(bookingRepository.findByCourtIdInAndBookingDateBetweenAndStatusNot(anyList(), any(), any(), any()))
                .thenReturn(List.of(
                        booking(court, DATE, 17, 0, 18, 0),
                        booking(court, DATE, 18, 0, 19, 0)));

        AvailabilityResponse day = ownerCalendarService.getCalendar(OWNER_EMAIL, venue.getId(), DATE, null)
                .getCourts().get(0);

        assertEquals(2, day.getSlots().size());
        assertTrue(day.getSlots().stream().noneMatch(slot -> slot.isAvailable()));
    }

    @Test
    void calendarWithoutBookingsReturnsAvailableSlots() {
        stubCalendar(List.of(court), hours(10, 0, 12, 0));

        AvailabilityResponse day = ownerCalendarService.getCalendar(OWNER_EMAIL, venue.getId(), DATE, null)
                .getCourts().get(0);

        assertEquals(2, day.getSlots().size());
        assertTrue(day.getSlots().stream().allMatch(slot -> slot.isAvailable()));
    }

    @Test
    void invalidSlotDurationIsRejectedBeforeGeneratingSlots() {
        ReflectionTestUtils.setField(ownerCalendarService, "slotDurationMinutes", 0);

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> ownerCalendarService.getCalendar(OWNER_EMAIL, venue.getId(), DATE, null));

        assertTrue(error.getMessage().contains("slot duration"));
    }

    @Test
    void nullDateIsRejected() {
        assertThrows(BadRequestException.class,
                () -> ownerCalendarService.getCalendar(OWNER_EMAIL, venue.getId(), null, null));
    }

    @Test
    void maximumDateIsRejectedBecauseAnOvernightBoundaryCannotAdvance() {
        assertThrows(BadRequestException.class,
                () -> ownerCalendarService.getCalendar(OWNER_EMAIL, venue.getId(), LocalDate.MAX, null));
    }

    @Test
    void excessiveCourtRangeIsRejectedAndCanBeFiltered() {
        List<Court> courts = IntStream.range(0, 201)
                .mapToObj(index -> court("crt-" + index, Sport.builder().id("s-" + index).name("Sport").build()))
                .toList();
        when(ownerAccessService.requireVenue(OWNER_EMAIL, venue.getId())).thenReturn(venue);
        when(courtRepository.findCalendarCourts(eq(venue.getId()), eq(CourtStatus.DELETED), any(Pageable.class)))
                .thenReturn(courts);

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> ownerCalendarService.getCalendar(OWNER_EMAIL, venue.getId(), DATE, null));

        assertTrue(error.getMessage().contains("maximum of 200 courts"));
    }

    @Test
    void repeatedCalendarRequestsRemainBoundedAndIndependent() {
        stubCalendar(List.of(court), hours(6, 0, 23, 59));

        for (int request = 0; request < 100; request++) {
            OwnerCalendarResponse response = ownerCalendarService.getCalendar(OWNER_EMAIL, venue.getId(), DATE, null);
            assertEquals(17, response.getCourts().get(0).getSlots().size());
        }
    }

    @Test
    void createWalkInOverlappingSlotThrowsConflictException() {
        when(courtRepository.findByIdWithLock("crt-1")).thenReturn(Optional.of(court));
        when(ownerAccessService.requireVenue(OWNER_EMAIL, "v-1")).thenReturn(venue);
        when(bookingRepository.existsOverlappingBooking(any(), any(), any(), any())).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> ownerCalendarService.createWalkIn(OWNER_EMAIL, walkInRequest));
    }

    private void stubCalendar(List<Court> courts, OperatingHours operatingHours) {
        when(ownerAccessService.requireVenue(OWNER_EMAIL, venue.getId())).thenReturn(venue);
        when(courtRepository.findCalendarCourts(eq(venue.getId()), eq(CourtStatus.DELETED), any(Pageable.class)))
                .thenReturn(courts);
        when(operatingHoursRepository.findByVenueId(venue.getId())).thenReturn(List.of(operatingHours));
        when(courtPricingRepository.findByCourtIdInAndDayOfWeek(anyList(), any())).thenReturn(List.of());
        when(blockedSlotRepository.findByCourtIdInAndDateBetween(anyList(), any(), any())).thenReturn(List.of());
        when(maintenanceWindowRepository.findOverlappingMaintenanceForCourts(anyList(), any(), any())).thenReturn(List.of());
        when(bookingRepository.findByCourtIdInAndBookingDateBetweenAndStatusNot(anyList(), any(), any(), any()))
                .thenReturn(List.of());
    }

    private OperatingHours hours(int openHour, int openMinute, int closeHour, int closeMinute) {
        return OperatingHours.builder()
                .venue(venue)
                .dayOfWeek(DATE.getDayOfWeek())
                .openTime(LocalTime.of(openHour, openMinute))
                .closeTime(LocalTime.of(closeHour, closeMinute))
                .isClosed(false)
                .build();
    }

    private Court court(String id, Sport sport) {
        return Court.builder()
                .id(id)
                .venue(venue)
                .sport(sport)
                .name("Court " + id)
                .hourlyRate(BigDecimal.valueOf(3000))
                .status(CourtStatus.ACTIVE)
                .build();
    }

    private Booking booking(Court bookedCourt, LocalDate date, int startHour, int startMinute,
                            int endHour, int endMinute) {
        return Booking.builder()
                .court(bookedCourt)
                .bookingDate(date)
                .startTime(LocalTime.of(startHour, startMinute))
                .endTime(LocalTime.of(endHour, endMinute))
                .status(BookingStatus.CONFIRMED)
                .build();
    }
}
