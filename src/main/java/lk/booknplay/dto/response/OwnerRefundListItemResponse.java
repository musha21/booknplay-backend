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
public class OwnerRefundListItemResponse {
    private String id;
    private String bookingId;
    private String bookingRef;
    private String venueName;
    private String customerName;
    private BigDecimal amount;
    private String mode;
    private String status;
    private String reference;
    private LocalDateTime processedAt;
}
