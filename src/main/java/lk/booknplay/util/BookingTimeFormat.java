package lk.booknplay.util;

import lk.booknplay.dto.response.BookingSlotResponse;
import lk.booknplay.entity.Booking;
import lk.booknplay.entity.BookingSlot;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Formats booking times from discrete {@link BookingSlot} rows, merging contiguous
 * hours and listing gaps. Falls back to the booking envelope when no slots exist.
 */
public final class BookingTimeFormat {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private BookingTimeFormat() {
    }

    public static String formatBooking(Booking booking) {
        if (booking == null) {
            return "";
        }
        List<BookingSlot> slots = booking.getSlots();
        return formatSlots(slots, booking.getStartTime(), booking.getEndTime());
    }

    public static String formatSlots(List<BookingSlot> slots, LocalTime envelopeStart, LocalTime envelopeEnd) {
        if (slots != null && !slots.isEmpty()) {
            List<LocalTime[]> ranges = new ArrayList<>();
            for (BookingSlot slot : slots) {
                if (slot == null || slot.getStartTime() == null || slot.getEndTime() == null) {
                    continue;
                }
                ranges.add(new LocalTime[]{slot.getStartTime(), slot.getEndTime()});
            }
            if (!ranges.isEmpty()) {
                return formatRanges(ranges);
            }
        }
        return formatEnvelope(envelopeStart, envelopeEnd);
    }

    public static List<BookingSlotResponse> toSlotResponses(List<BookingSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return List.of();
        }
        return slots.stream()
                .filter(s -> s != null && s.getStartTime() != null && s.getEndTime() != null)
                .sorted(Comparator.comparing(BookingSlot::getStartTime))
                .map(s -> BookingSlotResponse.builder()
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .price(s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO)
                        .build())
                .toList();
    }

    private static String formatRanges(List<LocalTime[]> ranges) {
        ranges.sort(Comparator.comparing(r -> r[0]));
        List<LocalTime[]> merged = new ArrayList<>();
        LocalTime runStart = ranges.get(0)[0];
        LocalTime runEnd = ranges.get(0)[1];
        for (int i = 1; i < ranges.size(); i++) {
            LocalTime nextStart = ranges.get(i)[0];
            LocalTime nextEnd = ranges.get(i)[1];
            if (nextStart.equals(runEnd) || !nextStart.isAfter(runEnd)) {
                if (nextEnd.isAfter(runEnd)) {
                    runEnd = nextEnd;
                }
            } else {
                merged.add(new LocalTime[]{runStart, runEnd});
                runStart = nextStart;
                runEnd = nextEnd;
            }
        }
        merged.add(new LocalTime[]{runStart, runEnd});

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < merged.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(formatEnvelope(merged.get(i)[0], merged.get(i)[1]));
        }
        return sb.toString();
    }

    private static String formatEnvelope(LocalTime start, LocalTime end) {
        if (start == null && end == null) {
            return "";
        }
        if (start == null) {
            return end.format(TIME_FMT);
        }
        if (end == null) {
            return start.format(TIME_FMT);
        }
        return start.format(TIME_FMT) + "-" + end.format(TIME_FMT);
    }
}
