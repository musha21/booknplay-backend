package lk.booknplay.service;

import lk.booknplay.dto.response.EarningsSummaryResponse;
import lk.booknplay.entity.Booking;
import lk.booknplay.entity.Business;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.repository.BookingRepository;
import lk.booknplay.repository.PayoutRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerEarningsServiceImplTest {

    @Mock
    private OwnerAccessService ownerAccessService;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PayoutRepository payoutRepository;

    @InjectMocks
    private OwnerEarningsServiceImpl ownerEarningsService;

    @Test
    void getSummary_AppliesCommissionToGross() {
        Business business = Business.builder()
                .id("biz-1")
                .commissionPercent(new BigDecimal("10.00"))
                .build();
        when(ownerAccessService.requireBusiness("owner@example.com")).thenReturn(business);

        Booking booking = Booking.builder()
                .id("b-1")
                .bookingDate(LocalDate.of(2026, 9, 12))
                .totalAmount(new BigDecimal("10000.00"))
                .status(BookingStatus.CONFIRMED)
                .build();
        when(bookingRepository.findForEarnings(eq("biz-1"), any(), any(), any())).thenReturn(List.of(booking));

        EarningsSummaryResponse summary = ownerEarningsService.getSummary(
                "owner@example.com",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        assertEquals(1, summary.getBookingCount());
        assertEquals(new BigDecimal("10000.00"), summary.getGross());
        assertEquals(new BigDecimal("1000.00"), summary.getCommission());
        assertEquals(new BigDecimal("9000.00"), summary.getNet());
    }
}
