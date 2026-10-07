package lk.booknplay.service;

import lk.booknplay.dto.request.OtpRequestDto;
import lk.booknplay.dto.request.OtpVerifyRequest;
import lk.booknplay.dto.request.PasswordForgotRequest;
import lk.booknplay.dto.request.PasswordResetRequest;
import lk.booknplay.dto.request.PhoneRegisterRequest;
import lk.booknplay.dto.response.AuthResponse;
import lk.booknplay.dto.response.OtpVerifyResponse;

public interface PhoneAuthService {

    /** Request OTP to verify a phone for registration (not login). */
    void requestRegisterOtp(OtpRequestDto request);

    /** Verify registration OTP; always returns verificationToken (never JWT login). */
    OtpVerifyResponse verifyRegisterOtp(OtpVerifyRequest request);

    AuthResponse registerWithPhone(PhoneRegisterRequest request);

    void requestLinkPhoneOtp(String userEmail, OtpRequestDto request);

    AuthResponse verifyLinkPhoneOtp(String userEmail, OtpVerifyRequest request);

    /** Always returns success-shaped response; only sends SMS if customer phone exists. */
    void forgotPassword(PasswordForgotRequest request);

    void resetPassword(PasswordResetRequest request);
}
