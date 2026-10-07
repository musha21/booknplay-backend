package lk.booknplay.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RefundResponse {
    private String status;
    private BigDecimal amount;
    private String mode;
    private String reference;
    private LocalDateTime processedAt;
    private String message;
}
