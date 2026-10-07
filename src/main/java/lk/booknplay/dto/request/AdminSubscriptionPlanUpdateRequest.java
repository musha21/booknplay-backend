package lk.booknplay.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import lk.booknplay.util.CommaSeparatedOrListDeserializer;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminSubscriptionPlanUpdateRequest {
    private String name;
    private String description;
    private BigDecimal priceMonthly;
    private BigDecimal priceYearly;
    private String currency;
    private Boolean highlighted;
    private Boolean active;
    private Integer sortOrder;
    @JsonDeserialize(using = CommaSeparatedOrListDeserializer.class)
    private String features;
    private Integer maxVenues;
    private Integer maxCourtsPerVenue;
    /** When true, clears maxVenues (unlimited). */
    private Boolean unlimitedVenues;
    /** When true, clears maxCourtsPerVenue (unlimited). */
    private Boolean unlimitedCourtsPerVenue;
    private Boolean calendarEnabled;
    private Boolean walkInEnabled;
    private Boolean earningsEnabled;
    private Boolean reportsEnabled;
    private Boolean advancedReportsEnabled;
    private BigDecimal commissionPercent;
    private Integer maxStaff;
    /** When true, clears maxStaff (unlimited). */
    private Boolean unlimitedStaff;

    @NotBlank
    private String reason;
}
