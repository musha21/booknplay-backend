package lk.booknplay.dto.response;

import lk.booknplay.enums.PayoutFrequency;
import lk.booknplay.enums.PayoutStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutResponse {
    private String id;
    private String businessId;
    private String businessName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private PayoutFrequency frequency;
    private BigDecimal gross;
    private BigDecimal refundsTotal;
    private BigDecimal commission;
    private BigDecimal gatewayFees;
    private BigDecimal net;
    private PayoutStatus status;
    private String note;
    private String transferReference;
    private LocalDate transferDate;
    private BigDecimal transferAmount;
    private String receiptUrl;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private List<PayoutItemResponse> items;
}
