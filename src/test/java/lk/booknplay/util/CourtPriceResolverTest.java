package lk.booknplay.util;

import lk.booknplay.entity.CourtPricing;
import lk.booknplay.enums.PricingRuleType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourtPriceResolverTest {

    @Test
    void fallsBackToHourlyRate() {
        BigDecimal price = CourtPriceResolver.resolve(
                new BigDecimal("3000"),
                List.of(),
                LocalTime.of(16, 0),
                LocalTime.of(17, 0));
        assertEquals(0, new BigDecimal("3000").compareTo(price));
    }

    @Test
    void peakOverridesNormal() {
        CourtPricing peak = CourtPricing.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(22, 0))
                .price(new BigDecimal("4000"))
                .ruleType(PricingRuleType.PEAK)
                .priority(20)
                .build();
        BigDecimal price = CourtPriceResolver.resolve(
                new BigDecimal("3000"),
                List.of(peak),
                LocalTime.of(19, 0),
                LocalTime.of(20, 0));
        assertEquals(0, new BigDecimal("4000").compareTo(price));
    }

    @Test
    void weekendBeatsPeakWhenBothMatch() {
        CourtPricing peak = CourtPricing.builder()
                .dayOfWeek(DayOfWeek.SATURDAY)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(22, 0))
                .price(new BigDecimal("4000"))
                .ruleType(PricingRuleType.PEAK)
                .priority(20)
                .build();
        CourtPricing weekend = CourtPricing.builder()
                .dayOfWeek(DayOfWeek.SATURDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(22, 0))
                .price(new BigDecimal("4500"))
                .ruleType(PricingRuleType.WEEKEND)
                .priority(30)
                .build();
        BigDecimal price = CourtPriceResolver.resolve(
                new BigDecimal("3000"),
                List.of(peak, weekend),
                LocalTime.of(19, 0),
                LocalTime.of(20, 0));
        assertEquals(0, new BigDecimal("4500").compareTo(price));
    }

    @Test
    void infersWeekendFromDayOfWeekWhenTypeMissing() {
        CourtPricing legacy = CourtPricing.builder()
                .dayOfWeek(DayOfWeek.SUNDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(22, 0))
                .price(new BigDecimal("4500"))
                .build();
        assertEquals(PricingRuleType.WEEKEND, CourtPriceResolver.effectiveType(legacy));
        assertEquals(30, CourtPriceResolver.effectivePriority(legacy));
    }
}
