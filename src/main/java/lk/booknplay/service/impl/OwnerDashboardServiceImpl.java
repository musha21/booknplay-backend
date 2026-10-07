package lk.booknplay.service.impl;

import lk.booknplay.dto.response.OwnerDashboardResponse;
import lk.booknplay.entity.BlockedSlot;
import lk.booknplay.entity.Booking;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.MaintenanceWindow;
import lk.booknplay.entity.OperatingHours;
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
import lk.booknplay.repository.VenueRepository;
import lk.booknplay.service.OwnerAccessService;
import lk.booknplay.service.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerDashboardServiceImpl implements OwnerDashboardService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Colombo");
    private static final List<VenueStatus> LIVE = List.of(VenueStatus.APPROVED, VenueStatus.ACTIVE);
    private static final List<BookingStatus> REVENUE_STATUSES =
            List.copyOf(EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED));
    private static final List<BookingStatus> OCCUPANCY_STATUSES =
            List.copyOf(EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED, BookingStatus.PENDING));

    private final OwnerAccessService ownerAccessService;
    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final OperatingHoursRepository operatingHoursRepository;
    private final BlockedSlotRepository blockedSlotRepository;
    private final MaintenanceWindowRepository maintenanceWindowRepository;
    private final CourtPricingRepository courtPricingRepository;

    @Override
    @Transactional(readOnly = true)
    public OwnerDashboardResponse getToday(String ownerEmail) {
        Business business = ownerAccessService.requireBusiness(ownerEmail);
        LocalDate today = LocalDate.now(ZONE);
        LocalTime now = LocalTime.now(ZONE);

        List<Booking> todayBookings = bookingRepository.findForEarnings(
                business.getId(), today, today, REVENUE_STATUSES);
        BigDecimal revenue = todayBookings.stream()
                .map(Booking::getTotalAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        long upcoming = todayBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .filter(b -> b.getStartTime() != null && !b.getStartTime().isBefore(now))
                .count();

        List<Venue> allVenues = venueRepository.findByBusinessIdAndStatusNot(business.getId(), VenueStatus.DELETED);
        List<Venue> liveVenues = allVenues.stream().filter(v -> LIVE.contains(v.getStatus())).toList();

        int occupancy = computeOccupancy(liveVenues, today);
        OwnerDashboardResponse.OnboardingChecklist onboarding = buildOnboarding(allVenues, liveVenues);
        List<OwnerDashboardResponse.DashboardAlert> alerts = buildAlerts(business, onboarding, liveVenues);

        return OwnerDashboardResponse.builder()
                .todaysRevenue(revenue)
                .todaysBookings(todayBookings.size())
                .occupancyPercent(occupancy)
                .upcomingCount(upcoming)
                .currency("LKR")
                .onboarding(onboarding)
                .alerts(alerts)
                .build();
    }

    private int computeOccupancy(List<Venue> liveVenues, LocalDate today) {
        long availableMinutes = 0;
        long bookedMinutes = 0;

        for (Venue venue : liveVenues) {
            List<Court> courts = courtRepository.findByVenueId(venue.getId()).stream()
                    .filter(c -> c.getStatus() == CourtStatus.ACTIVE)
                    .toList();
            if (courts.isEmpty()) continue;

            OperatingHours hours = operatingHoursRepository
                    .findByVenueIdAndDayOfWeek(venue.getId(), today.getDayOfWeek())
                    .orElse(null);
            LocalTime open = LocalTime.of(6, 0);
            LocalTime close = LocalTime.of(22, 0);
            boolean closed = false;
            if (hours != null) {
                closed = hours.isClosed();
                open = hours.getOpenTime() != null ? hours.getOpenTime() : open;
                close = hours.getCloseTime() != null ? hours.getCloseTime() : close;
            }
            if (closed) continue;

            long openMinutes = minutesBetween(open, close);
            if (openMinutes <= 0) continue;

            List<String> courtIds = courts.stream().map(Court::getId).toList();
            List<BlockedSlot> blocks = blockedSlotRepository.findByCourtIdInAndDateBetween(courtIds, today, today);
            List<MaintenanceWindow> maintenance = maintenanceWindowRepository.findOverlappingMaintenanceForCourts(
                    courtIds, today.atStartOfDay(), today.plusDays(1).atStartOfDay());
            List<Booking> bookings = bookingRepository.findWithSlotsByCourtIdInAndBookingDateBetweenAndStatusIn(
                    courtIds, today, today, OCCUPANCY_STATUSES);

            for (Court court : courts) {
                long courtAvailable = openMinutes;
                for (BlockedSlot block : blocks) {
                    if (!block.getCourt().getId().equals(court.getId())) continue;
                    courtAvailable -= overlapMinutes(open, close, block.getStartTime(), block.getEndTime());
                }
                for (MaintenanceWindow mw : maintenance) {
                    if (!mw.getCourt().getId().equals(court.getId())) continue;
                    LocalTime mwStart = mw.getStartDateTime().toLocalDate().equals(today)
                            ? mw.getStartDateTime().toLocalTime() : open;
                    LocalTime mwEnd = mw.getEndDateTime().toLocalDate().equals(today)
                            ? mw.getEndDateTime().toLocalTime() : close;
                    if (mw.getStartDateTime().toLocalDate().isBefore(today)) mwStart = open;
                    if (mw.getEndDateTime().toLocalDate().isAfter(today)) mwEnd = close;
                    courtAvailable -= overlapMinutes(open, close, mwStart, mwEnd);
                }
                courtAvailable = Math.max(0, courtAvailable);
                availableMinutes += courtAvailable;

                for (Booking booking : bookings) {
                    if (!booking.getCourt().getId().equals(court.getId())) continue;
                    bookedMinutes += overlapMinutes(open, close, booking.getStartTime(), booking.getEndTime());
                }
            }
        }

        if (availableMinutes <= 0) return 0;
        long cappedBooked = Math.min(bookedMinutes, availableMinutes);
        return (int) Math.round((cappedBooked * 100.0) / availableMinutes);
    }

    private OwnerDashboardResponse.OnboardingChecklist buildOnboarding(List<Venue> allVenues, List<Venue> liveVenues) {
        boolean hasVenue = !allVenues.isEmpty();
        boolean hasLive = !liveVenues.isEmpty();
        boolean hasCourt = false;
        boolean hasHours = false;
        boolean hasPricing = false;

        for (Venue venue : allVenues) {
            List<Court> courts = courtRepository.findByVenueId(venue.getId()).stream()
                    .filter(c -> c.getStatus() != CourtStatus.DELETED)
                    .toList();
            if (!courts.isEmpty()) hasCourt = true;
            if (!operatingHoursRepository.findByVenueId(venue.getId()).isEmpty()) hasHours = true;
            for (Court court : courts) {
                if (!courtPricingRepository.findByCourtId(court.getId()).isEmpty()
                        || (court.getHourlyRate() != null && court.getHourlyRate().signum() > 0)) {
                    hasPricing = true;
                    break;
                }
            }
            if (hasCourt && hasHours && hasPricing) break;
        }

        boolean complete = hasVenue && hasLive && hasCourt && hasHours && hasPricing;
        return OwnerDashboardResponse.OnboardingChecklist.builder()
                .hasVenue(hasVenue)
                .hasLiveVenue(hasLive)
                .hasCourt(hasCourt)
                .hasOperatingHours(hasHours)
                .hasPricing(hasPricing)
                .complete(complete)
                .build();
    }

    private List<OwnerDashboardResponse.DashboardAlert> buildAlerts(
            Business business,
            OwnerDashboardResponse.OnboardingChecklist onboarding,
            List<Venue> liveVenues) {
        List<OwnerDashboardResponse.DashboardAlert> alerts = new ArrayList<>();
        if (!onboarding.isComplete()) {
            alerts.add(OwnerDashboardResponse.DashboardAlert.builder()
                    .code("ONBOARDING_INCOMPLETE")
                    .message("Finish setting up your venues to start taking online bookings.")
                    .severity("info")
                    .build());
        }
        if (liveVenues.isEmpty() && onboarding.isHasVenue()) {
            alerts.add(OwnerDashboardResponse.DashboardAlert.builder()
                    .code("NO_LIVE_VENUE")
                    .message("Publish a venue so customers can find and book you.")
                    .severity("warning")
                    .build());
        }
        if (business.getContactPhone() == null || business.getContactPhone().isBlank()) {
            alerts.add(OwnerDashboardResponse.DashboardAlert.builder()
                    .code("MISSING_PHONE")
                    .message("Add a business phone number in Settings so customers can reach you.")
                    .severity("info")
                    .build());
        }
        return alerts;
    }

    private static long minutesBetween(LocalTime start, LocalTime end) {
        if (start == null || end == null) return 0;
        if (!end.isAfter(start)) {
            // overnight window
            return java.time.Duration.between(start, LocalTime.MAX).toMinutes() + 1
                    + java.time.Duration.between(LocalTime.MIN, end).toMinutes();
        }
        return java.time.Duration.between(start, end).toMinutes();
    }

    private static long overlapMinutes(LocalTime windowStart, LocalTime windowEnd, LocalTime start, LocalTime end) {
        if (start == null || end == null) return 0;
        LocalTime s = start.isBefore(windowStart) ? windowStart : start;
        LocalTime e = end.isAfter(windowEnd) ? windowEnd : end;
        if (!e.isAfter(s)) return 0;
        return java.time.Duration.between(s, e).toMinutes();
    }
}
