package lk.booknplay.dto.response;

import lk.booknplay.enums.PromotionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionResponse {
    private String id;
    private String code;
    private String name;
    private PromotionType type;
    private BigDecimal value;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> venueIds;
    private List<String> sportIds;
    private Integer maxRedemptions;
    private int redemptionCount;
    private boolean active;
}
