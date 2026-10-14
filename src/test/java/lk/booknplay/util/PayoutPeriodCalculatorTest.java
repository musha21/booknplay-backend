package lk.booknplay.util;

import lk.booknplay.enums.PayoutFrequency;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayoutPeriodCalculatorTest {

    @Test
    void completedWeekly_IsPreviousMonSun() {
        // Wednesday 2026-10-07 → previous week Mon 2026-09-28 .. Sun 2026-10-04
        PayoutPeriodCalculator.Period period =
                PayoutPeriodCalculator.completedPeriod(PayoutFrequency.WEEKLY, LocalDate.of(2026, 10, 7));
        assertEquals(LocalDate.of(2026, 9, 28), period.start());
        assertEquals(LocalDate.of(2026, 10, 4), period.end());
    }

    @Test
    void completedMonthly_IsPreviousCalendarMonth() {
        PayoutPeriodCalculator.Period period =
                PayoutPeriodCalculator.completedPeriod(PayoutFrequency.MONTHLY, LocalDate.of(2026, 10, 12));
        assertEquals(LocalDate.of(2026, 9, 1), period.start());
        assertEquals(LocalDate.of(2026, 9, 30), period.end());
    }

    @Test
    void currentWeekly_ContainsAsOf() {
        PayoutPeriodCalculator.Period period =
                PayoutPeriodCalculator.currentPeriod(PayoutFrequency.WEEKLY, LocalDate.of(2026, 10, 8));
        assertEquals(LocalDate.of(2026, 10, 5), period.start());
        assertEquals(LocalDate.of(2026, 10, 11), period.end());
    }
}
