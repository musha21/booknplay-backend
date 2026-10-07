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
public class OwnerDashboardResponse {

    private BigDecimal todaysRevenue;
    private long todaysBookings;
    private int occupancyPercent;
    private long upcomingCount;
    private String currency;
    private OnboardingChecklist onboarding;
    private List<DashboardAlert> alerts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnboardingChecklist {
        private boolean hasVenue;
        private boolean hasLiveVenue;
        private boolean hasCourt;
        private boolean hasOperatingHours;
        private boolean hasPricing;
        private boolean complete;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardAlert {
        private String code;
        private String message;
        private String severity;
    }
}
