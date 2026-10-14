package lk.booknplay.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPayoutConfigUpdateRequest {
    private BigDecimal gatewayFeePercent;
    private String reason;
}
