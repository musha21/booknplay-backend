package lk.booknplay.repository;

import lk.booknplay.dto.request.BookingCreateRequest;
import lk.booknplay.dto.request.WalkInBookingRequest;
import lk.booknplay.entity.Booking;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.Customer;
import lk.booknplay.entity.Sport;
import lk.booknplay.entity.User;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.BookingSource;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.enums.Role;
import lk.booknplay.exception.ConflictException;
import lk.booknplay.service.NotificationService;
import lk.booknplay.service.OwnerAccessService;
import lk.booknplay.service.BookingRulesService;
import lk.booknplay.service.BookingService;
import lk.booknplay.service.impl.OwnerCalendarServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BookingOverlapRepositoryTest {

    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final String OWNER_EMAIL = "owner@example.com";

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private CourtRepository courtRepository;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private BusinessRepository businessRepository;
    @Autowired
    private SportRepository sportRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private UserRepository userRepository;

    private OwnerCalendarServiceImpl ownerCalendarService;
    private Court court;
    private Sport sport;
    private Booking confirmed;
    private final LocalDate date = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUp() {
        BookingRulesService bookingRules = mock(BookingRulesService.class);
        org.mockito.stubbing.Answer<BookingRulesService.Result> priceAnswer = invocation -> {
            Court requestedCourt = invocation.getArgument(0);
            LocalDate requestedDate = invocation.getArgument(2);
            LocalTime start = invocation.getArgument(3);
            LocalTime end = invocation.getArgument(4);
            if (bookingRepository.existsOverlappingBooking(requestedCourt.getId(), requestedDate, start, end)) {
                throw new ConflictException("COURT_ALREADY_BOOKED", "The court is already booked for the selected time slot.");
            }
            return new BookingRulesService.Result(requestedCourt.getHourlyRate(),
                    java.util.List.of(new BookingRulesService.SlotPrice(start, end, requestedCourt.getHourlyRate())));
        };
        org.mockito.Mockito.when(bookingRules.validateAndPrice(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(priceAnswer);
        org.mockito.Mockito.when(bookingRules.validateAndPrice(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyBoolean()))
                .thenAnswer(priceAnswer);
        ownerCalendarService = new OwnerCalendarServiceImpl(
                mock(OwnerAccessService.class),
                mock(lk.booknplay.service.PlanEntitlementService.class),
                courtRepository,
                mock(OperatingHoursRepository.class),
                mock(CourtPricingRepository.class),
                mock(BlockedSlotRepository.class),
                mock(MaintenanceWindowRepository.class),
                bookingRepository,
                paymentRepository,
                bookingRules,
                mock(BookingService.class));

        User user = userRepository.save(User.builder()
                .email(CUSTOMER_EMAIL)
                .password("secret")
                .role(Role.CUSTOMER)
                .build());
        customerRepository.save(Customer.builder()
                .user(user)
                .firstName("John")
                .lastName("Doe")
                .phone("+94770000000")
                .build());

        Business business = businessRepository.save(Business.builder()
                .name("Arena Co")
                .ownerId("owner-1")
                .build());
        sport = sportRepository.save(Sport.builder().name("Futsal").build());
        Venue venue = venueRepository.save(Venue.builder()
                .business(business)
                .name("PlayArena")
                .address("Kandy")
                .city("Kandy")
                .build());
        court = courtRepository.save(Court.builder()
                .venue(venue)
                .sport(sport)
                .name("Court 1")
                .hourlyRate(new BigDecimal("2500"))
                .build());
        confirmed = bookingRepository.saveAndFlush(Booking.builder()
                .bookingRef("BNP-CONFIRMED")
                .court(court)
                .venue(venue)
                .sport(sport)
                .bookingDate(date)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .totalAmount(new BigDecimal("2500"))
                .status(BookingStatus.CONFIRMED)
                .source(BookingSource.ONLINE)
                .build());
    }

    @Test
    void walkInRejectsPartialOverlapAndCancelledSlotReopens() {
        org.junit.jupiter.api.Assertions.assertTrue(bookingRepository.existsOverlappingBooking(
                court.getId(), date, LocalTime.of(10, 30), LocalTime.of(11, 30)));
        ConflictException walkInConflict = assertThrows(ConflictException.class,
                () -> ownerCalendarService.createWalkIn(OWNER_EMAIL, walkInRequest(LocalTime.of(10, 30), LocalTime.of(11, 30))));

        assertEquals("COURT_ALREADY_BOOKED", walkInConflict.getCode());

        confirmed.setStatus(BookingStatus.CANCELLED);
        bookingRepository.saveAndFlush(confirmed);

        var reopened = ownerCalendarService.createWalkIn(OWNER_EMAIL, walkInRequest(LocalTime.of(10, 30), LocalTime.of(11, 30)));
        assertEquals(BookingStatus.CONFIRMED, reopened.getStatus());
        assertEquals(lk.booknplay.enums.CourtStatus.ACTIVE, courtRepository.findById(court.getId()).orElseThrow().getStatus());
    }

    private WalkInBookingRequest walkInRequest(LocalTime start, LocalTime end) {
        return WalkInBookingRequest.builder()
                .courtId(court.getId())
                .date(date)
                .startTime(start)
                .endTime(end)
                .guestName("Walk In")
                .guestPhone("+94771111111")
                .build();
    }
}
