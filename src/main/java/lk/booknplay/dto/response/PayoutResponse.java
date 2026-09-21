package lk.booknplay.dto.response;

import lk.booknplay.enums.PayoutStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutResponse {
    private String id;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal gross;
    private BigDecimal commission;
    private BigDecimal net;
    private PayoutStatus status;
    private String note;
    private LocalDateTime createdAt;
}
