package lk.booknplay.service;

import lk.booknplay.dto.request.CustomerRegisterRequest;
import lk.booknplay.dto.response.CustomerResponse;
import lk.booknplay.entity.Customer;
import lk.booknplay.entity.User;
import lk.booknplay.enums.CustomerStatus;
import lk.booknplay.enums.Role;
import lk.booknplay.exception.ConflictException;
import lk.booknplay.repository.CustomerRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private CustomerRegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = CustomerRegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("+94771234567")
                .build();
    }

    @Test
    void createCustomer_Success() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(customerRepository.existsByPhone("+94771234567")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        User user = User.builder()
                .id("u-1")
                .email("john@example.com")
                .password("encodedPassword")
                .role(Role.CUSTOMER)
                .build();

        Customer customer = Customer.builder()
                .id("c-1")
                .user(user)
                .firstName("John")
                .lastName("Doe")
                .phone("+94771234567")
                .status(CustomerStatus.ACTIVE)
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        CustomerResponse response = customerService.createCustomer(registerRequest);

        assertNotNull(response);
        assertEquals("c-1", response.getId());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("+94771234567", response.getPhone());
    }

    @Test
    void createCustomer_DuplicateEmail_ThrowsConflictException() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> customerService.createCustomer(registerRequest));
    }
}
