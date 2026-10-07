package lk.booknplay.dto.response;

import lk.booknplay.enums.PlanCode;
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
public class SubscriptionPlanResponse {
    private PlanCode code;
    private String name;
    private String description;
    private BigDecimal priceMonthly;
    private BigDecimal priceYearly;
    private String currency;
    private boolean highlighted;
    private List<String> features;
    private int sortOrder;
    private boolean active;
    private Integer maxVenues;
    private Integer maxCourtsPerVenue;
    private boolean calendarEnabled;
    private boolean walkInEnabled;
    private boolean earningsEnabled;
    private boolean reportsEnabled;
    private boolean advancedReportsEnabled;
    private BigDecimal commissionPercent;
    private Integer maxStaff;
}
