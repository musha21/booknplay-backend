package lk.booknplay.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import lk.booknplay.enums.PricingRuleType;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourtPricingUpdateRequest {

    @NotNull
    @Valid
    private List<PricingRule> rules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingRule {
        @NotNull
        private DayOfWeek dayOfWeek;
        @NotNull
        private LocalTime startTime;
        @NotNull
        private LocalTime endTime;
        @NotNull
        private BigDecimal price;
        private PricingRuleType ruleType;
        private Integer priority;
        private String label;
    }
}
