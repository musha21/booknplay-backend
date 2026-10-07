package lk.booknplay.service.impl;

import lk.booknplay.dto.response.AvailabilityResponse;
import lk.booknplay.entity.Booking;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.OperatingHours;
import lk.booknplay.entity.Sport;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.enums.CourtStatus;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.repository.BlockedSlotRepository;
import lk.booknplay.repository.BookingRepository;
import lk.booknplay.repository.CourtPricingRepository;
import lk.booknplay.repository.CourtRepository;
import lk.booknplay.repository.MaintenanceWindowRepository;
import lk.booknplay.repository.OperatingHoursRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceImplHoldTest {

    @Mock private CourtRepository courtRepository;
    @Mock private OperatingHoursRepository operatingHoursRepository;
    @Mock private CourtPricingRepository courtPricingRepository;
    @Mock private BlockedSlotRepository blockedSlotRepository;
    @Mock private MaintenanceWindowRepository maintenanceWindowRepository;
    @Mock private BookingRepository bookingRepository;

    @InjectMocks private AvailabilityServiceImpl availabilityService;

    private LocalDate futureDate;
    private Court court;

    @BeforeEach
    void setUp() {
        futureDate = LocalDate.now().plusDays(3);
        Venue venue = Venue.builder().id("v-1").name("Arena").status(VenueStatus.ACTIVE).build();
        Sport sport = Sport.builder().id("s-1").name("Futsal").build();
        court = Court.builder()
                .id("c-1")
                .name("Court 1")
                .venue(venue)
                .sport(sport)
                .hourlyRate(new BigDecimal("2000"))
                .status(CourtStatus.ACTIVE)
                .build();

        when(courtRepository.findById("c-1")).thenReturn(Optional.of(court));
        when(operatingHoursRepository.findByVenueIdAndDayOfWeek(eq("v-1"), any()))
                .thenReturn(Optional.of(OperatingHours.builder()
                        .venue(venue)
                        .dayOfWeek(futureDate.getDayOfWeek())
                        .openTime(LocalTime.of(8, 0))
                        .closeTime(LocalTime.of(12, 0))
                        .isClosed(false)
                        .build()));
        when(courtPricingRepository.findByCourtIdAndDayOfWeek("c-1", futureDate.getDayOfWeek()))
                .thenReturn(List.of());
        when(blockedSlotRepository.findByCourtIdInAndDateBetween(anyList(), any(), any()))
                .thenReturn(List.of());
        when(maintenanceWindowRepository.findOverlappingMaintenance(any(), any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void pendingBookingMarksSlotHeld() {
        Booking pending = Booking.builder()
                .id("b-pending")
                .court(court)
                .bookingDate(futureDate)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .status(BookingStatus.PENDING)
                .build();
        when(bookingRepository.findWithSlotsByCourtIdInAndBookingDateBetweenAndStatusIn(
                anyList(), any(), any(), anyList()))
                .thenReturn(List.of(pending));

        AvailabilityResponse response = availabilityService.getAvailability("c-1", futureDate);
        var held = response.getSlots().stream()
                .filter(slot -> slot.getStartTime().equals(LocalTime.of(9, 0)))
                .findFirst()
                .orElseThrow();

        assertFalse(held.isAvailable());
        assertEquals("HELD", held.getReason());
        assertTrue(response.getSlots().stream()
                .filter(slot -> slot.getStartTime().equals(LocalTime.of(10, 0)))
                .findFirst()
                .orElseThrow()
                .isAvailable());
    }

    @Test
    void confirmedBookingMarksSlotBooked() {
        Booking confirmed = Booking.builder()
                .id("b-confirmed")
                .court(court)
                .bookingDate(futureDate)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .status(BookingStatus.CONFIRMED)
                .build();
        when(bookingRepository.findWithSlotsByCourtIdInAndBookingDateBetweenAndStatusIn(
                anyList(), any(), any(), anyList()))
                .thenReturn(List.of(confirmed));

        AvailabilityResponse response = availabilityService.getAvailability("c-1", futureDate);
        var booked = response.getSlots().stream()
                .filter(slot -> slot.getStartTime().equals(LocalTime.of(9, 0)))
                .findFirst()
                .orElseThrow();

        assertFalse(booked.isAvailable());
        assertEquals("BOOKED", booked.getReason());
    }
}
