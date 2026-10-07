package lk.booknplay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingQuoteResponse {
    private String quoteId;
    private LocalDateTime expiresAt;
    private String paymentMode;
    private List<QuotedSlotResponse> slots;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private String promoCode;
    private BigDecimal payNow;
    private BigDecimal balanceDue;
    private String balanceCollection;
    private boolean cancellationAllowed;
    private LocalDateTime cancellationDeadline;
    private String afterDeadlineSummary;
    private String noShowSummary;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class QuotedSlotResponse {
        private String startTime;
        private String endTime;
        private BigDecimal price;
    }
}
