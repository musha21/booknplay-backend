package lk.booknplay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AdminDashboardResponse {
    private long customers;
    private long businesses;
    private long venues;
    private long pendingVenues;
    private long bookings;
    private long payments;
    private BigDecimal grossBookingValue;
}
