package lk.booknplay.util;

import lk.booknplay.entity.BusinessSubscription;
import lk.booknplay.enums.PlanCode;
import lk.booknplay.enums.SubscriptionAccessReason;
import lk.booknplay.enums.SubscriptionStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public final class SubscriptionAccess {

    public static final int TRIAL_DURATION_DAYS = 90;
    public static final int TRIAL_WARNING_DAYS = 14;

    private SubscriptionAccess() {
    }

    public static long daysRemaining(BusinessSubscription subscription, LocalDateTime now) {
        if (subscription == null) return 0;
        LocalDateTime end = resolveEnd(subscription);
        if (end == null) return 0;
        long days = ChronoUnit.DAYS.between(now.toLocalDate(), end.toLocalDate());
        return Math.max(0, days);
    }

    public static boolean canMutate(BusinessSubscription subscription, LocalDateTime now) {
        if (subscription == null) return false;
        refreshExpiredInMemory(subscription, now);
        SubscriptionStatus status = subscription.getStatus();
        return status == SubscriptionStatus.TRIALING
                || status == SubscriptionStatus.ACTIVE
                || status == SubscriptionStatus.PAST_DUE;
    }

    public static SubscriptionAccessReason accessReason(BusinessSubscription subscription, LocalDateTime now) {
        if (subscription == null || !canMutate(subscription, now)) {
            return SubscriptionAccessReason.TRIAL_EXPIRED;
        }
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE
                && subscription.getPlanCode() != PlanCode.TRIAL) {
            return SubscriptionAccessReason.SUBSCRIBED;
        }
        if (subscription.getStatus() == SubscriptionStatus.TRIALING
                || subscription.getPlanCode() == PlanCode.TRIAL) {
            long remaining = daysRemaining(subscription, now);
            if (remaining <= TRIAL_WARNING_DAYS) {
                return SubscriptionAccessReason.TRIAL_ENDING;
            }
            return SubscriptionAccessReason.TRIAL_ACTIVE;
        }
        return SubscriptionAccessReason.SUBSCRIBED;
    }

    /**
     * Marks TRIALING/ACTIVE rows expired in-memory when the end date has passed
     * (caller should persist if desired).
     */
    public static void refreshExpiredInMemory(BusinessSubscription subscription, LocalDateTime now) {
        if (subscription == null) return;
        LocalDateTime end = resolveEnd(subscription);
        if (end == null) return;
        if (!end.isBefore(now)) return;
        SubscriptionStatus status = subscription.getStatus();
        if (status == SubscriptionStatus.TRIALING
                || status == SubscriptionStatus.ACTIVE
                || status == SubscriptionStatus.PAST_DUE) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
        }
    }

    public static LocalDateTime resolveEnd(BusinessSubscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.TRIALING
                || subscription.getPlanCode() == PlanCode.TRIAL) {
            if (subscription.getTrialEndsAt() != null) return subscription.getTrialEndsAt();
        }
        if (subscription.getCurrentPeriodEnd() != null) return subscription.getCurrentPeriodEnd();
        return subscription.getTrialEndsAt();
    }
}
