package lk.booknplay.dto.request;

import lombok.Data;

@Data
public class OwnerStaffUpdateRequest {
    private String name;
    private Boolean active;
    private Boolean canCalendar;
    private Boolean canWalkIns;
    private Boolean canCourts;
    private Boolean canReports;
    private Boolean canEarnings;
    private Boolean canVenues;
    private Boolean canBilling;
}
