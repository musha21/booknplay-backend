package lk.booknplay.security.jwt;

import lk.booknplay.config.OtpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderPhoneRegistrationTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        OtpProperties otpProperties = new OtpProperties();
        otpProperties.setRegistrationTokenExpiryMs(900_000L);
        tokenProvider = new JwtTokenProvider(jwtProperties, otpProperties);
    }

    @Test
    void phoneRegistrationToken_roundTrip() {
        String token = tokenProvider.generatePhoneRegistrationToken("+94771234567");
        assertEquals("+94771234567", tokenProvider.parsePhoneRegistrationToken(token));
        assertFalse(tokenProvider.validateToken(token), "registration tokens must not authenticate sessions");
    }
}
