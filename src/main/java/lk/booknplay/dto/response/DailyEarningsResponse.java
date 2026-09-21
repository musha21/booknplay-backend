package lk.booknplay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyEarningsResponse {
    private LocalDate date;
    private long bookingCount;
    private BigDecimal gross;
    private BigDecimal commission;
    private BigDecimal net;
}
