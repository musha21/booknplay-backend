package lk.booknplay.service;

import lk.booknplay.dto.request.SlotSelectionRequest;
import lk.booknplay.entity.*;
import lk.booknplay.enums.CourtStatus;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.exception.ConflictException;
import lk.booknplay.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingRulesService {
    public static final int SLOT_MINUTES = 60;
    private static final ZoneId BOOKING_ZONE = ZoneId.of("Asia/Colombo");

    private final OperatingHoursRepository operatingHoursRepository;
    private final CourtPricingRepository courtPricingRepository;
    private final BlockedSlotRepository blockedSlotRepository;
    private final MaintenanceWindowRepository maintenanceWindowRepository;
    private final BookingRepository bookingRepository;

    /**
     * Legacy continuous range: expands every hour between start and end (no gaps).
     */
    @Transactional(readOnly = true)
    public Result validateAndPrice(Court court, Sport requestedSport, LocalDate date,
                                   LocalTime startTime, LocalTime endTime) {
        return validateAndPrice(court, requestedSport, date, startTime, endTime, true);
    }

    @Transactional(readOnly = true)
    public Result validateAndPrice(Court court, Sport requestedSport, LocalDate date,
                                   LocalTime startTime, LocalTime endTime, boolean requireFutureStart) {
        if (date == null || startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new BadRequestException("Booking must start and end on the same calendar day");
        }
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0 || minutes % SLOT_MINUTES != 0) {
            throw new BadRequestException("Booking duration must use complete 60-minute slots");
        }
        List<SlotSelectionRequest> slots = new ArrayList<>();
        for (LocalTime cursor = startTime; cursor.isBefore(endTime); cursor = cursor.plusMinutes(SLOT_MINUTES)) {
            slots.add(SlotSelectionRequest.builder()
                    .startTime(cursor)
                    .endTime(cursor.plusMinutes(SLOT_MINUTES))
                    .build());
        }
        return validateAndPrice(court, requestedSport, date, slots, requireFutureStart);
    }

    /**
     * Discrete hour slots. Gaps between selected hours are allowed and are not reserved.
     */
    @Transactional(readOnly = true)
    public Result validateAndPrice(Court court, Sport requestedSport, LocalDate date,
                                   List<SlotSelectionRequest> requestedSlots) {
        return validateAndPrice(court, requestedSport, date, requestedSlots, true);
    }

    @Transactional(readOnly = true)
    public Result validateAndPrice(Court court, Sport requestedSport, LocalDate date,
                                   List<SlotSelectionRequest> requestedSlots, boolean requireFutureStart) {
        if (court.getStatus() != CourtStatus.ACTIVE) {
            throw new BadRequestException("Court is currently inactive");
        }
        VenueStatus venueStatus = court.getVenue().getStatus();
        if (venueStatus != VenueStatus.ACTIVE && venueStatus != VenueStatus.APPROVED) {
            throw new BadRequestException("Venue is not available for booking");
        }
        if (court.getSport() == null || requestedSport == null
                || !court.getSport().getId().equals(requestedSport.getId())) {
            throw new BadRequestException("The selected sport is not offered by this court");
        }
        if (date == null) {
            throw new BadRequestException("Booking date is required");
        }
        if (requestedSlots == null || requestedSlots.isEmpty()) {
            throw new BadRequestException("Select at least one time slot");
        }

        List<SlotSelectionRequest> sorted = requestedSlots.stream()
                .sorted(Comparator.comparing(SlotSelectionRequest::getStartTime))
                .toList();

        Set<LocalTime> seenStarts = new HashSet<>();
        for (SlotSelectionRequest slot : sorted) {
            if (slot.getStartTime() == null || slot.getEndTime() == null) {
                throw new BadRequestException("Each slot needs a start and end time");
            }
            if (!slot.getStartTime().isBefore(slot.getEndTime())) {
                throw new BadRequestException("Each slot must start before it ends on the same day");
            }
            long minutes = Duration.between(slot.getStartTime(), slot.getEndTime()).toMinutes();
            if (minutes != SLOT_MINUTES) {
                throw new BadRequestException("Each selected slot must be exactly 60 minutes");
            }
            if (!seenStarts.add(slot.getStartTime())) {
                throw new BadRequestException("Duplicate time slots are not allowed");
            }
        }

        LocalDateTime now = LocalDateTime.now(BOOKING_ZONE);
        OperatingHours hours = operatingHoursRepository
                .findByVenueIdAndDayOfWeek(court.getVenue().getId(), date.getDayOfWeek())
                .orElse(null);
        LocalTime open = hours == null ? LocalTime.of(6, 0) : hours.getOpenTime();
        LocalTime close = hours == null ? LocalTime.of(22, 0) : hours.getCloseTime();
        if (hours != null && hours.isClosed()) {
            throw new ConflictException("VENUE_CLOSED", "The venue is closed on the selected day.");
        }
        if (open == null || close == null || !close.isAfter(open)) {
            throw new BadRequestException("Overnight operating hours are not supported");
        }

        List<BlockedSlot> blocked = blockedSlotRepository.findByCourtIdAndDate(court.getId(), date);
        List<CourtPricing> pricing = courtPricingRepository.findByCourtIdAndDayOfWeek(court.getId(), date.getDayOfWeek());

        List<SlotPrice> slots = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (SlotSelectionRequest selection : sorted) {
            LocalTime startTime = selection.getStartTime();
            LocalTime endTime = selection.getEndTime();
            LocalDateTime start = LocalDateTime.of(date, startTime);
            LocalDateTime end = LocalDateTime.of(date, endTime);

            if (requireFutureStart && !start.isAfter(now)) {
                throw new BadRequestException("Booking start time must be in the future");
            }
            if (startTime.isBefore(open) || endTime.isAfter(close)) {
                throw new ConflictException("OUTSIDE_OPERATING_HOURS", "The selected time is outside venue operating hours.");
            }
            if (Duration.between(open, startTime).toMinutes() % SLOT_MINUTES != 0) {
                throw new BadRequestException("Booking start must align with the venue's 60-minute slots");
            }
            for (BlockedSlot blockedSlot : blocked) {
                if (overlaps(startTime, endTime, blockedSlot.getStartTime(), blockedSlot.getEndTime())) {
                    throw new ConflictException("SLOT_BLOCKED", "The selected time is blocked by the venue.");
                }
            }
            if (!maintenanceWindowRepository.findOverlappingMaintenance(court.getId(), start, end).isEmpty()) {
                throw new ConflictException("COURT_MAINTENANCE", "The selected time overlaps scheduled maintenance.");
            }
            if (bookingRepository.existsOverlappingBooking(court.getId(), date, startTime, endTime)) {
                throw new ConflictException("COURT_ALREADY_BOOKED", "The court is already booked for the selected time slot.");
            }

            BigDecimal price = lk.booknplay.util.CourtPriceResolver.resolve(
                    court.getHourlyRate(), pricing, startTime, endTime);
            if (price == null || price.signum() < 0) {
                throw new BadRequestException("A valid price is not configured for every selected slot");
            }
            slots.add(new SlotPrice(startTime, endTime, price));
            total = total.add(price);
        }

        return new Result(total, slots);
    }

    private boolean overlaps(LocalTime firstStart, LocalTime firstEnd, LocalTime secondStart, LocalTime secondEnd) {
        return firstStart.isBefore(secondEnd) && firstEnd.isAfter(secondStart);
    }

    public record SlotPrice(LocalTime startTime, LocalTime endTime, BigDecimal price) {}
    public record Result(BigDecimal totalAmount, List<SlotPrice> slots) {}
}
