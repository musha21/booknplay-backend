package lk.booknplay.service;

import lk.booknplay.dto.response.EarningsSummaryResponse;
import lk.booknplay.entity.Booking;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.Sport;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.BookingSource;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.repository.BookingRepository;
import lk.booknplay.service.impl.OwnerEarningsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerEarningsServiceImplTest {

    @Mock
    private OwnerAccessService ownerAccessService;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PayoutSettlementService payoutSettlementService;
    @Mock
    private PlanEntitlementService planEntitlementService;

    @InjectMocks
    private OwnerEarningsServiceImpl ownerEarningsService;

    @Test
    void getSummary_AppliesCommissionToGross() {
        Business business = Business.builder()
                .id("biz-1")
                .commissionPercent(new BigDecimal("10.00"))
                .build();
        doNothing().when(ownerAccessService).requireStaffPermission(eq("owner@example.com"), any());
        when(ownerAccessService.requireBusiness("owner@example.com")).thenReturn(business);
        doNothing().when(planEntitlementService).assertEarnings(business);

        Booking booking = Booking.builder()
                .id("b-1")
                .bookingDate(LocalDate.of(2026, 9, 12))
                .totalAmount(new BigDecimal("10000.00"))
                .status(BookingStatus.CONFIRMED)
                .source(BookingSource.ONLINE)
                .sport(Sport.builder().id("sport-cricket").name("Cricket").build())
                .build();
        when(bookingRepository.findForEarnings(eq("biz-1"), any(), any(), any())).thenReturn(List.of(booking));

        EarningsSummaryResponse summary = ownerEarningsService.getSummary(
                "owner@example.com",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                null,
                null,
                null
        );

        assertEquals(1, summary.getBookingCount());
        assertEquals(new BigDecimal("10000.00"), summary.getGross());
        assertEquals(new BigDecimal("1000.00"), summary.getCommission());
        assertEquals(new BigDecimal("9000.00"), summary.getNet());
        assertEquals(1, summary.getBySport().size());
        assertEquals("sport-cricket", summary.getBySport().get(0).getSportId());
        assertEquals("Cricket", summary.getBySport().get(0).getSportName());
    }

    @Test
    void getSummary_ExcludesWalkInBookings() {
        Business business = Business.builder()
                .id("biz-1")
                .commissionPercent(new BigDecimal("10.00"))
                .build();
        doNothing().when(ownerAccessService).requireStaffPermission(eq("owner@example.com"), any());
        when(ownerAccessService.requireBusiness("owner@example.com")).thenReturn(business);
        doNothing().when(planEntitlementService).assertEarnings(business);

        Booking walkIn = Booking.builder()
                .id("b-walk")
                .bookingDate(LocalDate.of(2026, 9, 12))
                .totalAmount(new BigDecimal("5000.00"))
                .status(BookingStatus.CONFIRMED)
                .source(BookingSource.WALK_IN)
                .build();
        Booking online = Booking.builder()
                .id("b-online")
                .bookingDate(LocalDate.of(2026, 9, 12))
                .totalAmount(new BigDecimal("10000.00"))
                .status(BookingStatus.CONFIRMED)
                .source(BookingSource.ONLINE)
                .sport(Sport.builder().id("sport-cricket").name("Cricket").build())
                .build();
        when(bookingRepository.findForEarnings(eq("biz-1"), any(), any(), any()))
                .thenReturn(List.of(walkIn, online));

        EarningsSummaryResponse summary = ownerEarningsService.getSummary(
                "owner@example.com",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                null,
                null,
                null
        );

        assertEquals(1, summary.getBookingCount());
        assertEquals(new BigDecimal("10000.00"), summary.getGross());
    }

    @Test
    void getSummary_FiltersBySportAndVenue() {
        Business business = Business.builder()
                .id("biz-1")
                .commissionPercent(new BigDecimal("10.00"))
                .build();
        doNothing().when(ownerAccessService).requireStaffPermission(eq("owner@example.com"), any());
        when(ownerAccessService.requireBusiness("owner@example.com")).thenReturn(business);
        doNothing().when(planEntitlementService).assertEarnings(business);

        Sport cricket = Sport.builder().id("sport-cricket").name("Cricket").build();
        Sport badminton = Sport.builder().id("sport-badminton").name("Badminton").build();
        Venue venueA = Venue.builder().id("venue-a").name("Court A").build();
        Venue venueB = Venue.builder().id("venue-b").name("Court B").build();

        Booking keep = Booking.builder()
                .id("b-keep")
                .bookingDate(LocalDate.of(2026, 9, 12))
                .totalAmount(new BigDecimal("8000.00"))
                .status(BookingStatus.CONFIRMED)
                .source(BookingSource.ONLINE)
                .sport(cricket)
                .venue(venueA)
                .build();
        Booking wrongSport = Booking.builder()
                .id("b-sport")
                .bookingDate(LocalDate.of(2026, 9, 12))
                .totalAmount(new BigDecimal("5000.00"))
                .status(BookingStatus.CONFIRMED)
                .source(BookingSource.ONLINE)
                .sport(badminton)
                .venue(venueA)
                .build();
        Booking wrongVenue = Booking.builder()
                .id("b-venue")
                .bookingDate(LocalDate.of(2026, 9, 13))
                .totalAmount(new BigDecimal("7000.00"))
                .status(BookingStatus.CONFIRMED)
                .source(BookingSource.ONLINE)
                .sport(cricket)
                .venue(venueB)
                .build();
        when(bookingRepository.findForEarnings(eq("biz-1"), any(), any(), any()))
                .thenReturn(List.of(keep, wrongSport, wrongVenue));

        EarningsSummaryResponse summary = ownerEarningsService.getSummary(
                "owner@example.com",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                null,
                "sport-cricket",
                "venue-a"
        );

        assertEquals(1, summary.getBookingCount());
        assertEquals(new BigDecimal("8000.00"), summary.getGross());
        assertEquals(1, summary.getBySport().size());
        assertEquals("sport-cricket", summary.getBySport().get(0).getSportId());
        assertEquals(new BigDecimal("8000.00"), summary.getBySport().get(0).getGross());
    }
}
