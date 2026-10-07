package lk.booknplay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpVerifyResponse {

    private Boolean verified;
    private Boolean registrationRequired;
    private String verificationToken;

    /**
     * When {@code registrationRequired} is false, holds the same payload as email login.
     * Controllers may also return {@link AuthResponse} directly as {@code data}.
     */
    private AuthResponse auth;
}
