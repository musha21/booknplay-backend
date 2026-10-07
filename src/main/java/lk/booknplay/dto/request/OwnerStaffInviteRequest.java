package lk.booknplay.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OwnerStaffInviteRequest {
    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String temporaryPassword;

    private Boolean canCalendar;
    private Boolean canWalkIns;
    private Boolean canCourts;
    private Boolean canReports;
    private Boolean canEarnings;
    private Boolean canVenues;
    private Boolean canBilling;
}
