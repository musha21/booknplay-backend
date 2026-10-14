package lk.booknplay.service;

import lk.booknplay.dto.response.SubscriptionResponse;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.BusinessSubscription;
import lk.booknplay.entity.SubscriptionPlan;
import lk.booknplay.enums.PlanCode;
import lk.booknplay.enums.SubscriptionStatus;
import lk.booknplay.repository.BusinessRepository;
import lk.booknplay.repository.BusinessStaffRepository;
import lk.booknplay.repository.BusinessSubscriptionRepository;
import lk.booknplay.repository.SubscriptionPaymentRepository;
import lk.booknplay.repository.SubscriptionPlanRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.repository.VenueRepository;
import lk.booknplay.service.impl.OwnerSubscriptionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerSubscriptionPromotionsLimitsTest {

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

    private OwnerSubscriptionServiceImpl service() {
        return new OwnerSubscriptionServiceImpl(
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
    }

    @Test
    void getSubscription_MapsPromotionsEnabledFromPlan() {
        OwnerSubscriptionServiceImpl service = service();
        Business business = Business.builder().id("biz-1").ownerId("owner-1").build();
        BusinessSubscription subscription = BusinessSubscription.builder()
                .businessId("biz-1")
                .planCode(PlanCode.GROWTH)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDateTime.now().minusDays(1))
                .currentPeriodEnd(LocalDateTime.now().plusDays(30))
                .build();

        when(ownerAccessService.requireBusiness("owner@example.com")).thenReturn(business);
        when(subscriptionRepository.findByBusinessId("biz-1")).thenReturn(Optional.of(subscription));
        when(planRepository.findByCode(PlanCode.GROWTH)).thenReturn(Optional.of(
                SubscriptionPlan.builder()
                        .code(PlanCode.GROWTH)
                        .name("Growth")
                        .priceMonthly(new BigDecimal("9990"))
                        .priceYearly(new BigDecimal("99900"))
                        .currency("LKR")
                        .promotionsEnabled(true)
                        .reportsEnabled(true)
                        .calendarEnabled(true)
                        .walkInEnabled(true)
                        .earningsEnabled(true)
                        .build()
        ));
        when(venueRepository.findByBusinessIdAndStatusNot("biz-1", lk.booknplay.enums.VenueStatus.DELETED))
                .thenReturn(List.of());
        when(businessStaffRepository.countByBusinessIdAndActiveTrue("biz-1")).thenReturn(0L);

        SubscriptionResponse response = service.getSubscription("owner@example.com");

        assertTrue(response.getLimits().isPromotionsEnabled());
    }

    @Test
    void getSubscription_StarterPromotionsDisabled() {
        OwnerSubscriptionServiceImpl service = service();
        Business business = Business.builder().id("biz-2").ownerId("owner-2").build();
        BusinessSubscription subscription = BusinessSubscription.builder()
                .businessId("biz-2")
                .planCode(PlanCode.STARTER)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDateTime.now().minusDays(1))
                .currentPeriodEnd(LocalDateTime.now().plusDays(30))
                .build();

        when(ownerAccessService.requireBusiness("starter@example.com")).thenReturn(business);
        when(subscriptionRepository.findByBusinessId("biz-2")).thenReturn(Optional.of(subscription));
        when(planRepository.findByCode(PlanCode.STARTER)).thenReturn(Optional.of(
                SubscriptionPlan.builder()
                        .code(PlanCode.STARTER)
                        .name("Starter")
                        .priceMonthly(new BigDecimal("4990"))
                        .priceYearly(new BigDecimal("49900"))
                        .currency("LKR")
                        .promotionsEnabled(false)
                        .reportsEnabled(false)
                        .calendarEnabled(true)
                        .walkInEnabled(true)
                        .earningsEnabled(true)
                        .build()
        ));
        when(venueRepository.findByBusinessIdAndStatusNot("biz-2", lk.booknplay.enums.VenueStatus.DELETED))
                .thenReturn(List.of());
        when(businessStaffRepository.countByBusinessIdAndActiveTrue("biz-2")).thenReturn(0L);

        SubscriptionResponse response = service.getSubscription("starter@example.com");

        assertFalse(response.getLimits().isPromotionsEnabled());
    }
}
