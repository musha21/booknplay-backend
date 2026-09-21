package lk.booknplay.service.impl;

import lk.booknplay.dto.request.CustomerLoginRequest;
import lk.booknplay.dto.request.CustomerRegisterRequest;
import lk.booknplay.dto.request.RefreshTokenRequest;
import lk.booknplay.dto.response.AuthResponse;
import lk.booknplay.dto.response.CustomerResponse;
import lk.booknplay.entity.Customer;
import lk.booknplay.entity.RefreshToken;
import lk.booknplay.entity.User;
import lk.booknplay.exception.UnauthorizedException;
import lk.booknplay.repository.CustomerRepository;
import lk.booknplay.repository.RefreshTokenRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.security.jwt.JwtProperties;
import lk.booknplay.security.jwt.JwtTokenProvider;
import lk.booknplay.service.AuthService;
import lk.booknplay.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(CustomerRegisterRequest request) {
        CustomerResponse customer = customerService.createCustomer(request);
        User user = userRepository.findByEmail(customer.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User creation failed"));

        String accessToken = tokenProvider.generateAccessToken(user, customer.getId());
        String refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresInMs(jwtProperties.getAccessTokenExpiryMs())
                .role(user.getRole())
                .customer(customer)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(CustomerLoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        Customer customer = customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UnauthorizedException("Customer record not found"));

        String accessToken = tokenProvider.generateAccessToken(user, customer.getId());
        String refreshToken = createRefreshToken(user);

        CustomerResponse customerResponse = CustomerResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(user.getEmail())
                .phone(customer.getPhone())
                .profileImage(customer.getProfileImage())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresInMs(jwtProperties.getAccessTokenExpiryMs())
                .role(user.getRole())
                .customer(customerResponse)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (token.isRevoked() || token.getExpiryDate().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }

        User user = token.getUser();
        Customer customer = customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UnauthorizedException("Customer record not found"));

        String newAccessToken = tokenProvider.generateAccessToken(user, customer.getId());

        CustomerResponse customerResponse = CustomerResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(user.getEmail())
                .phone(customer.getPhone())
                .profileImage(customer.getProfileImage())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .build();

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(token.getToken())
                .expiresInMs(jwtProperties.getAccessTokenExpiryMs())
                .role(user.getRole())
                .customer(customerResponse)
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenRepository.findByToken(refreshTokenStr).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private String createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiryMs()))
                .isRevoked(false)
                .build();

        return refreshTokenRepository.save(token).getToken();
    }
}
