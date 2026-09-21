package lk.booknplay.service.impl;

import lk.booknplay.dto.request.CustomerLoginRequest;
import lk.booknplay.dto.request.RefreshTokenRequest;
import lk.booknplay.dto.response.AdminResponse;
import lk.booknplay.dto.response.AuthResponse;
import lk.booknplay.entity.RefreshToken;
import lk.booknplay.entity.User;
import lk.booknplay.enums.Role;
import lk.booknplay.exception.UnauthorizedException;
import lk.booknplay.repository.RefreshTokenRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.security.jwt.JwtProperties;
import lk.booknplay.security.jwt.JwtTokenProvider;
import lk.booknplay.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;

    @Override @Transactional
    public AuthResponse login(CustomerLoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        User user = admin(request.getEmail());
        return response(user, createRefreshToken(user));
    }

    @Override @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (token.isRevoked() || token.getExpiryDate().isBefore(Instant.now()) || token.getUser().getRole() != Role.SUPER_ADMIN) {
            throw new UnauthorizedException("Refresh token is expired or invalid");
        }
        return response(token.getUser(), token.getToken());
    }

    @Override @Transactional
    public void logout(String value) {
        refreshTokenRepository.findByToken(value).ifPresent(token -> { token.setRevoked(true); refreshTokenRepository.save(token); });
    }

    @Override @Transactional(readOnly = true)
    public AdminResponse me(String email) { return toAdmin(admin(email)); }

    private User admin(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UnauthorizedException("Invalid administrator account"));
        if (user.getRole() != Role.SUPER_ADMIN) throw new UnauthorizedException("Not an administrator account");
        return user;
    }

    private AuthResponse response(User user, String refreshToken) {
        return AuthResponse.builder().accessToken(tokenProvider.generateAccessToken(user, null)).refreshToken(refreshToken)
                .expiresInMs(jwtProperties.getAccessTokenExpiryMs()).role(user.getRole()).admin(toAdmin(user)).build();
    }

    private AdminResponse toAdmin(User user) { return AdminResponse.builder().userId(user.getId()).email(user.getEmail()).build(); }

    private String createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder().user(user).token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiryMs())).isRevoked(false).build();
        return refreshTokenRepository.save(token).getToken();
    }
}
