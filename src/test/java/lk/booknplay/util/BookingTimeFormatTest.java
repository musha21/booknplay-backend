package lk.booknplay.util;

import lk.booknplay.entity.Booking;
import lk.booknplay.entity.BookingSlot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingTimeFormatTest {

    @Test
    void singleHour() {
        Booking booking = bookingWithSlots(
                slot("10:00", "11:00"),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );
        assertEquals("10:00-11:00", BookingTimeFormat.formatBooking(booking));
    }

    @Test
    void mergesContiguousHours() {
        Booking booking = bookingWithSlots(
                List.of(
                        slot("10:00", "11:00"),
                        slot("11:00", "12:00")
                ),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        );
        assertEquals("10:00-12:00", BookingTimeFormat.formatBooking(booking));
    }

    @Test
    void listsGappedSlots() {
        Booking booking = bookingWithSlots(
                List.of(
                        slot("10:00", "11:00"),
                        slot("13:00", "14:00")
                ),
                LocalTime.of(10, 0),
                LocalTime.of(14, 0)
        );
        assertEquals("10:00-11:00, 13:00-14:00", BookingTimeFormat.formatBooking(booking));
    }

    @Test
    void fallsBackToEnvelopeWhenNoSlots() {
        Booking booking = Booking.builder()
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(14, 0))
                .build();
        assertEquals("10:00-14:00", BookingTimeFormat.formatBooking(booking));
    }

    @Test
    void mergesMixedContiguousAndGaps() {
        Booking booking = bookingWithSlots(
                List.of(
                        slot("09:00", "10:00"),
                        slot("10:00", "11:00"),
                        slot("14:00", "15:00")
                ),
                LocalTime.of(9, 0),
                LocalTime.of(15, 0)
        );
        assertEquals("09:00-11:00, 14:00-15:00", BookingTimeFormat.formatBooking(booking));
    }

    private static Booking bookingWithSlots(BookingSlot slot, LocalTime start, LocalTime end) {
        return bookingWithSlots(List.of(slot), start, end);
    }

    private static Booking bookingWithSlots(List<BookingSlot> slots, LocalTime start, LocalTime end) {
        Booking booking = Booking.builder()
                .startTime(start)
                .endTime(end)
                .build();
        booking.setSlots(slots);
        return booking;
    }

    private static BookingSlot slot(String start, String end) {
        return BookingSlot.builder()
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .price(BigDecimal.TEN)
                .build();
    }
}
