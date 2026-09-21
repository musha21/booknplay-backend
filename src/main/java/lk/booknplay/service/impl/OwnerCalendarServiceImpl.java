package lk.booknplay.service.impl;

import lk.booknplay.dto.request.BlockedSlotRequest;
import lk.booknplay.dto.request.BookingStatusUpdateRequest;
import lk.booknplay.dto.request.MaintenanceWindowRequest;
import lk.booknplay.dto.request.WalkInBookingRequest;
import lk.booknplay.dto.response.*;
import lk.booknplay.entity.*;
import lk.booknplay.enums.BookingSource;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.enums.CourtStatus;
import lk.booknplay.enums.PaymentStatus;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.exception.ConflictException;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.repository.*;
import lk.booknplay.service.OwnerAccessService;
import lk.booknplay.service.OwnerCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerCalendarServiceImpl implements OwnerCalendarService {

    private static final int MAX_COURTS_PER_RESPONSE = 200;
    private static final int MAX_SLOTS_PER_COURT = 1_440;

    private static final Set<BookingStatus> ALLOWED_STATUS_UPDATES =
            EnumSet.of(BookingStatus.NO_SHOW, BookingStatus.COMPLETED, BookingStatus.CANCELLED);

    private final OwnerAccessService ownerAccessService;
    private final CourtRepository courtRepository;
    private final OperatingHoursRepository operatingHoursRepository;
    private final CourtPricingRepository courtPricingRepository;
    private final BlockedSlotRepository blockedSlotRepository;
    private final MaintenanceWindowRepository maintenanceWindowRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    @Value("${booknplay.calendar.slot-duration-minutes:60}")
    private int slotDurationMinutes = 60;

    @Override
    @Transactional(readOnly = true)
    public OwnerCalendarResponse getCalendar(String ownerEmail, String venueId, LocalDate date, String courtId) {
        if (date == null) {
            throw new BadRequestException("Calendar date is required");
        }
        Duration slotDuration = validatedSlotDuration();
        if (date.equals(LocalDate.MAX)) {
            throw new BadRequestException("Calendar date is outside the supported range");
        }
        Venue venue = ownerAccessService.requireVenue(ownerEmail, venueId);
        List<Court> courts = courtId == null || courtId.isBlank()
                ? courtRepository.findCalendarCourts(
                        venueId, CourtStatus.DELETED, PageRequest.of(0, MAX_COURTS_PER_RESPONSE + 1))
                : courtRepository.findCalendarCourt(courtId, venueId, CourtStatus.DELETED)
                        .map(List::of)
                        .orElseGet(List::of);

        if (courts.size() > MAX_COURTS_PER_RESPONSE) {
            throw new BadRequestException("Calendar request exceeds the maximum of "
                    + MAX_COURTS_PER_RESPONSE + " courts; filter by courtId");
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        Map<DayOfWeek, OperatingHours> hoursByDay = operatingHoursRepository.findByVenueId(venueId).stream()
                .collect(Collectors.toMap(OperatingHours::getDayOfWeek, Function.identity(), (first, ignored) -> first));
        OperatingHours operatingHours = hoursByDay.get(dayOfWeek);
        LocalTime openTime = operatingHours != null ? operatingHours.getOpenTime() : LocalTime.of(6, 0);
        LocalTime closeTime = operatingHours != null ? operatingHours.getCloseTime() : LocalTime.of(22, 0);
        boolean isClosed = operatingHours != null && operatingHours.isClosed();

        LocalDateTime periodStart = LocalDateTime.of(date, openTime);
        LocalDateTime periodEnd = LocalDateTime.of(date, closeTime);
        if (!closeTime.isAfter(openTime)) {
            periodEnd = periodEnd.plusDays(1);
        }

        List<String> courtIds = courts.stream().map(Court::getId).toList();
        Map<String, List<CourtPricing>> pricingsByCourt = new HashMap<>();
        Map<String, List<BlockedSlot>> blocksByCourt = new HashMap<>();
        Map<String, List<MaintenanceWindow>> maintenanceByCourt = new HashMap<>();
        Map<String, List<Booking>> bookingsByCourt = new HashMap<>();
        if (!courtIds.isEmpty() && !isClosed) {
            pricingsByCourt = groupByCourt(courtPricingRepository.findByCourtIdInAndDayOfWeek(courtIds, dayOfWeek));
            blocksByCourt = groupByCourt(blockedSlotRepository.findByCourtIdInAndDateBetween(
                    courtIds, date, periodEnd.toLocalDate()));
            maintenanceByCourt = groupByCourt(maintenanceWindowRepository.findOverlappingMaintenanceForCourts(
                    courtIds, periodStart, periodEnd));
            bookingsByCourt = groupByCourt(bookingRepository.findByCourtIdInAndBookingDateBetweenAndStatusNot(
                    courtIds, date, periodEnd.toLocalDate(), BookingStatus.CANCELLED));
        }

        Map<String, List<CourtPricing>> finalPricingsByCourt = pricingsByCourt;
        Map<String, List<BlockedSlot>> finalBlocksByCourt = blocksByCourt;
        Map<String, List<MaintenanceWindow>> finalMaintenanceByCourt = maintenanceByCourt;
        Map<String, List<Booking>> finalBookingsByCourt = bookingsByCourt;
        List<AvailabilityResponse> courtCalendars = courts.stream()
                .map(court -> buildCourtDay(
                        court, date, openTime, closeTime, isClosed, slotDuration,
                        finalPricingsByCourt.getOrDefault(court.getId(), List.of()),
                        finalBlocksByCourt.getOrDefault(court.getId(), List.of()),
                        finalMaintenanceByCourt.getOrDefault(court.getId(), List.of()),
                        finalBookingsByCourt.getOrDefault(court.getId(), List.of())))
                .toList();

        return OwnerCalendarResponse.builder()
                .date(date)
                .venueId(venue.getId())
                .venueName(venue.getName())
                .courts(courtCalendars)
                .build();
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingResponse createWalkIn(String ownerEmail, WalkInBookingRequest request) {
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        Court court = courtRepository.findByIdWithLock(request.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Court not found with id: " + request.getCourtId()));
        ownerAccessService.requireVenue(ownerEmail, court.getVenue().getId());

        if (court.getStatus() != CourtStatus.ACTIVE) {
            throw new BadRequestException("Court is currently inactive");
        }

        if (bookingRepository.existsOverlappingBooking(
                court.getId(), request.getDate(), request.getStartTime(), request.getEndTime())) {
            throw new ConflictException("COURT_ALREADY_BOOKED", "The court is already booked for the selected time slot.");
        }

        long hours = Duration.between(request.getStartTime(), request.getEndTime()).toHours();
        if (hours <= 0) {
            hours = 1;
        }
        BigDecimal totalAmount = request.getAmount() != null
                ? request.getAmount()
                : court.getHourlyRate().multiply(BigDecimal.valueOf(hours));

        String bookingRef = "BNP-WI-" + request.getDate().toString().replace("-", "") + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Booking booking = Booking.builder()
                .bookingRef(bookingRef)
                .court(court)
                .venue(court.getVenue())
                .sport(court.getSport())
                .bookingDate(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .totalAmount(totalAmount)
                .status(BookingStatus.CONFIRMED)
                .source(BookingSource.WALK_IN)
                .guestName(request.getGuestName())
                .guestPhone(request.getGuestPhone())
                .build();

        Booking saved = bookingRepository.save(booking);

        paymentRepository.save(Payment.builder()
                .booking(saved)
                .amount(totalAmount)
                .currency("LKR")
                .paymentGateway("WALK_IN_CASH")
                .status(PaymentStatus.PAID)
                .build());

        return mapBooking(saved, PaymentStatus.PAID);
    }

    @Override
    @Transactional
    public BookingResponse updateBookingStatus(String ownerEmail, String bookingId, BookingStatusUpdateRequest request) {
        if (!ALLOWED_STATUS_UPDATES.contains(request.getStatus())) {
            throw new BadRequestException("Owner can only set NO_SHOW, COMPLETED, or CANCELLED");
        }
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        ownerAccessService.requireVenue(ownerEmail, booking.getVenue().getId());
        booking.setStatus(request.getStatus());
        Booking saved = bookingRepository.save(booking);
        PaymentStatus paymentStatus = paymentRepository.findByBookingId(saved.getId())
                .map(Payment::getStatus)
                .orElse(PaymentStatus.INITIATED);
        return mapBooking(saved, paymentStatus);
    }

    @Override
    @Transactional
    public MaintenanceWindowResponse addMaintenance(String ownerEmail, String courtId, MaintenanceWindowRequest request) {
        Court court = ownerAccessService.requireCourt(ownerEmail, courtId);
        if (!request.getStartDateTime().isBefore(request.getEndDateTime())) {
            throw new BadRequestException("Maintenance start must be before end");
        }
        MaintenanceWindow window = maintenanceWindowRepository.save(MaintenanceWindow.builder()
                .court(court)
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .description(request.getDescription())
                .build());
        return mapMaintenance(window);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceWindowResponse> listMaintenance(String ownerEmail, String courtId) {
        ownerAccessService.requireCourt(ownerEmail, courtId);
        return maintenanceWindowRepository.findByCourtId(courtId).stream().map(this::mapMaintenance).toList();
    }

    @Override
    @Transactional
    public void deleteMaintenance(String ownerEmail, String maintenanceId) {
        MaintenanceWindow window = maintenanceWindowRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance window not found"));
        ownerAccessService.requireCourt(ownerEmail, window.getCourt().getId());
        maintenanceWindowRepository.delete(window);
    }

    @Override
    @Transactional
    public BlockedSlotResponse addBlockedSlot(String ownerEmail, String courtId, BlockedSlotRequest request) {
        Court court = ownerAccessService.requireCourt(ownerEmail, courtId);
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BadRequestException("Blocked slot start must be before end");
        }
        BlockedSlot slot = blockedSlotRepository.save(BlockedSlot.builder()
                .court(court)
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .reason(request.getReason())
                .build());
        return mapBlocked(slot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockedSlotResponse> listBlockedSlots(String ownerEmail, String courtId) {
        ownerAccessService.requireCourt(ownerEmail, courtId);
        return blockedSlotRepository.findByCourtId(courtId).stream().map(this::mapBlocked).toList();
    }

    @Override
    @Transactional
    public void deleteBlockedSlot(String ownerEmail, String blockedSlotId) {
        BlockedSlot slot = blockedSlotRepository.findById(blockedSlotId)
                .orElseThrow(() -> new ResourceNotFoundException("Blocked slot not found"));
        ownerAccessService.requireCourt(ownerEmail, slot.getCourt().getId());
        blockedSlotRepository.delete(slot);
    }

    private AvailabilityResponse buildCourtDay(
            Court court,
            LocalDate date,
            LocalTime openTime,
            LocalTime closeTime,
            boolean isClosed,
            Duration slotDuration,
            List<CourtPricing> pricings,
            List<BlockedSlot> blockedSlots,
            List<MaintenanceWindow> maintenanceWindows,
            List<Booking> activeBookings) {
        List<AvailabilitySlotResponse> slots = new ArrayList<>();
        if (!isClosed && court.getStatus() == CourtStatus.ACTIVE) {
            LocalDateTime periodEnd = LocalDateTime.of(date, closeTime);
            if (!closeTime.isAfter(openTime)) {
                periodEnd = periodEnd.plusDays(1);
            }
            LocalDateTime currentSlotStart = LocalDateTime.of(date, openTime);
            int generatedSlots = 0;
            while (!currentSlotStart.plus(slotDuration).isAfter(periodEnd)) {
                if (++generatedSlots > MAX_SLOTS_PER_COURT) {
                    throw new BadRequestException("Calendar slot count exceeds the supported maximum");
                }
                LocalDateTime currentSlotEnd = currentSlotStart.plus(slotDuration);
                boolean isAvailable = true;
                String unavailableReason = null;

                for (BlockedSlot bs : blockedSlots) {
                    LocalDateTime blockedStart = LocalDateTime.of(bs.getDate(), bs.getStartTime());
                    LocalDateTime blockedEnd = endDateTime(bs.getDate(), bs.getStartTime(), bs.getEndTime());
                    if (overlaps(currentSlotStart, currentSlotEnd, blockedStart, blockedEnd)) {
                        isAvailable = false;
                        unavailableReason = "BLOCKED";
                        break;
                    }
                }

                if (isAvailable) {
                    for (MaintenanceWindow mw : maintenanceWindows) {
                        if (overlaps(currentSlotStart, currentSlotEnd, mw.getStartDateTime(), mw.getEndDateTime())) {
                            isAvailable = false;
                            unavailableReason = "MAINTENANCE";
                            break;
                        }
                    }
                }

                if (isAvailable) {
                    for (Booking b : activeBookings) {
                        LocalDateTime bookingStart = LocalDateTime.of(b.getBookingDate(), b.getStartTime());
                        LocalDateTime bookingEnd = endDateTime(b.getBookingDate(), b.getStartTime(), b.getEndTime());
                        if (overlaps(currentSlotStart, currentSlotEnd, bookingStart, bookingEnd)) {
                            isAvailable = false;
                            unavailableReason = b.getStatus() == BookingStatus.PENDING ? "HELD" : "BOOKED";
                            break;
                        }
                    }
                }

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

    private Duration validatedSlotDuration() {
        if (slotDurationMinutes <= 0 || slotDurationMinutes > 1_440) {
            throw new BadRequestException("Calendar slot duration must be between 1 and 1440 minutes");
        }
        return Duration.ofMinutes(slotDurationMinutes);
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

    private static <T> Map<String, List<T>> groupByCourt(List<T> values) {
        return values.stream().collect(Collectors.groupingBy(value -> {
            if (value instanceof CourtPricing pricing) return pricing.getCourt().getId();
            if (value instanceof BlockedSlot blockedSlot) return blockedSlot.getCourt().getId();
            if (value instanceof MaintenanceWindow maintenance) return maintenance.getCourt().getId();
            if (value instanceof Booking booking) return booking.getCourt().getId();
            throw new IllegalArgumentException("Unsupported court calendar value: " + value.getClass().getName());
        }));
    }

    private BookingResponse mapBooking(Booking booking, PaymentStatus paymentStatus) {
        String customerName = booking.getCustomer() != null
                ? booking.getCustomer().getFirstName() + " " + booking.getCustomer().getLastName()
                : booking.getGuestName();
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingRef(booking.getBookingRef())
                .customerId(booking.getCustomer() != null ? booking.getCustomer().getId() : null)
                .customerName(customerName)
                .venueId(booking.getVenue().getId())
                .venueName(booking.getVenue().getName())
                .courtId(booking.getCourt().getId())
                .courtName(booking.getCourt().getName())
                .sportId(booking.getSport().getId())
                .sportName(booking.getSport().getName())
                .date(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .totalAmount(booking.getTotalAmount())
                .currency("LKR")
                .status(booking.getStatus())
                .paymentStatus(paymentStatus)
                .source(booking.getSource())
                .guestName(booking.getGuestName())
                .guestPhone(booking.getGuestPhone())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private MaintenanceWindowResponse mapMaintenance(MaintenanceWindow window) {
        return MaintenanceWindowResponse.builder()
                .id(window.getId())
                .courtId(window.getCourt().getId())
                .startDateTime(window.getStartDateTime())
                .endDateTime(window.getEndDateTime())
                .description(window.getDescription())
                .build();
    }

    private BlockedSlotResponse mapBlocked(BlockedSlot slot) {
        return BlockedSlotResponse.builder()
                .id(slot.getId())
                .courtId(slot.getCourt().getId())
                .date(slot.getDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .reason(slot.getReason())
                .build();
    }
}
