package lk.booknplay.service;

import lk.booknplay.dto.request.AdminCreateBusinessRequest;
import lk.booknplay.dto.response.AdminCreateBusinessResponse;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.BusinessSubscription;
import lk.booknplay.entity.User;
import lk.booknplay.enums.PlanCode;
import lk.booknplay.enums.Role;
import lk.booknplay.enums.SubscriptionStatus;
import lk.booknplay.exception.ConflictException;
import lk.booknplay.repository.AdminAuditLogRepository;
import lk.booknplay.repository.BookingRepository;
import lk.booknplay.repository.BusinessRepository;
import lk.booknplay.repository.BusinessSubscriptionRepository;
import lk.booknplay.repository.CustomerRepository;
import lk.booknplay.repository.PaymentRepository;
import lk.booknplay.repository.SubscriptionPlanRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.repository.VenueRepository;
import lk.booknplay.service.impl.AdminPlatformServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCreateBusinessTest {

    @Mock private CustomerRepository customers;
    @Mock private BusinessRepository businesses;
    @Mock private VenueRepository venues;
    @Mock private BookingRepository bookings;
    @Mock private PaymentRepository payments;
    @Mock private UserRepository users;
    @Mock private AdminAuditLogRepository auditLogs;
    @Mock private BusinessSubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionPlanRepository planRepository;
    @Mock private OwnerSubscriptionService ownerSubscriptionService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminPlatformServiceImpl service;

    @Test
    void createBusiness_ProvisionsOwnerBusinessAndTrial() {
        AdminCreateBusinessRequest request = AdminCreateBusinessRequest.builder()
                .businessName("Kandy Courts")
                .ownerName("Amal Perera")
                .email("Owner@Example.com")
                .temporaryPassword("TempPass1!")
                .contactPhone("+94771234567")
                .address("Kandy")
                .build();

        when(users.existsByEmail("owner@example.com")).thenReturn(false);
        when(passwordEncoder.encode("TempPass1!")).thenReturn("encoded-pass");
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-1");
            return u;
        });
        when(businesses.save(any(Business.class))).thenAnswer(inv -> {
            Business b = inv.getArgument(0);
            b.setId("biz-1");
            return b;
        });
        when(ownerSubscriptionService.createTrialForBusiness(any(Business.class)))
                .thenReturn(BusinessSubscription.builder()
                        .businessId("biz-1")
                        .planCode(PlanCode.TRIAL)
                        .status(SubscriptionStatus.TRIALING)
                        .trialEndsAt(LocalDateTime.now().plusDays(90))
                        .build());
        when(users.findById("user-1")).thenReturn(Optional.of(User.builder()
                .id("user-1")
                .email("owner@example.com")
                .role(Role.BUSINESS_OWNER)
                .isEnabled(true)
                .isLocked(false)
                .build()));
        when(subscriptionRepository.findByBusinessId("biz-1")).thenReturn(Optional.of(
                BusinessSubscription.builder()
                        .businessId("biz-1")
                        .planCode(PlanCode.TRIAL)
                        .status(SubscriptionStatus.TRIALING)
                        .trialEndsAt(LocalDateTime.now().plusDays(90))
                        .build()));
        when(venues.findByBusinessId("biz-1")).thenReturn(List.of());
        when(auditLogs.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AdminCreateBusinessResponse response = service.createBusiness(request, "admin@booknplay.lk");

        assertEquals("biz-1", response.getId());
        assertEquals("Kandy Courts", response.getName());
        assertEquals("owner@example.com", response.getOwnerEmail());
        assertEquals("TempPass1!", response.getTemporaryPassword());
        assertEquals("TRIAL", response.getPlanCode());
        assertEquals("TRIALING", response.getSubscriptionStatus());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).save(userCaptor.capture());
        assertEquals("encoded-pass", userCaptor.getValue().getPassword());
        assertEquals(Role.BUSINESS_OWNER, userCaptor.getValue().getRole());

        ArgumentCaptor<Business> businessCaptor = ArgumentCaptor.forClass(Business.class);
        verify(businesses).save(businessCaptor.capture());
        assertEquals("user-1", businessCaptor.getValue().getOwnerId());
        assertEquals(BigDecimal.ZERO, businessCaptor.getValue().getCommissionPercent());

        verify(ownerSubscriptionService).createTrialForBusiness(any(Business.class));
        verify(auditLogs).save(any());
    }

    @Test
    void createBusiness_RejectsDuplicateEmail() {
        when(users.existsByEmail("taken@example.com")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () ->
                service.createBusiness(AdminCreateBusinessRequest.builder()
                        .businessName("Dup")
                        .ownerName("Owner")
                        .email("taken@example.com")
                        .temporaryPassword("TempPass1!")
                        .contactPhone("+94771234567")
                        .address("Colombo")
                        .build(), "admin@booknplay.lk"));

        assertEquals("EMAIL_EXISTS", ex.getCode());
        assertTrue(ex.getMessage().toLowerCase().contains("email"));
        verify(users, never()).save(any());
        verify(businesses, never()).save(any());
        verify(ownerSubscriptionService, never()).createTrialForBusiness(any());
    }
}
