package lk.booknplay.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lk.booknplay.config.OtpProperties;
import lk.booknplay.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    public static final String PURPOSE_PHONE_REGISTRATION = "PHONE_REGISTRATION";

    private final JwtProperties jwtProperties;
    private final OtpProperties otpProperties;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    public String generateAccessToken(User user, String customerId) {
        return generateAccessToken(user, customerId, null);
    }

    public String generateAccessToken(User user, String customerId, String businessId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiryMs());

        var builder = Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256);

        if (customerId != null) {
            builder.claim("customerId", customerId);
        }
        if (businessId != null) {
            builder.claim("businessId", businessId);
        }

        return builder.compact();
    }

    /**
     * Short-lived token proving a phone number passed OTP verification and may complete registration.
     * Not an access token — must not be accepted by {@link lk.booknplay.security.filter.JwtAuthenticationFilter}.
     */
    public String generatePhoneRegistrationToken(String normalizedPhone) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + otpProperties.getRegistrationTokenExpiryMs());
        return Jwts.builder()
                .setSubject(normalizedPhone)
                .claim("purpose", PURPOSE_PHONE_REGISTRATION)
                .claim("phone", normalizedPhone)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * @return normalized phone from a valid phone-registration token
     * @throws JwtException if invalid, expired, or wrong purpose
     */
    public String parsePhoneRegistrationToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        String purpose = claims.get("purpose", String.class);
        if (!PURPOSE_PHONE_REGISTRATION.equals(purpose)) {
            throw new JwtException("Invalid verification token purpose");
        }
        String phone = claims.get("phone", String.class);
        if (phone == null || phone.isBlank()) {
            phone = claims.getSubject();
        }
        if (phone == null || phone.isBlank()) {
            throw new JwtException("Invalid verification token");
        }
        return phone;
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
            // Phone-registration tokens must not authenticate API sessions.
            if (PURPOSE_PHONE_REGISTRATION.equals(claims.get("purpose", String.class))) {
                return false;
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }
}
