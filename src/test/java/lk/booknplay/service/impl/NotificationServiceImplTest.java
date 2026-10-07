package lk.booknplay.service.impl;

import lk.booknplay.entity.Booking;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.Customer;
import lk.booknplay.entity.Sport;
import lk.booknplay.entity.User;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.BookingSource;
import lk.booknplay.exception.SmsDeliveryException;
import lk.booknplay.repository.BookingRepository;
import lk.booknplay.service.SmsLenzService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock private SmsLenzService smsLenzService;
    @Mock private BookingRepository bookingRepository;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        lenient().when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        lenient().when(bookingRepository.findByIdWithSlots(any())).thenReturn(Optional.empty());
        notificationService = new NotificationServiceImpl(mailSenderProvider, smsLenzService, bookingRepository);
    }

    @Test
    void buildSmsMessage_gappedSlots_listsDiscreteTimes() {
        Booking booking = sampleOnlineBooking();
        booking.setStartTime(LocalTime.of(10, 0));
        booking.setEndTime(LocalTime.of(14, 0));
        booking.setSlots(java.util.List.of(
                lk.booknplay.entity.BookingSlot.builder()
                        .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                        .price(BigDecimal.TEN).build(),
                lk.booknplay.entity.BookingSlot.builder()
                        .startTime(LocalTime.of(13, 0)).endTime(LocalTime.of(14, 0))
                        .price(BigDecimal.TEN).build()
        ));
        String msg = NotificationServiceImpl.buildSmsMessage(booking, "BOOKING_CONFIRMED");
        assertTrue(msg.contains("10:00-11:00, 13:00-14:00"));
        assertFalse(msg.contains("10:00-14:00"));
    }

    @Test
    void buildSmsMessage_confirmed_includesDetails() {
        Booking booking = sampleOnlineBooking();
        String msg = NotificationServiceImpl.buildSmsMessage(booking, "BOOKING_CONFIRMED");
        assertTrue(msg.contains("confirmed"));
        assertTrue(msg.contains("BNP-1"));
        assertTrue(msg.contains("Arena"));
        assertTrue(msg.contains("Badminton"));
        assertTrue(msg.contains("Court A"));
        assertTrue(msg.contains("Arena, Badminton — Court A"));
        assertFalse(msg.contains("Court Court A"));
        assertTrue(msg.contains("2026-10-10"));
        assertTrue(msg.contains("18:00-19:00"));
        assertTrue(msg.contains("2500"));
    }

    @Test
    void buildSmsMessage_tableTennis_prefixesTableWhenNameLacksLabel() {
        Sport sport = Sport.builder().name("Table Tennis").build();
        Court court = Court.builder().name("A").sport(sport).build();
        Booking booking = sampleOnlineBooking();
        booking.setSport(sport);
        booking.setCourt(court);

        String msg = NotificationServiceImpl.buildSmsMessage(booking, "BOOKING_CONFIRMED");
        assertTrue(msg.contains("Table Tennis"));
        assertTrue(msg.contains("Table A"));
        assertTrue(msg.contains("Arena, Table Tennis — Table A"));
    }

    @Test
    void buildSmsMessage_cancelled_includesDetails() {
        Booking booking = sampleOnlineBooking();
        String msg = NotificationServiceImpl.buildSmsMessage(booking, "BOOKING_CANCELLED");
        assertTrue(msg.contains("cancelled"));
        assertTrue(msg.contains("BNP-1"));
        assertTrue(msg.contains("Badminton"));
        assertFalse(msg.contains("Amount"));
    }

    @Test
    void formatResourceName_skipsDuplicatePrefix() {
        assertEquals("Court A", NotificationServiceImpl.formatResourceName("Court", "Court A"));
        assertEquals("Table A", NotificationServiceImpl.formatResourceName("Table", "A"));
        assertEquals("Table 1", NotificationServiceImpl.formatResourceName("Table", "Table 1"));
    }

    @Test
    void resolveSmsPhone_prefersContactPhone() {
        Customer customer = Customer.builder().phone("0779999999").build();
        Booking booking = sampleOnlineBooking();
        booking.setContactPhone("0771234567");
        assertEquals("+94771234567", NotificationServiceImpl.resolveSmsPhone(customer, booking));
    }

    @Test
    void resolveSmsPhone_fallsBackToCustomerPhone() {
        Customer customer = Customer.builder().phone("0771234567").build();
        Booking booking = sampleOnlineBooking();
        booking.setContactPhone(null);
        assertEquals("+94771234567", NotificationServiceImpl.resolveSmsPhone(customer, booking));
    }

    @Test
    void resolveSmsPhone_invalid_returnsNull() {
        Booking booking = sampleOnlineBooking();
        booking.setContactPhone("123");
        assertNull(NotificationServiceImpl.resolveSmsPhone(null, booking));
    }

    @Test
    void sendBookingNotification_walkIn_sendsSmsWhenPhonePresent() {
        Booking booking = sampleOnlineBooking();
        booking.setSource(BookingSource.WALK_IN);
        booking.setContactPhone("0771234567");
        Customer customer = customerWithEmail();

        notificationService.sendBookingNotification(customer, booking, "BOOKING_CONFIRMED");

        verify(smsLenzService).sendSms(eq("+94771234567"), contains("confirmed"));
    }

    @Test
    void sendBookingNotification_onlineConfirmed_sendsSms() {
        Booking booking = sampleOnlineBooking();
        booking.setContactPhone("0771234567");
        Customer customer = customerWithEmail();

        notificationService.sendBookingNotification(customer, booking, "BOOKING_CONFIRMED");

        verify(smsLenzService).sendSms(eq("+94771234567"), contains("confirmed"));
    }

    @Test
    void sendBookingNotification_smsFailure_doesNotThrow() {
        Booking booking = sampleOnlineBooking();
        booking.setContactPhone("0771234567");
        Customer customer = customerWithEmail();
        doThrow(new SmsDeliveryException("fail")).when(smsLenzService).sendSms(anyString(), anyString());

        assertDoesNotThrow(() ->
                notificationService.sendBookingNotification(customer, booking, "BOOKING_CANCELLED"));
    }

    private static Booking sampleOnlineBooking() {
        Venue venue = Venue.builder().name("Arena").build();
        Sport sport = Sport.builder().name("Badminton").build();
        Court court = Court.builder().name("Court A").sport(sport).build();
        return Booking.builder()
                .bookingRef("BNP-1")
                .venue(venue)
                .sport(sport)
                .court(court)
                .bookingDate(LocalDate.of(2026, 10, 10))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .totalAmount(new BigDecimal("2500.00"))
                .source(BookingSource.ONLINE)
                .build();
    }

    private static Customer customerWithEmail() {
        User user = User.builder().email("a@example.com").build();
        return Customer.builder().firstName("Ann").phone("0779999999").user(user).build();
    }
}
