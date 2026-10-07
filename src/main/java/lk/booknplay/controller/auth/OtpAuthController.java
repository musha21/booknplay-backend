package lk.booknplay.controller.auth;

import jakarta.validation.Valid;
import lk.booknplay.dto.request.OtpRequestDto;
import lk.booknplay.dto.request.OtpVerifyRequest;
import lk.booknplay.dto.request.PasswordForgotRequest;
import lk.booknplay.dto.request.PasswordResetRequest;
import lk.booknplay.dto.request.PhoneRegisterRequest;
import lk.booknplay.dto.response.AuthResponse;
import lk.booknplay.dto.response.OtpVerifyResponse;
import lk.booknplay.service.PhoneAuthService;
import lk.booknplay.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class OtpAuthController {

    private final PhoneAuthService phoneAuthService;

    /** Request OTP to verify a new phone before registration. */
    @PostMapping("/otp/request")
    public ResponseEntity<ApiResponse<Void>> requestOtp(@Valid @RequestBody OtpRequestDto request) {
        phoneAuthService.requestRegisterOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Verification code sent"));
    }

    /** Verify registration OTP; returns verificationToken (never a session JWT). */
    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<OtpVerifyResponse>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        OtpVerifyResponse response = phoneAuthService.verifyRegisterOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Phone verified", response));
    }

    @PostMapping("/phone/register")
    public ResponseEntity<ApiResponse<AuthResponse>> registerWithPhone(
            @Valid @RequestBody PhoneRegisterRequest request) {
        AuthResponse response = phoneAuthService.registerWithPhone(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer registered successfully", response));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody PasswordForgotRequest request) {
        phoneAuthService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "If an account exists for this phone, a verification code has been sent"));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        phoneAuthService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password updated. You can sign in with your phone and new password"));
    }
}
