package lk.booknplay.service.impl;

import lk.booknplay.dto.request.CustomerLoginRequest;
import lk.booknplay.dto.response.AuthResponse;
import lk.booknplay.entity.Customer;
import lk.booknplay.entity.RefreshToken;
import lk.booknplay.entity.User;
import lk.booknplay.enums.Role;
import lk.booknplay.repository.CustomerRepository;
import lk.booknplay.repository.RefreshTokenRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.security.jwt.JwtProperties;
import lk.booknplay.security.jwt.JwtTokenProvider;
import lk.booknplay.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplPhoneLoginTest {

    @Mock private CustomerService customerService;
    @Mock private CustomerRepository customerRepository;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private JwtProperties jwtProperties;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void login_withPhoneAndPassword_issuesTokens() {
        User user = User.builder()
                .id("u-1")
                .email("john@example.com")
                .phone("+94771234567")
                .role(Role.CUSTOMER)
                .build();
        Customer customer = Customer.builder()
                .id("c-1")
                .user(user)
                .firstName("John")
                .lastName("Doe")
                .phone("+94771234567")
                .build();

        when(userRepository.findByPhone("+94771234567")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUserId("u-1")).thenReturn(Optional.of(customer));
        when(tokenProvider.generateAccessToken(user, "c-1")).thenReturn("access");
        when(jwtProperties.getRefreshTokenExpiryMs()).thenReturn(604800000L);
        when(jwtProperties.getAccessTokenExpiryMs()).thenReturn(900000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> {
            RefreshToken t = inv.getArgument(0);
            t.setToken("refresh");
            return t;
        });

        AuthResponse response = authService.login(CustomerLoginRequest.builder()
                .phone("0771234567")
                .password("password123")
                .build());

        assertEquals("access", response.getAccessToken());
        assertEquals("refresh", response.getRefreshToken());
        assertEquals("c-1", response.getCustomer().getId());
        verify(authenticationManager).authenticate(argThat(token ->
                "john@example.com".equals(token.getPrincipal())
                        && "password123".equals(token.getCredentials())
        ));
    }
}
