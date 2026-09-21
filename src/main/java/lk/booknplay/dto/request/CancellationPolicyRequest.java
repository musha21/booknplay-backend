package lk.booknplay.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationPolicyRequest {

    @NotNull
    @Min(0)
    private Integer hoursBeforeDeadline;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal refundPercentage;
}
