package lk.booknplay.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessUpdateRequest {

    private String businessName;
    private String ownerName;

    @Email
    private String contactEmail;

    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Invalid phone number format")
    private String contactPhone;

    private String address;

    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String newPassword;

    private String currentPassword;
}
