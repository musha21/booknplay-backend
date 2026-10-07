package lk.booknplay.service;

import lk.booknplay.dto.request.SubscriptionCheckoutRequest;
import lk.booknplay.dto.response.SubscriptionCheckoutResponse;
import lk.booknplay.dto.response.SubscriptionPlanResponse;
import lk.booknplay.dto.response.SubscriptionResponse;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.BusinessSubscription;
import lk.booknplay.enums.PlanCode;

import java.util.List;
import java.util.Map;

public interface OwnerSubscriptionService {
    /** Create the automatic 90-day TRIAL for a newly registered business. */
    BusinessSubscription createTrialForBusiness(Business business);

    /** Ensure a subscription row exists (backfill TRIAL for legacy businesses). */
    BusinessSubscription ensureSubscription(Business business);

    /** Copy plan.commissionPercent onto the business (plan wins on assign). */
    void applyPlanCommission(String businessId, PlanCode planCode);

    SubscriptionResponse getSubscription(String ownerEmail);

    List<SubscriptionPlanResponse> listPlans();

    SubscriptionCheckoutResponse checkout(String ownerEmail, SubscriptionCheckoutRequest request);

    SubscriptionCheckoutResponse getPaymentStatus(String ownerEmail, String paymentId);

    /** Called from PayHere notify when order_id starts with SUB-. */
    void confirmPayHereNotify(Map<String, String> fields);

    boolean canMutateBusiness(String businessId);
}
