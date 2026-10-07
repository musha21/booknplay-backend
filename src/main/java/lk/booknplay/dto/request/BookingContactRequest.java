package lk.booknplay.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingContactRequest {
    @NotBlank @Size(max = 150)
    private String fullName;
    @NotBlank @Email @Size(max = 190)
    private String email;
    @NotBlank @Size(max = 40)
    private String phoneNumber;
    @Size(max = 2000)
    private String specialRequests;
}
