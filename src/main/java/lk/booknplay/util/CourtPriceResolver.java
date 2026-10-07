package lk.booknplay.util;

import lk.booknplay.entity.CourtPricing;
import lk.booknplay.enums.PricingRuleType;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * Resolves the LKR price for a single bookable hour.
 *
 * <p><b>Implemented precedence</b> (highest wins among matching bands):
 * WEEKEND → PEAK → CUSTOM → Court.hourlyRate (Normal).
 *
 * <p><b>Target future precedence</b> (not implemented yet — next epic):
 * Promo code → Early-bird → Holiday → Weekend → Peak → Normal.
 * Member pricing remains a separate product concern.
 */
public final class CourtPriceResolver {

    private CourtPriceResolver() {
    }

    public static BigDecimal resolve(BigDecimal hourlyRate, List<CourtPricing> rules,
                                     LocalTime slotStart, LocalTime slotEnd) {
        BigDecimal base = hourlyRate != null ? hourlyRate : BigDecimal.ZERO;
        if (rules == null || rules.isEmpty() || slotStart == null || slotEnd == null) {
            return base;
        }

        return rules.stream()
                .filter(rule -> matches(rule, slotStart, slotEnd))
                .max(Comparator
                        .comparingInt(CourtPriceResolver::effectivePriority)
                        .thenComparingInt(r -> typeRank(effectiveType(r))))
                .map(CourtPricing::getPrice)
                .filter(price -> price != null)
                .orElse(base);
    }

    public static boolean matches(CourtPricing rule, LocalTime slotStart, LocalTime slotEnd) {
        if (rule == null || rule.getStartTime() == null || rule.getEndTime() == null) {
            return false;
        }
        return !slotStart.isBefore(rule.getStartTime()) && !slotEnd.isAfter(rule.getEndTime());
    }

    public static PricingRuleType effectiveType(CourtPricing rule) {
        if (rule.getRuleType() != null) {
            return rule.getRuleType();
        }
        DayOfWeek day = rule.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return PricingRuleType.WEEKEND;
        }
        return PricingRuleType.CUSTOM;
    }

    public static int effectivePriority(CourtPricing rule) {
        if (rule.getPriority() != null) {
            return rule.getPriority();
        }
        return defaultPriority(effectiveType(rule));
    }

    public static int defaultPriority(PricingRuleType type) {
        if (type == null) {
            return 10;
        }
        return switch (type) {
            case WEEKEND -> 30;
            case PEAK -> 20;
            case CUSTOM -> 10;
        };
    }

    public static PricingRuleType inferType(DayOfWeek day, PricingRuleType requested) {
        if (requested != null) {
            return requested;
        }
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return PricingRuleType.WEEKEND;
        }
        return PricingRuleType.CUSTOM;
    }

    private static int typeRank(PricingRuleType type) {
        return defaultPriority(type);
    }
}
