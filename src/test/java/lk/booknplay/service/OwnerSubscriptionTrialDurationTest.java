package lk.booknplay.service;

import lk.booknplay.entity.Business;
import lk.booknplay.entity.BusinessSubscription;
import lk.booknplay.entity.SubscriptionPlan;
import lk.booknplay.enums.PlanCode;
import lk.booknplay.repository.BusinessRepository;
import lk.booknplay.repository.BusinessStaffRepository;
import lk.booknplay.repository.BusinessSubscriptionRepository;
import lk.booknplay.repository.SubscriptionPaymentRepository;
import lk.booknplay.repository.SubscriptionPlanRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.repository.VenueRepository;
import lk.booknplay.service.impl.OwnerSubscriptionServiceImpl;
import lk.booknplay.util.SubscriptionAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerSubscriptionTrialDurationTest {

    @Mock
    private BusinessSubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionPlanRepository planRepository;
    @Mock
    private SubscriptionPaymentRepository paymentRepository;
    @Mock
    private BusinessRepository businessRepository;
    @Mock
    private OwnerAccessService ownerAccessService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private BusinessStaffRepository businessStaffRepository;
    @Mock
    private PayHereCheckoutStore checkoutStore;

    @Test
    void createTrialForBusiness_UsesConfiguredTrialDurationDays() {
        OwnerSubscriptionServiceImpl service = new OwnerSubscriptionServiceImpl(
                subscriptionRepository,
                planRepository,
                paymentRepository,
                businessRepository,
                ownerAccessService,
                userRepository,
                venueRepository,
                businessStaffRepository,
                checkoutStore,
                "MOCK",
                "http://localhost:8080",
                "",
                "",
                "http://localhost:5173/owner/billing/return",
                "http://localhost:5173/owner/billing/return"
        );

        when(planRepository.findByCode(PlanCode.TRIAL)).thenReturn(Optional.of(
                SubscriptionPlan.builder()
                        .code(PlanCode.TRIAL)
                        .name("Free trial")
                        .priceMonthly(BigDecimal.ZERO)
                        .priceYearly(BigDecimal.ZERO)
                        .trialDurationDays(30)
                        .commissionPercent(BigDecimal.ZERO)
                        .build()
        ));
        when(subscriptionRepository.save(any(BusinessSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessRepository.findById("biz-1")).thenReturn(Optional.of(
                Business.builder().id("biz-1").commissionPercent(BigDecimal.ZERO).build()
        ));
        when(businessRepository.save(any(Business.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now().minusSeconds(2);
        service.createTrialForBusiness(Business.builder().id("biz-1").build());
        LocalDateTime after = LocalDateTime.now().plusSeconds(2);

        ArgumentCaptor<BusinessSubscription> captor = ArgumentCaptor.forClass(BusinessSubscription.class);
        verify(subscriptionRepository).save(captor.capture());
        BusinessSubscription saved = captor.getValue();

        long days = ChronoUnit.DAYS.between(saved.getTrialStartsAt().toLocalDate(), saved.getTrialEndsAt().toLocalDate());
        assertEquals(30, days);
        assertTrue(!saved.getTrialStartsAt().isBefore(before) && !saved.getTrialStartsAt().isAfter(after));
    }

    @Test
    void resolveTrialDurationDays_FallsBackToDefault() {
        assertEquals(90, SubscriptionAccess.resolveTrialDurationDays(null));
        assertEquals(90, SubscriptionAccess.resolveTrialDurationDays(0));
        assertEquals(90, SubscriptionAccess.resolveTrialDurationDays(400));
        assertEquals(45, SubscriptionAccess.resolveTrialDurationDays(45));
    }
}
