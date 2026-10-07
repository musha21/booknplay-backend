package lk.booknplay.service;

import lk.booknplay.dto.response.SubscriptionResponse;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.SubscriptionPlan;

public interface PlanEntitlementService {
    SubscriptionPlan resolvePlan(Business business);

    SubscriptionResponse.PlanLimits limitsOf(SubscriptionPlan plan);

    long countActiveVenues(String businessId);

    long countActiveCourts(String venueId);

    void assertCanCreateVenue(Business business);

    void assertCanCreateCourt(Business business, String venueId);

    void assertCanAddStaff(Business business);

    void assertCalendarAccess(Business business);

    void assertWalkIn(Business business);

    void assertEarnings(Business business);

    void assertReports(Business business);

    void assertAdvancedReports(Business business);

    void assertPromotions(Business business);
}
