package lk.booknplay.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityOnboardRequest {

    @NotBlank
    private String sportName;

    @Min(1)
    private int quantity;

    @Builder.Default
    private List<String> courtNames = new ArrayList<>();

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    @Builder.Default
    private Integer durationMinutes = 60;
}
