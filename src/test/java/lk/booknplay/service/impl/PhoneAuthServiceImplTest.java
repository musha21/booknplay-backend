package lk.booknplay.service.impl;

import lk.booknplay.dto.request.OtpRequestDto;
import lk.booknplay.dto.request.OtpVerifyRequest;
import lk.booknplay.dto.request.PasswordForgotRequest;
import lk.booknplay.dto.request.PasswordResetRequest;
import lk.booknplay.dto.request.PhoneRegisterRequest;
import lk.booknplay.dto.response.AuthResponse;
import lk.booknplay.dto.response.OtpVerifyResponse;
import lk.booknplay.entity.Customer;
import lk.booknplay.entity.User;
import lk.booknplay.enums.CustomerStatus;
import lk.booknplay.enums.OtpPurpose;
import lk.booknplay.enums.Role;
import lk.booknplay.exception.ConflictException;
import lk.booknplay.exception.SmsDeliveryException;
import lk.booknplay.repository.CustomerRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.security.jwt.JwtTokenProvider;
import lk.booknplay.service.AuthService;
import lk.booknplay.service.OtpService;
import lk.booknplay.service.SmsLenzService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhoneAuthServiceImplTest {

    @Mock private OtpService otpService;
    @Mock private SmsLenzService smsLenzService;
    @Mock private UserRepository userRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthService authService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PhoneAuthServiceImpl phoneAuthService;

    @Test
    void requestRegisterOtp_sendsSms() {
        when(userRepository.existsByPhone("+94771234567")).thenReturn(false);
        when(customerRepository.existsByPhone("+94771234567")).thenReturn(false);
        when(otpService.createOtp("+94771234567", OtpPurpose.REGISTER)).thenReturn("482193");

        phoneAuthService.requestRegisterOtp(OtpRequestDto.builder().phone("0771234567").build());

        verify(smsLenzService).sendSms(eq("+94771234567"), contains("482193"));
    }

    @Test
    void requestRegisterOtp_duplicatePhone_conflict() {
        when(userRepository.existsByPhone("+94771234567")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> phoneAuthService.requestRegisterOtp(OtpRequestDto.builder().phone("0771234567").build()));
        verify(otpService, never()).createOtp(anyString(), any());
    }

    @Test
    void requestRegisterOtp_smsFailure_invalidatesOtp() {
        when(userRepository.existsByPhone("+94771234567")).thenReturn(false);
        when(customerRepository.existsByPhone("+94771234567")).thenReturn(false);
        when(otpService.createOtp("+94771234567", OtpPurpose.REGISTER)).thenReturn("482193");
        doThrow(new SmsDeliveryException("fail")).when(smsLenzService).sendSms(anyString(), anyString());

        assertThrows(SmsDeliveryException.class,
                () -> phoneAuthService.requestRegisterOtp(OtpRequestDto.builder().phone("0771234567").build()));

        verify(otpService).invalidateLatest("+94771234567", OtpPurpose.REGISTER);
    }

    @Test
    void verifyRegisterOtp_returnsVerificationToken_notAuth() {
        when(userRepository.existsByPhone("+94771234567")).thenReturn(false);
        when(customerRepository.existsByPhone("+94771234567")).thenReturn(false);
        doNothing().when(otpService).verifyOtp(anyString(), anyString(), eq(OtpPurpose.REGISTER));
        when(jwtTokenProvider.generatePhoneRegistrationToken("+94771234567")).thenReturn("reg-token");

        OtpVerifyResponse response = phoneAuthService.verifyRegisterOtp(
                OtpVerifyRequest.builder().phone("0771234567").otp("482193").build());

        assertTrue(response.getRegistrationRequired());
        assertEquals("reg-token", response.getVerificationToken());
        assertNull(response.getAuth());
        verify(authService, never()).issueTokensForCustomer(any());
    }

    @Test
    void registerWithPhone_requiresPassword() {
        when(jwtTokenProvider.parsePhoneRegistrationToken("tok")).thenReturn("+94771234567");
        when(userRepository.existsByPhone("+94771234567")).thenReturn(false);
        when(customerRepository.existsByPhone("+94771234567")).thenReturn(false);
        when(userRepository.existsByEmail("a@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId("c-1");
            c.getUser().setId("u-1");
            return c;
        });
        AuthResponse auth = AuthResponse.builder().accessToken("access").build();
        when(authService.issueTokensForCustomer(any(User.class))).thenReturn(auth);

        AuthResponse result = phoneAuthService.registerWithPhone(PhoneRegisterRequest.builder()
                .verificationToken("tok")
                .firstName("Mohamed")
                .lastName("Musharaf")
                .email("a@example.com")
                .password("password123")
                .build());

        assertEquals(auth, result);
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertEquals("encoded", captor.getValue().getUser().getPassword());
        assertTrue(captor.getValue().getUser().isPhoneVerified());
        assertEquals(CustomerStatus.ACTIVE, captor.getValue().getStatus());
    }

    @Test
    void forgotPassword_unknownPhone_doesNotSendSms() {
        when(userRepository.findByPhone("+94771234567")).thenReturn(Optional.empty());
        when(customerRepository.findByPhone("+94771234567")).thenReturn(Optional.empty());

        phoneAuthService.forgotPassword(PasswordForgotRequest.builder().phone("0771234567").build());

        verify(otpService, never()).createOtp(anyString(), any());
        verify(smsLenzService, never()).sendSms(anyString(), anyString());
    }

    @Test
    void forgotPassword_knownPhone_sendsResetOtp() {
        User user = User.builder().id("u-1").email("a@example.com").phone("+94771234567").role(Role.CUSTOMER).build();
        when(userRepository.findByPhone("+94771234567")).thenReturn(Optional.of(user));
        when(otpService.createOtp("+94771234567", OtpPurpose.RESET_PASSWORD)).thenReturn("111111");

        phoneAuthService.forgotPassword(PasswordForgotRequest.builder().phone("0771234567").build());

        verify(smsLenzService).sendSms(eq("+94771234567"), contains("111111"));
    }

    @Test
    void resetPassword_updatesHash() {
        User user = User.builder().id("u-1").email("a@example.com").phone("+94771234567").role(Role.CUSTOMER).build();
        doNothing().when(otpService).verifyOtp("+94771234567", "111111", OtpPurpose.RESET_PASSWORD);
        when(userRepository.findByPhone("+94771234567")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass12")).thenReturn("new-hash");

        phoneAuthService.resetPassword(PasswordResetRequest.builder()
                .phone("0771234567")
                .otp("111111")
                .newPassword("newpass12")
                .build());

        assertEquals("new-hash", user.getPassword());
        verify(userRepository).save(user);
    }
}
