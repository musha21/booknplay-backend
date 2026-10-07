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
public class OwnerStaffResponse {
    private String id;
    private String userId;
    private String email;
    private String name;
    private boolean active;
    private boolean canCalendar;
    private boolean canWalkIns;
    private boolean canCourts;
    private boolean canReports;
    private boolean canEarnings;
    private boolean canVenues;
    private boolean canBilling;
    private LocalDateTime createdAt;
    private Integer maxStaff;
    private long activeStaffCount;
}
