package lk.booknplay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutItemResponse {
    private String id;
    private String paymentId;
    private String bookingId;
    private BigDecimal gross;
    private BigDecimal refund;
    private BigDecimal commission;
    private BigDecimal gatewayFee;
    private BigDecimal net;
    private boolean adjustment;
}
