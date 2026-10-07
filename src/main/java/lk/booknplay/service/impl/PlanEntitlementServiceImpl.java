package lk.booknplay.service.impl;

import lk.booknplay.dto.response.SubscriptionResponse;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.BusinessSubscription;
import lk.booknplay.entity.SubscriptionPlan;
import lk.booknplay.enums.CourtStatus;
import lk.booknplay.enums.PlanCode;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.exception.ForbiddenException;
import lk.booknplay.repository.BusinessStaffRepository;
import lk.booknplay.repository.CourtRepository;
import lk.booknplay.repository.SubscriptionPlanRepository;
import lk.booknplay.repository.VenueRepository;
import lk.booknplay.service.OwnerSubscriptionService;
import lk.booknplay.service.PlanEntitlementService;
import lk.booknplay.util.SubscriptionAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanEntitlementServiceImpl implements PlanEntitlementService {

    public static final String PLAN_LIMIT = "PLAN_LIMIT";

    private final OwnerSubscriptionService ownerSubscriptionService;
    private final SubscriptionPlanRepository planRepository;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final BusinessStaffRepository businessStaffRepository;

    @Override
    public SubscriptionPlan resolvePlan(Business business) {
        BusinessSubscription subscription = ownerSubscriptionService.ensureSubscription(business);
        LocalDateTime now = LocalDateTime.now();
        SubscriptionAccess.refreshExpiredInMemory(subscription, now);
        PlanCode code = subscription.getPlanCode() != null ? subscription.getPlanCode() : PlanCode.TRIAL;
        return planRepository.findByCode(code)
                .orElseGet(() -> defaultPlan(code));
    }

    @Override
    public SubscriptionResponse.PlanLimits limitsOf(SubscriptionPlan plan) {
        if (plan == null) {
            return SubscriptionResponse.PlanLimits.builder()
                    .calendarEnabled(true)
                    .walkInEnabled(true)
                    .earningsEnabled(true)
                    .reportsEnabled(true)
                    .advancedReportsEnabled(true)
                    .promotionsEnabled(true)
                    .build();
        }
        return SubscriptionResponse.PlanLimits.builder()
                .maxVenues(plan.getMaxVenues())
                .maxCourtsPerVenue(plan.getMaxCourtsPerVenue())
                .maxStaff(plan.getMaxStaff())
                .calendarEnabled(plan.isCalendarEnabled())
                .walkInEnabled(plan.isWalkInEnabled())
                .earningsEnabled(plan.isEarningsEnabled())
                .reportsEnabled(plan.isReportsEnabled())
                .advancedReportsEnabled(plan.isAdvancedReportsEnabled())
                .promotionsEnabled(plan.isPromotionsEnabled())
                .build();
    }

    @Override
    public long countActiveVenues(String businessId) {
        return venueRepository.findByBusinessIdAndStatusNot(businessId, VenueStatus.DELETED).size();
    }

    @Override
    public long countActiveCourts(String venueId) {
        return courtRepository.findByVenueId(venueId).stream()
                .filter(c -> c.getStatus() != CourtStatus.DELETED)
                .count();
    }

    @Override
    public void assertCanCreateVenue(Business business) {
        SubscriptionPlan plan = resolvePlan(business);
        Integer max = plan.getMaxVenues();
        if (max == null) return;
        long used = countActiveVenues(business.getId());
        if (used >= max) {
            throw new ForbiddenException(PLAN_LIMIT,
                    "Your " + plan.getName() + " plan allows " + max + " venue"
                            + (max == 1 ? "" : "s") + ". Upgrade to add more.");
        }
    }

    @Override
    public void assertCanCreateCourt(Business business, String venueId) {
        SubscriptionPlan plan = resolvePlan(business);
        Integer max = plan.getMaxCourtsPerVenue();
        if (max == null) return;
        long used = countActiveCourts(venueId);
        if (used >= max) {
            throw new ForbiddenException(PLAN_LIMIT,
                    "Your " + plan.getName() + " plan allows " + max + " court"
                            + (max == 1 ? "" : "s") + " per venue. Upgrade to add more.");
        }
    }

    @Override
    public void assertCanAddStaff(Business business) {
        SubscriptionPlan plan = resolvePlan(business);
        Integer max = plan.getMaxStaff();
        if (max == null) return;
        long used = businessStaffRepository.countByBusinessIdAndActiveTrue(business.getId());
        if (used >= max) {
            throw new ForbiddenException(PLAN_LIMIT,
                    "Your " + plan.getName() + " plan allows " + max + " staff seat"
                            + (max == 1 ? "" : "s") + ". Upgrade to add more.");
        }
    }

    @Override
    public void assertCalendarAccess(Business business) {
        SubscriptionPlan plan = resolvePlan(business);
        if (!plan.isCalendarEnabled()) {
            throw new ForbiddenException(PLAN_LIMIT,
                    "Calendar is not included in your " + plan.getName() + " plan. Upgrade to unlock it.");
        }
    }

    @Override
    public void assertWalkIn(Business business) {
        assertCalendarAccess(business);
        SubscriptionPlan plan = resolvePlan(business);
        if (!plan.isWalkInEnabled()) {
            throw new ForbiddenException(PLAN_LIMIT,
                    "Walk-ins are not included in your " + plan.getName() + " plan. Upgrade to unlock them.");
        }
    }

    @Override
    public void assertEarnings(Business business) {
        SubscriptionPlan plan = resolvePlan(business);
        if (!plan.isEarningsEnabled()) {
            throw new ForbiddenException(PLAN_LIMIT,
                    "Earnings are not included in your " + plan.getName() + " plan. Upgrade to unlock them.");
        }
    }

    @Override
    public void assertReports(Business business) {
        SubscriptionPlan plan = resolvePlan(business);
        if (!plan.isReportsEnabled()) {
            throw new ForbiddenException(PLAN_LIMIT,
                    "Reports are not included in your " + plan.getName() + " plan. Upgrade to unlock them.");
        }
    }

    @Override
    public void assertAdvancedReports(Business business) {
        assertReports(business);
        SubscriptionPlan plan = resolvePlan(business);
        if (!plan.isAdvancedReportsEnabled()) {
            throw new ForbiddenException(PLAN_LIMIT,
                    "Advanced reports are not included in your " + plan.getName() + " plan. Upgrade to unlock them.");
        }
    }

    @Override
    public void assertPromotions(Business business) {
        SubscriptionPlan plan = resolvePlan(business);
        if (!plan.isPromotionsEnabled()) {
            throw new ForbiddenException(PLAN_LIMIT,
                    "Promotions are not included in your " + plan.getName() + " plan. Upgrade to unlock them.");
        }
    }

    private SubscriptionPlan defaultPlan(PlanCode code) {
        // Safe fallback if seeder has not run yet
        return SubscriptionPlan.builder()
                .code(code)
                .name(code.name())
                .priceMonthly(java.math.BigDecimal.ZERO)
                .priceYearly(java.math.BigDecimal.ZERO)
                .calendarEnabled(true)
                .walkInEnabled(true)
                .earningsEnabled(true)
                .reportsEnabled(true)
                .advancedReportsEnabled(code == PlanCode.PRO || code == PlanCode.TRIAL)
                .promotionsEnabled(code != PlanCode.STARTER)
                .maxVenues(code == PlanCode.STARTER ? 1 : code == PlanCode.PRO ? null : 5)
                .build();
    }
}
