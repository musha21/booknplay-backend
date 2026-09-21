package lk.booknplay.service.impl;

import lk.booknplay.dto.response.AvailabilityResponse;
import lk.booknplay.dto.response.AvailabilitySlotResponse;
import lk.booknplay.entity.*;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.enums.CourtStatus;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.repository.*;
import lk.booknplay.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

    private static final Duration SLOT_DURATION = Duration.ofHours(1);
    private static final int MAX_SLOTS_PER_DAY = 24;

    private final CourtRepository courtRepository;
    private final OperatingHoursRepository operatingHoursRepository;
    private final CourtPricingRepository courtPricingRepository;
    private final BlockedSlotRepository blockedSlotRepository;
    private final MaintenanceWindowRepository maintenanceWindowRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(String courtId, LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException("Availability date cannot be in the past");
        }

        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Court not found with id: " + courtId));

        if (court.getStatus() != CourtStatus.ACTIVE) {
            return AvailabilityResponse.builder()
                    .date(date)
                    .courtId(court.getId())
                    .courtName(court.getName())
                    .venueId(court.getVenue().getId())
                    .venueName(court.getVenue().getName())
                    .sportName(court.getSport().getName())
                    .slots(List.of())
                    .build();
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        Optional<OperatingHours> opHoursOpt = operatingHoursRepository.findByVenueIdAndDayOfWeek(court.getVenue().getId(), dayOfWeek);

        LocalTime openTime = opHoursOpt.map(OperatingHours::getOpenTime).orElse(LocalTime.of(6, 0));
        LocalTime closeTime = opHoursOpt.map(OperatingHours::getCloseTime).orElse(LocalTime.of(22, 0));
        boolean isClosed = opHoursOpt.map(OperatingHours::isClosed).orElse(false);

        List<AvailabilitySlotResponse> slots = new ArrayList<>();

        if (!isClosed) {
            List<CourtPricing> pricings = courtPricingRepository.findByCourtIdAndDayOfWeek(courtId, dayOfWeek);
            LocalDateTime periodStart = LocalDateTime.of(date, openTime);
            LocalDateTime periodEnd = LocalDateTime.of(date, closeTime);
            if (!closeTime.isAfter(openTime)) {
                periodEnd = periodEnd.plusDays(1);
            }
            List<BlockedSlot> blockedSlots = blockedSlotRepository.findByCourtIdInAndDateBetween(
                    List.of(courtId), date, periodEnd.toLocalDate());
            List<MaintenanceWindow> maintenanceWindows = maintenanceWindowRepository.findOverlappingMaintenance(
                    courtId, periodStart, periodEnd
            );
            List<Booking> activeBookings = bookingRepository.findByCourtIdInAndBookingDateBetweenAndStatusNot(
                    List.of(courtId), date, periodEnd.toLocalDate(), BookingStatus.CANCELLED
            );

            LocalDateTime currentSlotStart = periodStart;
            int generatedSlots = 0;
            while (!currentSlotStart.plus(SLOT_DURATION).isAfter(periodEnd)) {
                if (++generatedSlots > MAX_SLOTS_PER_DAY) {
                    throw new BadRequestException("Availability slot count exceeds the supported maximum");
                }
                LocalDateTime currentSlotEnd = currentSlotStart.plus(SLOT_DURATION);

                boolean isAvailable = true;
                String unavailableReason = null;

                // Check blocked slots
                for (BlockedSlot bs : blockedSlots) {
                    LocalDateTime blockedStart = LocalDateTime.of(bs.getDate(), bs.getStartTime());
                    LocalDateTime blockedEnd = endDateTime(bs.getDate(), bs.getStartTime(), bs.getEndTime());
                    if (overlaps(currentSlotStart, currentSlotEnd, blockedStart, blockedEnd)) {
                        isAvailable = false;
                        unavailableReason = "BLOCKED";
                        break;
                    }
                }

                // Check maintenance
                if (isAvailable) {
                    for (MaintenanceWindow mw : maintenanceWindows) {
                        if (overlaps(currentSlotStart, currentSlotEnd, mw.getStartDateTime(), mw.getEndDateTime())) {
                            isAvailable = false;
                            unavailableReason = "MAINTENANCE";
                            break;
                        }
                    }
                }

                // Check active bookings
                if (isAvailable) {
                    for (Booking b : activeBookings) {
                        LocalDateTime bookingStart = LocalDateTime.of(b.getBookingDate(), b.getStartTime());
                        LocalDateTime bookingEnd = endDateTime(b.getBookingDate(), b.getStartTime(), b.getEndTime());
                        if (overlaps(currentSlotStart, currentSlotEnd, bookingStart, bookingEnd)) {
                            isAvailable = false;
                            unavailableReason = "BOOKED";
                            break;
                        }
                    }
                }

                // Determine price for slot
                BigDecimal price = court.getHourlyRate();
                for (CourtPricing cp : pricings) {
                    LocalDateTime pricingStart = LocalDateTime.of(date, cp.getStartTime());
                    LocalDateTime pricingEnd = endDateTime(date, cp.getStartTime(), cp.getEndTime());
                    if (!currentSlotStart.isBefore(pricingStart) && !currentSlotEnd.isAfter(pricingEnd)) {
                        price = cp.getPrice();
                        break;
                    }
                }

                slots.add(AvailabilitySlotResponse.builder()
                        .startTime(currentSlotStart.toLocalTime())
                        .endTime(currentSlotEnd.toLocalTime())
                        .available(isAvailable)
                        .price(price)
                        .currency("LKR")
                        .reason(unavailableReason)
                        .build());

                currentSlotStart = currentSlotEnd;
            }
        }

        return AvailabilityResponse.builder()
                .date(date)
                .courtId(court.getId())
                .courtName(court.getName())
                .venueId(court.getVenue().getId())
                .venueName(court.getVenue().getName())
                .sportName(court.getSport().getName())
                .slots(slots)
                .build();
    }

    private static LocalDateTime endDateTime(LocalDate date, LocalTime startTime, LocalTime endTime) {
        LocalDateTime end = LocalDateTime.of(date, endTime);
        return endTime.isAfter(startTime) ? end : end.plusDays(1);
    }

    private static boolean overlaps(
            LocalDateTime firstStart, LocalDateTime firstEnd,
            LocalDateTime secondStart, LocalDateTime secondEnd) {
        return firstStart.isBefore(secondEnd) && firstEnd.isAfter(secondStart);
    }
}
