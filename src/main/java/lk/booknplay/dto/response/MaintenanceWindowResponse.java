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
public class MaintenanceWindowResponse {
    private String id;
    private String courtId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String description;
}
