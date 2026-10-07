package lk.booknplay.service.impl;

import lk.booknplay.dto.request.SubscriptionCheckoutRequest;
import lk.booknplay.dto.response.SubscriptionCheckoutResponse;
import lk.booknplay.dto.response.SubscriptionPlanResponse;
import lk.booknplay.dto.response.SubscriptionResponse;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.BusinessSubscription;
import lk.booknplay.entity.SubscriptionPayment;
import lk.booknplay.entity.SubscriptionPlan;
import lk.booknplay.entity.User;
import lk.booknplay.enums.BillingInterval;
import lk.booknplay.enums.PaymentStatus;
import lk.booknplay.enums.PlanCode;
import lk.booknplay.enums.SubscriptionStatus;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.repository.BusinessRepository;
import lk.booknplay.repository.BusinessStaffRepository;
import lk.booknplay.repository.BusinessSubscriptionRepository;
import lk.booknplay.repository.SubscriptionPaymentRepository;
import lk.booknplay.repository.SubscriptionPlanRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.repository.VenueRepository;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.service.OwnerAccessService;
import lk.booknplay.service.OwnerSubscriptionService;
import lk.booknplay.service.PayHereCheckoutStore;
import lk.booknplay.util.PayHereHash;
import lk.booknplay.util.SubscriptionAccess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OwnerSubscriptionServiceImpl implements OwnerSubscriptionService {

    public static final String ORDER_PREFIX = "SUB-";

    private final BusinessSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final BusinessRepository businessRepository;
    private final OwnerAccessService ownerAccessService;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final BusinessStaffRepository businessStaffRepository;
    private final PayHereCheckoutStore checkoutStore;

    private final String paymentsMode;
    private final String appBaseUrl;
    private final String merchantId;
    private final String merchantSecret;
    private final String returnUrlBase;
    private final String cancelUrlBase;

    public OwnerSubscriptionServiceImpl(
            BusinessSubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository planRepository,
            SubscriptionPaymentRepository paymentRepository,
            BusinessRepository businessRepository,
            OwnerAccessService ownerAccessService,
            UserRepository userRepository,
            VenueRepository venueRepository,
            BusinessStaffRepository businessStaffRepository,
            PayHereCheckoutStore checkoutStore,
            @Value("${booknplay.payments.mode:PAYHERE}") String paymentsMode,
            @Value("${app.base-url:http://localhost:8080}") String appBaseUrl,
            @Value("${payhere.merchant-id:}") String merchantId,
            @Value("${payhere.merchant-secret:}") String merchantSecret,
            @Value("${booknplay.subscriptions.return-url:http://localhost:5173/owner/billing/return}") String returnUrlBase,
            @Value("${booknplay.subscriptions.cancel-url:http://localhost:5173/owner/billing/return}") String cancelUrlBase) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.paymentRepository = paymentRepository;
        this.businessRepository = businessRepository;
        this.ownerAccessService = ownerAccessService;
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
        this.businessStaffRepository = businessStaffRepository;
        this.checkoutStore = checkoutStore;
        this.paymentsMode = paymentsMode;
        this.appBaseUrl = trimTrailingSlash(appBaseUrl);
        this.merchantId = merchantId == null ? "" : merchantId.trim();
        this.merchantSecret = merchantSecret == null ? "" : merchantSecret.trim();
        this.returnUrlBase = returnUrlBase;
        this.cancelUrlBase = cancelUrlBase;
    }

    @Override
    @Transactional
    public BusinessSubscription createTrialForBusiness(Business business) {
        LocalDateTime now = LocalDateTime.now();
        BusinessSubscription subscription = BusinessSubscription.builder()
                .businessId(business.getId())
                .planCode(PlanCode.TRIAL)
                .status(SubscriptionStatus.TRIALING)
                .trialStartsAt(now)
                .trialEndsAt(now.plusDays(SubscriptionAccess.TRIAL_DURATION_DAYS))
                .cancelAtPeriodEnd(false)
                .build();
        BusinessSubscription saved = subscriptionRepository.save(subscription);
        applyPlanCommission(business.getId(), PlanCode.TRIAL);
        return saved;
    }

    @Override
    @Transactional
    public void applyPlanCommission(String businessId, PlanCode planCode) {
        if (businessId == null || planCode == null) {
            return;
        }
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        SubscriptionPlan plan = planRepository.findByCode(planCode).orElse(null);
        BigDecimal percent = plan != null && plan.getCommissionPercent() != null
                ? plan.getCommissionPercent()
                : (planCode == PlanCode.TRIAL ? BigDecimal.ZERO : new BigDecimal("10.00"));
        if (percent.signum() < 0 || percent.compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("Plan commission must be between 0 and 100");
        }
        business.setCommissionPercent(percent);
        businessRepository.save(business);
    }

    @Override
    @Transactional
    public BusinessSubscription ensureSubscription(Business business) {
        return subscriptionRepository.findByBusinessId(business.getId())
                .map(existing -> persistIfExpired(existing))
                .orElseGet(() -> backfillTrial(business));
    }

    @Override
    @Transactional
    public SubscriptionResponse getSubscription(String ownerEmail) {
        Business business = ownerAccessService.requireBusiness(ownerEmail);
        return toResponse(ensureSubscription(business));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> listPlans() {
        return planRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .filter(plan -> plan.getCode() != PlanCode.TRIAL)
                .map(this::toPlanResponse)
                .toList();
    }

    @Override
    @Transactional
    public SubscriptionCheckoutResponse checkout(String ownerEmail, SubscriptionCheckoutRequest request) {
        Business business = ownerAccessService.requireOwner(ownerEmail);
        ensureSubscription(business);

        PlanCode planCode = request.getPlanCode();
        if (planCode == null || planCode == PlanCode.TRIAL) {
            throw new BadRequestException("Choose a paid plan to subscribe");
        }
        BillingInterval interval = request.getBillingInterval();
        if (interval == null) {
            throw new BadRequestException("billingInterval is required");
        }

        SubscriptionPlan plan = planRepository.findByCode(planCode)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planCode));
        if (!plan.isActive()) {
            throw new BadRequestException("Plan is not available");
        }

        BigDecimal amount = interval == BillingInterval.YEARLY ? plan.getPriceYearly() : plan.getPriceMonthly();
        String gateway = resolveGateway(request.getGateway());

        SubscriptionPayment payment = paymentRepository.save(SubscriptionPayment.builder()
                .businessId(business.getId())
                .planCode(planCode)
                .billingInterval(interval)
                .amount(amount)
                .currency(plan.getCurrency())
                .paymentGateway(gateway)
                .orderId("PENDING")
                .status(PaymentStatus.INITIATED)
                .build());

        // Stable PayHere order_id once the UUID exists.
        String orderId = ORDER_PREFIX + payment.getId().replace("-", "");
        payment.setOrderId(orderId);

        if (isDummyGateway(gateway)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setGatewayReference("DUMMY");
            paymentRepository.save(payment);
            activatePaidPlan(business.getId(), planCode, interval);
            return SubscriptionCheckoutResponse.builder()
                    .paymentId(payment.getId())
                    .paymentUrl(null)
                    .status(PaymentStatus.SUCCESS)
                    .paymentGateway(gateway)
                    .build();
        }

        if (merchantId.isBlank() || merchantSecret.isBlank()) {
            throw new BadRequestException(
                    "PayHere merchant credentials are not configured for subscription checkout");
        }

        User owner = userRepository.findById(business.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Business owner not found"));
        String email = business.getContactEmail() != null ? business.getContactEmail() : owner.getEmail();
        String firstName = splitName(business.getOwnerName(), true, "Owner");
        String lastName = splitName(business.getOwnerName(), false, "Account");
        String phone = business.getContactPhone() == null ? "" : business.getContactPhone();
        String hash = PayHereHash.checkoutHash(merchantId, orderId, amount, plan.getCurrency(), merchantSecret);
        String token = PayHereCheckoutStore.newToken();
        String returnUrl = UriComponentsBuilder.fromUriString(returnUrlBase)
                .replaceQueryParam("paymentId", payment.getId())
                .build(true)
                .toUriString();
        String cancelUrl = UriComponentsBuilder.fromUriString(cancelUrlBase)
                .replaceQueryParam("paymentId", payment.getId())
                .build(true)
                .toUriString();

        checkoutStore.put(new PayHereCheckoutStore.Session(
                token,
                payment.getId(),
                orderId,
                amount,
                plan.getCurrency(),
                firstName,
                lastName,
                email,
                phone,
                "BooknPlay " + plan.getName() + " (" + interval.name().toLowerCase() + ")",
                hash,
                returnUrl,
                cancelUrl
        ));

        payment.setCheckoutToken(token);
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        return SubscriptionCheckoutResponse.builder()
                .paymentId(payment.getId())
                .paymentUrl(appBaseUrl + "/api/v1/public/payhere/checkout/" + token)
                .status(PaymentStatus.PROCESSING)
                .paymentGateway("PAYHERE")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionCheckoutResponse getPaymentStatus(String ownerEmail, String paymentId) {
        Business business = ownerAccessService.requireBusiness(ownerEmail);
        SubscriptionPayment payment = paymentRepository.findByIdAndBusinessId(paymentId, business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription payment not found"));
        return SubscriptionCheckoutResponse.builder()
                .paymentId(payment.getId())
                .paymentUrl(null)
                .status(payment.getStatus())
                .paymentGateway(payment.getPaymentGateway())
                .build();
    }

    @Override
    @Transactional
    public void confirmPayHereNotify(Map<String, String> fields) {
        if (merchantSecret.isBlank()) {
            throw new BadRequestException("PayHere merchant secret is not configured");
        }
        String orderId = firstNonBlank(fields.get("order_id"), fields.get("orderId"));
        String paymentId = firstNonBlank(fields.get("payment_id"), fields.get("paymentId"));
        String statusCode = firstNonBlank(fields.get("status_code"), fields.get("statusCode"));
        String md5sig = firstNonBlank(fields.get("md5sig"), fields.get("md5Sig"));
        String payhereAmount = firstNonBlank(fields.get("payhere_amount"), fields.get("payhereAmount"));
        String payhereCurrency = firstNonBlank(fields.get("payhere_currency"), fields.get("payhereCurrency"), "LKR");
        String merchantFromPayload = firstNonBlank(fields.get("merchant_id"), fields.get("merchantId"), merchantId);

        if (orderId == null || statusCode == null || md5sig == null || payhereAmount == null) {
            throw new BadRequestException("Incomplete PayHere notify payload");
        }

        String expected = PayHereHash.notifySignature(
                merchantFromPayload, orderId, payhereAmount, payhereCurrency, statusCode, merchantSecret);
        if (!expected.equalsIgnoreCase(md5sig)) {
            throw new BadRequestException("Invalid PayHere notify signature");
        }

        SubscriptionPayment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription payment not found for order " + orderId));

        if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.PAID) {
            return;
        }

        if ("2".equals(statusCode.trim())) {
            payment.setPaymentGateway("PAYHERE");
            if (paymentId != null && paymentId.trim().matches("\\d+")) {
                payment.setGatewayReference(paymentId.trim());
            }
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);
            activatePaidPlan(payment.getBusinessId(), payment.getPlanCode(), payment.getBillingInterval());
            log.info("Subscription payment {} activated plan {} for business {}",
                    payment.getId(), payment.getPlanCode(), payment.getBusinessId());
        } else {
            payment.setPaymentGateway("PAYHERE");
            if (paymentId != null && paymentId.trim().matches("\\d+")) {
                payment.setGatewayReference(paymentId.trim());
            }
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canMutateBusiness(String businessId) {
        return subscriptionRepository.findByBusinessId(businessId)
                .map(sub -> {
                    SubscriptionAccess.refreshExpiredInMemory(sub, LocalDateTime.now());
                    return SubscriptionAccess.canMutate(sub, LocalDateTime.now());
                })
                .orElse(false);
    }

    @Transactional
    public void activatePaidPlan(String businessId, PlanCode planCode, BillingInterval interval) {
        BusinessSubscription subscription = subscriptionRepository.findByBusinessId(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        LocalDateTime now = LocalDateTime.now();
        subscription.setPlanCode(planCode);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(interval == BillingInterval.YEARLY ? now.plusYears(1) : now.plusMonths(1));
        subscription.setCancelAtPeriodEnd(false);
        subscriptionRepository.save(subscription);
        applyPlanCommission(businessId, planCode);
    }

    private BusinessSubscription backfillTrial(Business business) {
        LocalDateTime start = business.getCreatedAt() != null ? business.getCreatedAt() : LocalDateTime.now();
        LocalDateTime end = start.plusDays(SubscriptionAccess.TRIAL_DURATION_DAYS);
        LocalDateTime now = LocalDateTime.now();
        boolean stillTrial = !end.isBefore(now);
        BusinessSubscription subscription = BusinessSubscription.builder()
                .businessId(business.getId())
                .planCode(stillTrial ? PlanCode.TRIAL : PlanCode.TRIAL)
                .status(stillTrial ? SubscriptionStatus.TRIALING : SubscriptionStatus.EXPIRED)
                .trialStartsAt(start)
                .trialEndsAt(end)
                .cancelAtPeriodEnd(false)
                .build();
        return subscriptionRepository.save(subscription);
    }

    private BusinessSubscription persistIfExpired(BusinessSubscription subscription) {
        LocalDateTime now = LocalDateTime.now();
        SubscriptionStatus before = subscription.getStatus();
        SubscriptionAccess.refreshExpiredInMemory(subscription, now);
        if (before != subscription.getStatus()) {
            return subscriptionRepository.save(subscription);
        }
        return subscription;
    }

    private SubscriptionResponse toResponse(BusinessSubscription subscription) {
        LocalDateTime now = LocalDateTime.now();
        SubscriptionAccess.refreshExpiredInMemory(subscription, now);
        boolean canMutate = SubscriptionAccess.canMutate(subscription, now);
        SubscriptionPlan plan = planRepository.findByCode(
                subscription.getPlanCode() != null ? subscription.getPlanCode() : PlanCode.TRIAL
        ).orElse(null);
        long venueCount = venueRepository.findByBusinessIdAndStatusNot(
                subscription.getBusinessId(), VenueStatus.DELETED).size();
        long staffCount = businessStaffRepository.countByBusinessIdAndActiveTrue(subscription.getBusinessId());
        return SubscriptionResponse.builder()
                .businessId(subscription.getBusinessId())
                .planCode(subscription.getPlanCode())
                .status(subscription.getStatus())
                .trialStartsAt(subscription.getTrialStartsAt())
                .trialEndsAt(subscription.getTrialEndsAt())
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .cancelAtPeriodEnd(subscription.isCancelAtPeriodEnd())
                .daysRemaining(SubscriptionAccess.daysRemaining(subscription, now))
                .access(SubscriptionResponse.SubscriptionAccess.builder()
                        .canMutate(canMutate)
                        .reason(SubscriptionAccess.accessReason(subscription, now))
                        .build())
                .limits(limitsOf(plan))
                .usage(SubscriptionResponse.SubscriptionUsage.builder()
                        .venueCount(venueCount)
                        .staffCount(staffCount)
                        .build())
                .build();
    }

    private SubscriptionResponse.PlanLimits limitsOf(SubscriptionPlan plan) {
        if (plan == null) {
            return SubscriptionResponse.PlanLimits.builder()
                    .calendarEnabled(true)
                    .walkInEnabled(true)
                    .earningsEnabled(true)
                    .reportsEnabled(true)
                    .advancedReportsEnabled(true)
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
                .build();
    }

    private SubscriptionPlanResponse toPlanResponse(SubscriptionPlan plan) {
        List<String> features = plan.getFeatures() == null || plan.getFeatures().isBlank()
                ? List.of()
                : Arrays.stream(plan.getFeatures().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return SubscriptionPlanResponse.builder()
                .code(plan.getCode())
                .name(plan.getName())
                .description(plan.getDescription())
                .priceMonthly(plan.getPriceMonthly())
                .priceYearly(plan.getPriceYearly())
                .currency(plan.getCurrency())
                .highlighted(plan.isHighlighted())
                .features(features)
                .sortOrder(plan.getSortOrder())
                .active(plan.isActive())
                .maxVenues(plan.getMaxVenues())
                .maxCourtsPerVenue(plan.getMaxCourtsPerVenue())
                .calendarEnabled(plan.isCalendarEnabled())
                .walkInEnabled(plan.isWalkInEnabled())
                .earningsEnabled(plan.isEarningsEnabled())
                .reportsEnabled(plan.isReportsEnabled())
                .advancedReportsEnabled(plan.isAdvancedReportsEnabled())
                .commissionPercent(plan.getCommissionPercent())
                .maxStaff(plan.getMaxStaff())
                .build();
    }

    private String resolveGateway(String requested) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim().toUpperCase();
        }
        if ("DUMMY".equalsIgnoreCase(String.valueOf(paymentsMode).trim())) {
            return "DUMMY";
        }
        return "PAYHERE";
    }

    private boolean isDummyGateway(String gateway) {
        return "DUMMY".equalsIgnoreCase(gateway)
                || "DUMMY".equalsIgnoreCase(String.valueOf(paymentsMode).trim());
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String splitName(String full, boolean first, String fallback) {
        if (full == null || full.isBlank()) return fallback;
        String[] parts = full.trim().split("\\s+", 2);
        if (first) return parts[0];
        return parts.length > 1 ? parts[1] : fallback;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }
}
