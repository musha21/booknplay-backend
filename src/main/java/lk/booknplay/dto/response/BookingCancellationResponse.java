package lk.booknplay.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingCancellationResponse {
    private BookingResponse booking;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
    private BigDecimal cancellationFee;
    private BigDecimal refundAmount;
    private RefundResponse refund;
}
