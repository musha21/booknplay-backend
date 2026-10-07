package lk.booknplay.dto.response;

import lk.booknplay.enums.PlanCode;
import lk.booknplay.enums.SubscriptionAccessReason;
import lk.booknplay.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private String businessId;
    private PlanCode planCode;
    private SubscriptionStatus status;
    private LocalDateTime trialStartsAt;
    private LocalDateTime trialEndsAt;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private boolean cancelAtPeriodEnd;
    private long daysRemaining;
    private SubscriptionAccess access;
    private PlanLimits limits;
    private SubscriptionUsage usage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionAccess {
        private boolean canMutate;
        private SubscriptionAccessReason reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanLimits {
        private Integer maxVenues;
        private Integer maxCourtsPerVenue;
        private Integer maxStaff;
        private boolean calendarEnabled;
        private boolean walkInEnabled;
        private boolean earningsEnabled;
        private boolean reportsEnabled;
        private boolean advancedReportsEnabled;
        private boolean promotionsEnabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionUsage {
        private long venueCount;
        private long staffCount;
    }
}
