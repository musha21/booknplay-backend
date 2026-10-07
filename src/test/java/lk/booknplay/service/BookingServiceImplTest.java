package lk.booknplay.service;

import lk.booknplay.dto.request.SlotSelectionRequest;
import lk.booknplay.entity.*;
import lk.booknplay.enums.CourtStatus;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.exception.ConflictException;
import lk.booknplay.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {
    @Mock OperatingHoursRepository operatingHoursRepository;
    @Mock CourtPricingRepository courtPricingRepository;
    @Mock BlockedSlotRepository blockedSlotRepository;
    @Mock MaintenanceWindowRepository maintenanceWindowRepository;
    @Mock BookingRepository bookingRepository;
    @InjectMocks BookingRulesService rules;

    private Court court;
    private Sport sport;

    @BeforeEach
    void setUp() {
        Business business = Business.builder().id("business-1").name("Arena").build();
        Venue venue = Venue.builder().id("venue-1").business(business).name("PlayArena")
                .status(VenueStatus.ACTIVE).build();
        sport = Sport.builder().id("sport-1").name("Badminton").build();
        court = Court.builder().id("court-1").venue(venue).sport(sport).name("Court 1")
                .hourlyRate(new BigDecimal("2500")).status(CourtStatus.ACTIVE).build();
    }

    @Test
    void confirmedOverlapIsRejectedBySharedRules() {
        when(operatingHoursRepository.findByVenueIdAndDayOfWeek(any(), any())).thenReturn(java.util.Optional.empty());
        when(blockedSlotRepository.findByCourtIdAndDate(any(), any())).thenReturn(List.of());
        when(maintenanceWindowRepository.findOverlappingMaintenance(any(), any(), any())).thenReturn(List.of());
        when(bookingRepository.existsOverlappingBooking(any(), any(), any(), any())).thenReturn(true);

        ConflictException error = assertThrows(ConflictException.class, () -> rules.validateAndPrice(
                court, sport, LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0)));
        assertEquals("COURT_ALREADY_BOOKED", error.getCode());
    }

    @Test
    void gappedSlotsPriceOnlySelectedHours() {
        when(operatingHoursRepository.findByVenueIdAndDayOfWeek(any(), any())).thenReturn(java.util.Optional.empty());
        when(blockedSlotRepository.findByCourtIdAndDate(any(), any())).thenReturn(List.of());
        when(maintenanceWindowRepository.findOverlappingMaintenance(any(), any(), any())).thenReturn(List.of());
        when(courtPricingRepository.findByCourtIdAndDayOfWeek(any(), any())).thenReturn(List.of());
        when(bookingRepository.existsOverlappingBooking(any(), any(), any(), any())).thenReturn(false);

        LocalDate date = LocalDate.now().plusDays(1);
        BookingRulesService.Result result = rules.validateAndPrice(court, sport, date, List.of(
                SlotSelectionRequest.builder().startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0)).build(),
                SlotSelectionRequest.builder().startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(15, 0)).build()
        ));

        assertEquals(2, result.slots().size());
        assertEquals(new BigDecimal("5000"), result.totalAmount());
        assertEquals(LocalTime.of(10, 0), result.slots().get(0).startTime());
        assertEquals(LocalTime.of(14, 0), result.slots().get(1).startTime());
        // Gap hour 11-12 / 12-13 / 13-14 must never be overlap-checked as part of a filled envelope
        verify(bookingRepository).existsOverlappingBooking(eq("court-1"), eq(date), eq(LocalTime.of(10, 0)), eq(LocalTime.of(11, 0)));
        verify(bookingRepository).existsOverlappingBooking(eq("court-1"), eq(date), eq(LocalTime.of(14, 0)), eq(LocalTime.of(15, 0)));
        verify(bookingRepository, never()).existsOverlappingBooking(eq("court-1"), eq(date), eq(LocalTime.of(11, 0)), eq(LocalTime.of(12, 0)));
        verify(bookingRepository, never()).existsOverlappingBooking(eq("court-1"), eq(date), eq(LocalTime.of(10, 0)), eq(LocalTime.of(15, 0)));
    }

    @Test
    void continuousLegacyRangeStillExpandsEveryHour() {
        when(operatingHoursRepository.findByVenueIdAndDayOfWeek(any(), any())).thenReturn(java.util.Optional.empty());
        when(blockedSlotRepository.findByCourtIdAndDate(any(), any())).thenReturn(List.of());
        when(maintenanceWindowRepository.findOverlappingMaintenance(any(), any(), any())).thenReturn(List.of());
        when(courtPricingRepository.findByCourtIdAndDayOfWeek(any(), any())).thenReturn(List.of());
        when(bookingRepository.existsOverlappingBooking(any(), any(), any(), any())).thenReturn(false);

        BookingRulesService.Result result = rules.validateAndPrice(
                court, sport, LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0));

        assertEquals(2, result.slots().size());
        assertEquals(new BigDecimal("5000"), result.totalAmount());
    }
}
