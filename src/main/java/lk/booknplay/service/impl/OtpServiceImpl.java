package lk.booknplay.service.impl;

import lk.booknplay.config.OtpProperties;
import lk.booknplay.entity.OtpVerification;
import lk.booknplay.enums.OtpPurpose;
import lk.booknplay.exception.TooManyRequestsException;
import lk.booknplay.exception.UnauthorizedException;
import lk.booknplay.repository.OtpVerificationRepository;
import lk.booknplay.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties otpProperties;

    @Override
    @Transactional
    public String createOtp(String normalizedPhone, OtpPurpose purpose) {
        otpVerificationRepository.findFirstByPhoneAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(normalizedPhone, purpose)
                .ifPresent(existing -> {
                    Instant cooldownEnds = existing.getCreatedAt().plusSeconds(otpProperties.getResendCooldownSeconds());
                    if (cooldownEnds.isAfter(Instant.now())) {
                        throw new TooManyRequestsException("Please wait before requesting another code");
                    }
                });

        otpVerificationRepository.invalidateActiveOtps(normalizedPhone, purpose);

        String otp = generateSixDigitOtp();
        Instant now = Instant.now();

        OtpVerification record = OtpVerification.builder()
                .phone(normalizedPhone)
                .otpHash(passwordEncoder.encode(otp))
                .purpose(purpose)
                .expiresAt(now.plusSeconds(otpProperties.getExpirySeconds()))
                .attemptCount(0)
                .verified(false)
                .createdAt(now)
                .build();

        otpVerificationRepository.save(record);
        return otp;
    }

    @Override
    @Transactional
    public void verifyOtp(String normalizedPhone, String otp, OtpPurpose purpose) {
        OtpVerification record = otpVerificationRepository
                .findFirstByPhoneAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(normalizedPhone, purpose)
                .orElseThrow(() -> new UnauthorizedException("Invalid verification code"));

        if (record.getExpiresAt().isBefore(Instant.now())) {
            record.setVerified(true);
            otpVerificationRepository.save(record);
            throw new UnauthorizedException("Verification code expired");
        }

        if (record.getAttemptCount() >= otpProperties.getMaxAttempts()) {
            throw new TooManyRequestsException("Too many attempts");
        }

        if (otp == null || otp.isBlank() || !passwordEncoder.matches(otp.trim(), record.getOtpHash())) {
            record.setAttemptCount(record.getAttemptCount() + 1);
            otpVerificationRepository.save(record);
            if (record.getAttemptCount() >= otpProperties.getMaxAttempts()) {
                throw new TooManyRequestsException("Too many attempts");
            }
            throw new UnauthorizedException("Invalid verification code");
        }

        record.setVerified(true);
        otpVerificationRepository.save(record);
    }

    @Override
    @Transactional
    public void invalidateLatest(String normalizedPhone, OtpPurpose purpose) {
        otpVerificationRepository.invalidateActiveOtps(normalizedPhone, purpose);
    }

    private static String generateSixDigitOtp() {
        int value = RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}
