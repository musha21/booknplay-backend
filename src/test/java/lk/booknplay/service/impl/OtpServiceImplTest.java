package lk.booknplay.service.impl;

import lk.booknplay.config.OtpProperties;
import lk.booknplay.entity.OtpVerification;
import lk.booknplay.enums.OtpPurpose;
import lk.booknplay.exception.TooManyRequestsException;
import lk.booknplay.exception.UnauthorizedException;
import lk.booknplay.repository.OtpVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpProperties otpProperties;

    @InjectMocks
    private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        lenient().when(otpProperties.getExpirySeconds()).thenReturn(300);
        lenient().when(otpProperties.getMaxAttempts()).thenReturn(5);
        lenient().when(otpProperties.getResendCooldownSeconds()).thenReturn(60);
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "hash:" + inv.getArgument(0));
        lenient().when(passwordEncoder.matches(anyString(), anyString()))
                .thenAnswer(inv -> ("hash:" + inv.getArgument(0)).equals(inv.getArgument(1)));
    }

    @Test
    void createOtp_generatesSixDigitsAndSavesHash() {
        when(otpVerificationRepository.findFirstByPhoneAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());

        String otp = otpService.createOtp("+94771234567", OtpPurpose.LOGIN);

        assertNotNull(otp);
        assertTrue(otp.matches("\\d{6}"));

        ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
        verify(otpVerificationRepository).invalidateActiveOtps("+94771234567", OtpPurpose.LOGIN);
        verify(otpVerificationRepository).save(captor.capture());
        assertEquals("hash:" + otp, captor.getValue().getOtpHash());
        assertFalse(captor.getValue().isVerified());
    }

    @Test
    void createOtp_resendCooldown_throws429() {
        OtpVerification recent = OtpVerification.builder()
                .phone("+94771234567")
                .otpHash("hash:111111")
                .purpose(OtpPurpose.LOGIN)
                .createdAt(Instant.now().minusSeconds(10))
                .expiresAt(Instant.now().plusSeconds(290))
                .verified(false)
                .build();
        when(otpVerificationRepository.findFirstByPhoneAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(recent));

        assertThrows(TooManyRequestsException.class,
                () -> otpService.createOtp("+94771234567", OtpPurpose.LOGIN));
        verify(otpVerificationRepository, never()).save(any());
    }

    @Test
    void verifyOtp_correctCode_marksVerified() {
        OtpVerification record = activeRecord("hash:482193", 0);
        when(otpVerificationRepository.findFirstByPhoneAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(record));

        otpService.verifyOtp("+94771234567", "482193", OtpPurpose.LOGIN);

        assertTrue(record.isVerified());
        verify(otpVerificationRepository).save(record);
    }

    @Test
    void verifyOtp_wrongCode_incrementsAttempts() {
        OtpVerification record = activeRecord("hash:482193", 0);
        when(otpVerificationRepository.findFirstByPhoneAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(record));

        assertThrows(UnauthorizedException.class,
                () -> otpService.verifyOtp("+94771234567", "000000", OtpPurpose.LOGIN));
        assertEquals(1, record.getAttemptCount());
        assertFalse(record.isVerified());
    }

    @Test
    void verifyOtp_expired_throwsUnauthorized() {
        OtpVerification record = activeRecord("hash:482193", 0);
        record.setExpiresAt(Instant.now().minusSeconds(1));
        when(otpVerificationRepository.findFirstByPhoneAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(record));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> otpService.verifyOtp("+94771234567", "482193", OtpPurpose.LOGIN));
        assertEquals("Verification code expired", ex.getMessage());
        assertTrue(record.isVerified());
    }

    @Test
    void verifyOtp_reuseAfterVerified_throwsUnauthorized() {
        when(otpVerificationRepository.findFirstByPhoneAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> otpService.verifyOtp("+94771234567", "482193", OtpPurpose.LOGIN));
    }

    @Test
    void verifyOtp_maxAttempts_throws429() {
        OtpVerification record = activeRecord("hash:482193", 5);
        when(otpVerificationRepository.findFirstByPhoneAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(record));

        assertThrows(TooManyRequestsException.class,
                () -> otpService.verifyOtp("+94771234567", "482193", OtpPurpose.LOGIN));
    }

    private static OtpVerification activeRecord(String hash, int attempts) {
        return OtpVerification.builder()
                .phone("+94771234567")
                .otpHash(hash)
                .purpose(OtpPurpose.LOGIN)
                .createdAt(Instant.now().minusSeconds(30))
                .expiresAt(Instant.now().plusSeconds(270))
                .attemptCount(attempts)
                .verified(false)
                .build();
    }
}
