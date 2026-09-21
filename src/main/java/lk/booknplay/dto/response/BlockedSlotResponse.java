package lk.booknplay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedSlotResponse {
    private String id;
    private String courtId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
}
