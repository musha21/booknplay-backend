package lk.booknplay.service.impl;

import io.jsonwebtoken.JwtException;
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
import lk.booknplay.exception.UnauthorizedException;
import lk.booknplay.repository.CustomerRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.security.jwt.JwtTokenProvider;
import lk.booknplay.service.AuthService;
import lk.booknplay.service.OtpService;
import lk.booknplay.service.PhoneAuthService;
import lk.booknplay.service.SmsLenzService;
import lk.booknplay.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PhoneAuthServiceImpl implements PhoneAuthService {

    private static final Logger log = LoggerFactory.getLogger(PhoneAuthServiceImpl.class);

    private static final String OTP_SMS_TEMPLATE =
            "Your BooknPlay verification code is %s. It expires in 5 minutes.";

    private final OtpService otpService;
    private final SmsLenzService smsLenzService;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void requestRegisterOtp(OtpRequestDto request) {
        String phone = PhoneNumberUtil.normalizeSriLanka(request.getPhone());
        if (userRepository.existsByPhone(phone) || customerRepository.existsByPhone(phone)) {
            throw new ConflictException("PHONE_EXISTS", "Phone number already registered");
        }
        sendOtp(phone, OtpPurpose.REGISTER);
    }

    @Override
    @Transactional
    public OtpVerifyResponse verifyRegisterOtp(OtpVerifyRequest request) {
        String phone = PhoneNumberUtil.normalizeSriLanka(request.getPhone());
        if (userRepository.existsByPhone(phone) || customerRepository.existsByPhone(phone)) {
            throw new ConflictException("PHONE_EXISTS", "Phone number already registered");
        }
        otpService.verifyOtp(phone, request.getOtp(), OtpPurpose.REGISTER);

        String verificationToken = jwtTokenProvider.generatePhoneRegistrationToken(phone);
        return OtpVerifyResponse.builder()
                .verified(true)
                .registrationRequired(true)
                .verificationToken(verificationToken)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse registerWithPhone(PhoneRegisterRequest request) {
        String phone;
        try {
            phone = jwtTokenProvider.parsePhoneRegistrationToken(request.getVerificationToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid or expired verification token");
        }

        phone = PhoneNumberUtil.normalizeSriLanka(phone);

        if (userRepository.existsByPhone(phone) || customerRepository.existsByPhone(phone)) {
            throw new ConflictException("PHONE_EXISTS", "Phone number already registered");
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(
                    "EMAIL_EXISTS",
                    "Email already in use. Sign in with email/password, then link this phone number from your account."
            );
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(phone)
                .phoneVerified(true)
                .emailVerified(false)
                .role(Role.CUSTOMER)
                .isEnabled(true)
                .isLocked(false)
                .build();

        Customer customer = Customer.builder()
                .user(user)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phone(phone)
                .status(CustomerStatus.ACTIVE)
                .build();

        Customer saved = customerRepository.save(customer);
        return authService.issueTokensForCustomer(saved.getUser());
    }

    @Override
    @Transactional
    public void requestLinkPhoneOtp(String userEmail, OtpRequestDto request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        String phone = PhoneNumberUtil.normalizeSriLanka(request.getPhone());
        assertPhoneAvailableForUser(phone, user.getId());
        sendOtp(phone, OtpPurpose.PHONE_VERIFICATION);
    }

    @Override
    @Transactional
    public AuthResponse verifyLinkPhoneOtp(String userEmail, OtpVerifyRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        String phone = PhoneNumberUtil.normalizeSriLanka(request.getPhone());
        otpService.verifyOtp(phone, request.getOtp(), OtpPurpose.PHONE_VERIFICATION);
        assertPhoneAvailableForUser(phone, user.getId());

        user.setPhone(phone);
        user.setPhoneVerified(true);
        userRepository.save(user);

        Customer customer = customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UnauthorizedException("Customer record not found"));
        customer.setPhone(phone);
        customerRepository.save(customer);

        return authService.issueTokensForCustomer(user);
    }

    @Override
    @Transactional
    public void forgotPassword(PasswordForgotRequest request) {
        String phone;
        try {
            phone = PhoneNumberUtil.normalizeSriLanka(request.getPhone());
        } catch (Exception ex) {
            // Generic success — do not reveal invalid phone format differences beyond 400 from util
            throw ex;
        }

        Optional<User> userOpt = findCustomerUserByPhone(phone);
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for unknown phone ending {}", mask(phone));
            return;
        }

        sendOtp(phone, OtpPurpose.RESET_PASSWORD);
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        String phone = PhoneNumberUtil.normalizeSriLanka(request.getPhone());
        otpService.verifyOtp(phone, request.getOtp(), OtpPurpose.RESET_PASSWORD);

        User user = findCustomerUserByPhone(phone)
                .orElseThrow(() -> new UnauthorizedException("Invalid verification code"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        if (user.getPhone() == null) {
            user.setPhone(phone);
        }
        user.setPhoneVerified(true);
        userRepository.save(user);
    }

    private void sendOtp(String phone, OtpPurpose purpose) {
        String otp = otpService.createOtp(phone, purpose);
        try {
            smsLenzService.sendSms(phone, String.format(OTP_SMS_TEMPLATE, otp));
        } catch (SmsDeliveryException ex) {
            otpService.invalidateLatest(phone, purpose);
            throw ex;
        }
    }

    private Optional<User> findCustomerUserByPhone(String phone) {
        Optional<User> byUserPhone = userRepository.findByPhone(phone);
        if (byUserPhone.isPresent()) {
            return byUserPhone.filter(u -> u.getRole() == Role.CUSTOMER);
        }

        return customerRepository.findByPhone(phone).map(customer -> {
            User user = customer.getUser();
            if (user.getPhone() == null) {
                user.setPhone(phone);
                userRepository.save(user);
            }
            return user;
        });
    }

    private void assertPhoneAvailableForUser(String phone, String userId) {
        userRepository.findByPhone(phone).ifPresent(other -> {
            if (!other.getId().equals(userId)) {
                throw new ConflictException("PHONE_EXISTS", "Phone number already registered");
            }
        });
        customerRepository.findByPhone(phone).ifPresent(other -> {
            if (other.getUser() == null || !other.getUser().getId().equals(userId)) {
                throw new ConflictException("PHONE_EXISTS", "Phone number already registered");
            }
        });
    }

    private static String mask(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(phone.length() - 4);
    }
}
