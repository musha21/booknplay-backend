package lk.booknplay.service;

import lk.booknplay.entity.Business;
import lk.booknplay.entity.BusinessSubscription;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.BillingInterval;
import lk.booknplay.enums.PlanCode;
import lk.booknplay.enums.SubscriptionStatus;
import lk.booknplay.enums.VenueStatus;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionVenueArchiveTest {

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
    void persistIfExpired_ArchivesLiveVenuesWhenTrialEnds() {
        OwnerSubscriptionServiceImpl service = service();
        Business business = Business.builder().id("biz-1").ownerId("owner-1").build();
        BusinessSubscription subscription = BusinessSubscription.builder()
                .businessId("biz-1")
                .planCode(PlanCode.TRIAL)
                .status(SubscriptionStatus.TRIALING)
                .trialStartsAt(LocalDateTime.now().minusDays(100))
                .trialEndsAt(LocalDateTime.now().minusDays(1))
                .build();
        Venue live = Venue.builder()
                .id("venue-1")
                .name("Court A")
                .status(VenueStatus.APPROVED)
                .business(business)
                .archivedBySubscription(false)
                .build();

        when(subscriptionRepository.findByBusinessId("biz-1")).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(BusinessSubscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(venueRepository.findByBusinessIdAndStatusIn(
                eq("biz-1"), eq(List.of(VenueStatus.ACTIVE, VenueStatus.APPROVED))))
                .thenReturn(List.of(live));
        when(venueRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        BusinessSubscription result = service.ensureSubscription(business);

        assertEquals(SubscriptionStatus.EXPIRED, result.getStatus());
        assertEquals(VenueStatus.DELETED, live.getStatus());
        assertEquals(VenueStatus.APPROVED, live.getArchivedFromStatus());
        assertTrue(live.isArchivedBySubscription());
        verify(venueRepository).saveAll(anyList());
    }

    @Test
    void activatePaidPlan_RestoresOnlySubscriptionArchivedVenues() {
        OwnerSubscriptionServiceImpl service = service();
        BusinessSubscription subscription = BusinessSubscription.builder()
                .businessId("biz-1")
                .planCode(PlanCode.TRIAL)
                .status(SubscriptionStatus.EXPIRED)
                .trialEndsAt(LocalDateTime.now().minusDays(1))
                .build();
        Venue subscriptionArchived = Venue.builder()
                .id("venue-sub")
                .name("Was Live")
                .status(VenueStatus.DELETED)
                .archivedFromStatus(VenueStatus.ACTIVE)
                .archivedBySubscription(true)
                .build();
        Venue ownerArchived = Venue.builder()
                .id("venue-owner")
                .name("Owner Archived")
                .status(VenueStatus.DELETED)
                .archivedFromStatus(VenueStatus.APPROVED)
                .archivedBySubscription(false)
                .build();

        when(subscriptionRepository.findByBusinessId("biz-1")).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(BusinessSubscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(businessRepository.findById("biz-1")).thenReturn(Optional.of(
                Business.builder().id("biz-1").ownerId("owner-1").build()));
        when(planRepository.findByCode(PlanCode.STARTER)).thenReturn(Optional.empty());
        when(venueRepository.findByBusinessIdAndStatusAndArchivedBySubscriptionTrue(
                "biz-1", VenueStatus.DELETED))
                .thenReturn(List.of(subscriptionArchived));
        when(venueRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        service.activatePaidPlan("biz-1", PlanCode.STARTER, BillingInterval.MONTHLY);

        assertEquals(VenueStatus.ACTIVE, subscriptionArchived.getStatus());
        assertFalse(subscriptionArchived.isArchivedBySubscription());
        assertNull(subscriptionArchived.getArchivedFromStatus());
        assertEquals(VenueStatus.DELETED, ownerArchived.getStatus());
        assertFalse(ownerArchived.isArchivedBySubscription());
        verify(venueRepository).saveAll(List.of(subscriptionArchived));
    }

    @Test
    void expireDueSubscriptionsAndArchiveVenues_ExpiresAndArchives() {
        OwnerSubscriptionServiceImpl service = service();
        BusinessSubscription due = BusinessSubscription.builder()
                .businessId("biz-1")
                .planCode(PlanCode.STARTER)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDateTime.now().minusMonths(2))
                .currentPeriodEnd(LocalDateTime.now().minusDays(1))
                .build();
        BusinessSubscription stillActive = BusinessSubscription.builder()
                .businessId("biz-2")
                .planCode(PlanCode.GROWTH)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDateTime.now().minusDays(1))
                .currentPeriodEnd(LocalDateTime.now().plusDays(20))
                .build();
        Venue live = Venue.builder()
                .id("venue-1")
                .status(VenueStatus.ACTIVE)
                .archivedBySubscription(false)
                .build();

        when(subscriptionRepository.findByStatusIn(anyList())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            java.util.Collection<SubscriptionStatus> statuses = inv.getArgument(0);
            if (statuses.contains(SubscriptionStatus.EXPIRED)) {
                return List.of();
            }
            return List.of(due, stillActive);
        });
        when(subscriptionRepository.save(any(BusinessSubscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(venueRepository.findByBusinessIdAndStatusIn(
                eq("biz-1"), eq(List.of(VenueStatus.ACTIVE, VenueStatus.APPROVED))))
                .thenReturn(List.of(live));
        when(venueRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        int expired = service.expireDueSubscriptionsAndArchiveVenues();

        assertEquals(1, expired);
        assertEquals(SubscriptionStatus.EXPIRED, due.getStatus());
        assertEquals(SubscriptionStatus.ACTIVE, stillActive.getStatus());
        assertEquals(VenueStatus.DELETED, live.getStatus());
        assertTrue(live.isArchivedBySubscription());
        verify(venueRepository, never()).findByBusinessIdAndStatusIn(
                eq("biz-2"), anyList());
    }

    @Test
    void expireDueSubscriptionsAndArchiveVenues_BackfillsAlreadyExpiredLiveVenues() {
        OwnerSubscriptionServiceImpl service = service();
        BusinessSubscription alreadyExpired = BusinessSubscription.builder()
                .businessId("biz-old")
                .planCode(PlanCode.TRIAL)
                .status(SubscriptionStatus.EXPIRED)
                .trialEndsAt(LocalDateTime.now().minusDays(30))
                .build();
        Venue leftoverLive = Venue.builder()
                .id("venue-leftover")
                .status(VenueStatus.APPROVED)
                .archivedBySubscription(false)
                .build();

        when(subscriptionRepository.findByStatusIn(anyList())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            java.util.Collection<SubscriptionStatus> statuses = inv.getArgument(0);
            if (statuses.contains(SubscriptionStatus.EXPIRED)) {
                return List.of(alreadyExpired);
            }
            return List.of();
        });
        when(venueRepository.findByBusinessIdAndStatusIn(
                eq("biz-old"), eq(List.of(VenueStatus.ACTIVE, VenueStatus.APPROVED))))
                .thenReturn(List.of(leftoverLive));
        when(venueRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        int affected = service.expireDueSubscriptionsAndArchiveVenues();

        assertEquals(1, affected);
        assertEquals(VenueStatus.DELETED, leftoverLive.getStatus());
        assertEquals(VenueStatus.APPROVED, leftoverLive.getArchivedFromStatus());
        assertTrue(leftoverLive.isArchivedBySubscription());
    }

    @Test
    void archiveVenuesForExpiredSubscription_SkipsAlreadyDeleted() {
        OwnerSubscriptionServiceImpl service = service();
        when(venueRepository.findByBusinessIdAndStatusIn(
                eq("biz-1"), eq(List.of(VenueStatus.ACTIVE, VenueStatus.APPROVED))))
                .thenReturn(List.of());

        assertEquals(0, service.archiveVenuesForExpiredSubscription("biz-1"));

        verify(venueRepository, never()).saveAll(anyList());
    }
}
