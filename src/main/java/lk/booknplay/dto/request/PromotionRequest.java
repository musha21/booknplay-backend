package lk.booknplay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class PromotionRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    @NotNull
    private PromotionType type;
    @NotNull
    @Positive
    private BigDecimal value;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    private List<String> venueIds;
    private List<String> sportIds;
    private Integer maxRedemptions;
    private Boolean active;
}
