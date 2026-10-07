package lk.booknplay.service.impl;

import lk.booknplay.dto.request.CustomerLoginRequest;
import lk.booknplay.dto.request.CustomerRegisterRequest;
import lk.booknplay.dto.request.RefreshTokenRequest;
import lk.booknplay.dto.response.AuthResponse;
import lk.booknplay.dto.response.CustomerResponse;
import lk.booknplay.entity.Customer;
import lk.booknplay.entity.RefreshToken;
import lk.booknplay.entity.User;
import lk.booknplay.enums.Role;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.exception.UnauthorizedException;
import lk.booknplay.repository.CustomerRepository;
import lk.booknplay.repository.RefreshTokenRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.security.jwt.JwtProperties;
import lk.booknplay.security.jwt.JwtTokenProvider;
import lk.booknplay.service.AuthService;
import lk.booknplay.service.CustomerService;
import lk.booknplay.util.PhoneNumberUtil;
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
        String loginEmail = resolveLoginEmail(request);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginEmail, request.getPassword())
            );
        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid phone/email or password");
        }

        User user = userRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid phone/email or password"));

        if (user.getRole() != Role.CUSTOMER) {
            throw new UnauthorizedException("Invalid phone/email or password");
        }

        return issueTokensForCustomer(user);
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

        return AuthResponse.builder()
                .accessToken(tokenProvider.generateAccessToken(user, customer.getId()))
                .refreshToken(token.getToken())
                .expiresInMs(jwtProperties.getAccessTokenExpiryMs())
                .role(user.getRole())
                .customer(mapCustomer(customer, user))
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

    @Override
    @Transactional
    public AuthResponse issueTokensForCustomer(User user) {
        Customer customer = customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UnauthorizedException("Customer record not found"));

        String accessToken = tokenProvider.generateAccessToken(user, customer.getId());
        String refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresInMs(jwtProperties.getAccessTokenExpiryMs())
                .role(user.getRole())
                .customer(mapCustomer(customer, user))
                .build();
    }

    private String resolveLoginEmail(CustomerLoginRequest request) {
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            return request.getEmail().trim();
        }
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw new BadRequestException("Email or phone is required");
        }
        String phone = PhoneNumberUtil.normalizeSriLanka(request.getPhone());
        User byUserPhone = userRepository.findByPhone(phone).orElse(null);
        if (byUserPhone != null && byUserPhone.getRole() == Role.CUSTOMER) {
            return byUserPhone.getEmail();
        }
        Customer customer = customerRepository.findByPhone(phone).orElse(null);
        if (customer != null && customer.getUser() != null) {
            return customer.getUser().getEmail();
        }
        throw new UnauthorizedException("Invalid phone/email or password");
    }

    private static CustomerResponse mapCustomer(Customer customer, User user) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(user.getEmail())
                .phone(customer.getPhone())
                .profileImage(customer.getProfileImage())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .build();
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
