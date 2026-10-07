package lk.booknplay.service.impl;

import io.jsonwebtoken.JwtException;
import lk.booknplay.dto.request.CustomerRegisterRequest;
import lk.booknplay.dto.request.CustomerUpdateRequest;
import lk.booknplay.dto.response.CustomerResponse;
import lk.booknplay.entity.Customer;
import lk.booknplay.entity.User;
import lk.booknplay.enums.CustomerStatus;
import lk.booknplay.enums.Role;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.exception.ConflictException;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.exception.UnauthorizedException;
import lk.booknplay.repository.CustomerRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.security.jwt.JwtTokenProvider;
import lk.booknplay.service.CustomerService;
import lk.booknplay.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerRegisterRequest request) {
        String phone = PhoneNumberUtil.normalizeSriLanka(request.getPhone());
        assertPhoneVerifiedByToken(request.getVerificationToken(), phone);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("EMAIL_EXISTS", "Email already in use");
        }
        if (customerRepository.existsByPhone(phone) || userRepository.existsByPhone(phone)) {
            throw new ConflictException("PHONE_EXISTS", "Phone number already in use");
        }

        User user = User.builder()
                .email(request.getEmail())
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
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(phone)
                .status(CustomerStatus.ACTIVE)
                .build();

        Customer saved = customerRepository.save(customer);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(String id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return mapToResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByEmail(String email) {
        Customer customer = customerRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + email));
        return mapToResponse(customer);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(String id, CustomerUpdateRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        String phone = PhoneNumberUtil.normalizeSriLanka(request.getPhone());
        if (!customer.getPhone().equals(phone)
                && (customerRepository.existsByPhone(phone) || userRepository.existsByPhone(phone))) {
            throw new ConflictException("PHONE_EXISTS", "Phone number already in use");
        }

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setPhone(phone);
        User user = customer.getUser();
        if (user != null) {
            boolean phoneChanged = user.getPhone() == null || !phone.equals(user.getPhone());
            user.setPhone(phone);
            if (phoneChanged) {
                user.setPhoneVerified(false);
            }
        }
        if (request.getProfileImage() != null) {
            customer.setProfileImage(request.getProfileImage());
        }

        return mapToResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public void deleteCustomer(String id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        customer.setStatus(CustomerStatus.DELETED);
        customer.getUser().setEnabled(false);
        customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> searchCustomers(String query, Pageable pageable) {
        return customerRepository.searchCustomers(query, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCurrentCustomer(String email) {
        return getCustomerByEmail(email);
    }

    private void assertPhoneVerifiedByToken(String verificationToken, String expectedPhone) {
        String tokenPhone;
        try {
            tokenPhone = jwtTokenProvider.parsePhoneRegistrationToken(verificationToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid or expired phone verification token");
        }
        String normalizedTokenPhone = PhoneNumberUtil.normalizeSriLanka(tokenPhone);
        if (!normalizedTokenPhone.equals(expectedPhone)) {
            throw new BadRequestException("Phone number does not match the verified number");
        }
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getUser().getEmail())
                .phone(customer.getPhone())
                .profileImage(customer.getProfileImage())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}
