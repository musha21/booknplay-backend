package lk.booknplay.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPayoutSettleRequest {
    private String transferReference;
    private LocalDate transferDate;
    private BigDecimal transferAmount;
    private String note;
    private String overrideReason;
}
