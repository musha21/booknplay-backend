package lk.booknplay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private long customers;
    private long businesses;
    private long venues;
    private long pendingVenues;
    private long bookings;
    private long payments;
    private BigDecimal grossBookingValue;

    private long trialingCount;
    private long activePaidCount;
    private long expiredCount;
    private List<PlanSubscriptionStat> subscriptionByPlan;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanSubscriptionStat {
        private String code;
        private String name;
        private long businessCount;
        private BigDecimal commissionPercent;
    }
}
