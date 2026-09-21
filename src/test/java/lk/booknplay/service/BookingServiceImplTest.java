package lk.booknplay.service;

import lk.booknplay.dto.request.BookingCreateRequest;
import lk.booknplay.entity.*;
import lk.booknplay.enums.CourtStatus;
import lk.booknplay.enums.Role;
import lk.booknplay.exception.ConflictException;
import lk.booknplay.repository.*;
import lk.booknplay.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private SportRepository sportRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Customer customer;
    private Court court;
    private Sport sport;
    private BookingCreateRequest request;

    @BeforeEach
    void setUp() {
        User user = User.builder().id("u-1").email("customer@example.com").role(Role.CUSTOMER).build();
        customer = Customer.builder().id("c-1").user(user).firstName("John").lastName("Doe").phone("+94770000000").build();
        Venue venue = Venue.builder().id("v-1").name("PlayArena").build();
        sport = Sport.builder().id("s-1").name("Badminton").build();
        court = Court.builder().id("crt-1").venue(venue).sport(sport).name("Court 1").hourlyRate(BigDecimal.valueOf(2500)).status(CourtStatus.ACTIVE).build();

        request = BookingCreateRequest.builder()
                .courtId("crt-1")
                .sportId("s-1")
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .build();
    }

    @Test
    void createBooking_OverlappingSlot_ThrowsConflictException() {
        when(customerRepository.findByUser_Email("customer@example.com")).thenReturn(Optional.of(customer));
        when(courtRepository.findByIdWithLock("crt-1")).thenReturn(Optional.of(court));
        when(sportRepository.findById("s-1")).thenReturn(Optional.of(sport));
        when(bookingRepository.existsOverlappingBooking(any(), any(), any(), any())).thenReturn(true);

        assertThrows(ConflictException.class, () -> bookingService.createBooking("customer@example.com", request));
    }
}
