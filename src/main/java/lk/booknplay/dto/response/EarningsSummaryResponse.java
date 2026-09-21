package lk.booknplay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarningsSummaryResponse {
    private LocalDate from;
    private LocalDate to;
    private long bookingCount;
    private BigDecimal gross;
    private BigDecimal commission;
    private BigDecimal net;
    private BigDecimal commissionPercent;
    private List<DailyEarningsResponse> daily;
}
