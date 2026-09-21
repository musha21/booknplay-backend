package lk.booknplay.service.impl;

import lk.booknplay.dto.response.DailyEarningsResponse;
import lk.booknplay.dto.response.EarningsSummaryResponse;
import lk.booknplay.dto.response.PayoutResponse;
import lk.booknplay.entity.Booking;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.Payout;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.repository.BookingRepository;
import lk.booknplay.repository.PayoutRepository;
import lk.booknplay.service.OwnerAccessService;
import lk.booknplay.service.OwnerEarningsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerEarningsServiceImpl implements OwnerEarningsService {

    private static final List<BookingStatus> EARNING_STATUSES =
            List.copyOf(EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED));

    private final OwnerAccessService ownerAccessService;
    private final BookingRepository bookingRepository;
    private final PayoutRepository payoutRepository;

    @Override
    @Transactional(readOnly = true)
    public EarningsSummaryResponse getSummary(String ownerEmail, LocalDate from, LocalDate to) {
        Range range = resolveRange(from, to);
        Business business = ownerAccessService.requireBusiness(ownerEmail);
        List<Booking> bookings = bookingRepository.findForEarnings(
                business.getId(), range.from(), range.to(), EARNING_STATUSES);
        BigDecimal percent = business.getCommissionPercent() != null
                ? business.getCommissionPercent()
                : new BigDecimal("10.00");
        List<DailyEarningsResponse> daily = bucketDaily(bookings, percent);
        BigDecimal gross = daily.stream().map(DailyEarningsResponse::getGross).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commission = daily.stream().map(DailyEarningsResponse::getCommission).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = daily.stream().map(DailyEarningsResponse::getNet).reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = daily.stream().mapToLong(DailyEarningsResponse::getBookingCount).sum();
        return EarningsSummaryResponse.builder()
                .from(range.from())
                .to(range.to())
                .bookingCount(count)
                .gross(gross)
                .commission(commission)
                .net(net)
                .commissionPercent(percent)
                .daily(daily)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyEarningsResponse> getDaily(String ownerEmail, LocalDate from, LocalDate to) {
        return getSummary(ownerEmail, from, to).getDaily();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayoutResponse> listPayouts(String ownerEmail) {
        Business business = ownerAccessService.requireBusiness(ownerEmail);
        return payoutRepository.findByBusinessIdOrderByPeriodStartDesc(business.getId()).stream()
                .map(this::mapPayout)
                .toList();
    }

    private List<DailyEarningsResponse> bucketDaily(List<Booking> bookings, BigDecimal percent) {
        Map<LocalDate, List<Booking>> byDate = bookings.stream()
                .collect(Collectors.groupingBy(Booking::getBookingDate));
        List<DailyEarningsResponse> daily = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Booking>> entry : byDate.entrySet()) {
            BigDecimal gross = entry.getValue().stream()
                    .map(Booking::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal commission = gross.multiply(percent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            daily.add(DailyEarningsResponse.builder()
                    .date(entry.getKey())
                    .bookingCount(entry.getValue().size())
                    .gross(gross)
                    .commission(commission)
                    .net(gross.subtract(commission))
                    .build());
        }
        daily.sort(Comparator.comparing(DailyEarningsResponse::getDate));
        return daily;
    }

    private PayoutResponse mapPayout(Payout payout) {
        return PayoutResponse.builder()
                .id(payout.getId())
                .periodStart(payout.getPeriodStart())
                .periodEnd(payout.getPeriodEnd())
                .gross(payout.getGross())
                .commission(payout.getCommission())
                .net(payout.getNet())
                .status(payout.getStatus())
                .note(payout.getNote())
                .createdAt(payout.getCreatedAt())
                .build();
    }

    private Range resolveRange(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(29);
        if (start.isAfter(end)) {
            throw new lk.booknplay.exception.BadRequestException("from date must be on or before to date");
        }
        return new Range(start, end);
    }

    private record Range(LocalDate from, LocalDate to) {
    }
}
