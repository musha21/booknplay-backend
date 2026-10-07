package lk.booknplay.util;

import lk.booknplay.entity.BusinessSubscription;
import lk.booknplay.enums.PlanCode;
import lk.booknplay.enums.SubscriptionAccessReason;
import lk.booknplay.enums.SubscriptionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionAccessTest {

    @Test
    void trialingAllowsMutateAndReportsActiveReason() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 1, 12, 0);
        BusinessSubscription sub = BusinessSubscription.builder()
                .businessId("b1")
                .planCode(PlanCode.TRIAL)
                .status(SubscriptionStatus.TRIALING)
                .trialStartsAt(now.minusDays(10))
                .trialEndsAt(now.plusDays(80))
                .build();

        assertTrue(SubscriptionAccess.canMutate(sub, now));
        assertEquals(SubscriptionAccessReason.TRIAL_ACTIVE, SubscriptionAccess.accessReason(sub, now));
        assertEquals(80, SubscriptionAccess.daysRemaining(sub, now));
    }

    @Test
    void trialEndingWithinFourteenDays() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 1, 12, 0);
        BusinessSubscription sub = BusinessSubscription.builder()
                .businessId("b1")
                .planCode(PlanCode.TRIAL)
                .status(SubscriptionStatus.TRIALING)
                .trialEndsAt(now.plusDays(3))
                .build();

        assertTrue(SubscriptionAccess.canMutate(sub, now));
        assertEquals(SubscriptionAccessReason.TRIAL_ENDING, SubscriptionAccess.accessReason(sub, now));
    }

    @Test
    void expiredTrialBlocksMutate() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 1, 12, 0);
        BusinessSubscription sub = BusinessSubscription.builder()
                .businessId("b1")
                .planCode(PlanCode.TRIAL)
                .status(SubscriptionStatus.TRIALING)
                .trialEndsAt(now.minusDays(1))
                .build();

        assertFalse(SubscriptionAccess.canMutate(sub, now));
        assertEquals(SubscriptionStatus.EXPIRED, sub.getStatus());
        assertEquals(SubscriptionAccessReason.TRIAL_EXPIRED, SubscriptionAccess.accessReason(sub, now));
    }

    @Test
    void activePaidPlanIsSubscribed() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 1, 12, 0);
        BusinessSubscription sub = BusinessSubscription.builder()
                .businessId("b1")
                .planCode(PlanCode.GROWTH)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(now.minusDays(5))
                .currentPeriodEnd(now.plusDays(25))
                .build();

        assertTrue(SubscriptionAccess.canMutate(sub, now));
        assertEquals(SubscriptionAccessReason.SUBSCRIBED, SubscriptionAccess.accessReason(sub, now));
    }
}
