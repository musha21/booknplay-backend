package lk.booknplay.util;

import lk.booknplay.enums.PayoutFrequency;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

public final class PayoutPeriodCalculator {

    public static final ZoneId ZONE = ZoneId.of("Asia/Colombo");

    private PayoutPeriodCalculator() {
    }

    public record Period(LocalDate start, LocalDate end) {
    }

    /** Completed period that ended strictly before {@code asOf}. */
    public static Period completedPeriod(PayoutFrequency frequency, LocalDate asOf) {
        if (frequency == PayoutFrequency.MONTHLY) {
            LocalDate firstOfThisMonth = asOf.withDayOfMonth(1);
            LocalDate end = firstOfThisMonth.minusDays(1);
            LocalDate start = end.withDayOfMonth(1);
            return new Period(start, end);
        }
        LocalDate thisMonday = asOf.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = thisMonday.minusDays(1); // Sunday
        LocalDate start = end.minusDays(6); // Monday
        return new Period(start, end);
    }

    /** Current in-progress period containing {@code asOf}. */
    public static Period currentPeriod(PayoutFrequency frequency, LocalDate asOf) {
        if (frequency == PayoutFrequency.MONTHLY) {
            LocalDate start = asOf.withDayOfMonth(1);
            LocalDate end = asOf.with(TemporalAdjusters.lastDayOfMonth());
            return new Period(start, end);
        }
        LocalDate start = asOf.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = start.plusDays(6);
        return new Period(start, end);
    }
}
