package lk.booknplay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationPreviewResponse {
    private boolean eligible;
    private BigDecimal amountPaid;
    private BigDecimal cancellationFee;
    private BigDecimal refundAmount;
    private String refundMethod;
    private LocalDateTime deadline;
    private String message;
    private String refundMode;
    private String policySnapshotSource;
}
