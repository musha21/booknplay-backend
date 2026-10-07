package lk.booknplay.service;

import lk.booknplay.enums.OtpPurpose;

public interface OtpService {

    /**
     * Create a new OTP for the phone/purpose, invalidate previous active OTPs,
     * and return the plaintext code once for SMS delivery. Never log the return value.
     */
    String createOtp(String normalizedPhone, OtpPurpose purpose);

    /**
     * Verify the OTP. Marks it verified on success.
     */
    void verifyOtp(String normalizedPhone, String otp, OtpPurpose purpose);

    /**
     * Invalidate the latest unused OTP (e.g. after SMS delivery failure).
     */
    void invalidateLatest(String normalizedPhone, OtpPurpose purpose);
}
