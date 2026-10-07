package lk.booknplay.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLoginRequest {

    /** Optional when {@link #phone} is provided (customer phone+password login). */
    @Email(message = "Invalid email format")
    private String email;

    /** Optional when {@link #email} is provided. Normalized server-side for customers. */
    private String phone;

    @NotBlank(message = "Password is required")
    private String password;

    @AssertTrue(message = "Email or phone is required")
    public boolean isEmailOrPhonePresent() {
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPhone = phone != null && !phone.isBlank();
        return hasEmail || hasPhone;
    }
}
