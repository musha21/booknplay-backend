package lk.booknplay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlotResponse {

    private LocalTime startTime;
    private LocalTime endTime;
    private boolean available;
    private BigDecimal price;
    private String currency;
    private String reason; // e.g. "BOOKED", "MAINTENANCE", "CLOSED", "BLOCKED"
}
