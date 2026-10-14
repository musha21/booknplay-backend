package lk.booknplay.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lk.booknplay.enums.PayoutFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerRegisterRequest {

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Invalid phone number format")
    private String contactPhone;

    @NotBlank(message = "Owner name is required")
    private String ownerName;

    @NotBlank(message = "Business address is required")
    private String address;

    private String contactEmail;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "Account holder name is required")
    private String bankAccountName;

    @NotBlank(message = "Account number is required")
    private String bankAccountNumber;

    @NotBlank(message = "Bank branch is required")
    private String bankBranch;

    private PayoutFrequency payoutFrequency;
}
