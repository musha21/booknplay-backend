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
public class SportEarningsBreakdown {
    private String sportId;
    private String sportName;
    private long bookingCount;
    private BigDecimal gross;
    private BigDecimal commission;
    private BigDecimal net;
}
