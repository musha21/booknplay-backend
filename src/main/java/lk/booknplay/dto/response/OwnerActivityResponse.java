package lk.booknplay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerActivityResponse {
    private String id;
    private String type;
    private String summary;
    private String bookingRef;
    private String venueName;
    private LocalDateTime occurredAt;
}
