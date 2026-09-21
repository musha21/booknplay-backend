package lk.booknplay.dto.response;

import lk.booknplay.enums.CourtStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourtResponse {

    private String id;
    private String venueId;
    private String venueName;
    private String sportId;
    private String sportName;
    private String name;
    private BigDecimal hourlyRate;
    private Integer durationMinutes;
    private CourtStatus status;
}
