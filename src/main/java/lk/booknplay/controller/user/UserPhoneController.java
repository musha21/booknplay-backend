package lk.booknplay.controller.user;

import jakarta.validation.Valid;
import lk.booknplay.dto.request.OtpRequestDto;
import lk.booknplay.dto.request.OtpVerifyRequest;
import lk.booknplay.dto.response.AuthResponse;
import lk.booknplay.service.PhoneAuthService;
import lk.booknplay.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/phone")
@RequiredArgsConstructor
public class UserPhoneController {

    private final PhoneAuthService phoneAuthService;

    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<Void>> requestLinkOtp(
            Authentication authentication,
            @Valid @RequestBody OtpRequestDto request) {
        phoneAuthService.requestLinkPhoneOtp(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Verification code sent"));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyLinkOtp(
            Authentication authentication,
            @Valid @RequestBody OtpVerifyRequest request) {
        AuthResponse response = phoneAuthService.verifyLinkPhoneOtp(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Phone number linked successfully", response));
    }
}
