package lk.booknplay.service.impl;

import lk.booknplay.dto.request.AdminSubscriptionPlanUpdateRequest;
import lk.booknplay.dto.request.AdminSubscriptionUpdateRequest;
import lk.booknplay.dto.response.*;
import lk.booknplay.entity.*;
import lk.booknplay.enums.PlanCode;
import lk.booknplay.enums.SubscriptionStatus;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.repository.*;
import lk.booknplay.service.AdminPlatformService;
import lk.booknplay.service.OwnerSubscriptionService;
import lk.booknplay.util.SubscriptionAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPlatformServiceImpl implements AdminPlatformService {
    private final CustomerRepository customers;
    private final BusinessRepository businesses;
    private final VenueRepository venues;
    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final AdminAuditLogRepository auditLogs;
    private final BusinessSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final OwnerSubscriptionService ownerSubscriptionService;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        long trialing = subscriptionRepository.countByStatus(SubscriptionStatus.TRIALING);
        long activePaid = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE)
                + subscriptionRepository.countByStatus(SubscriptionStatus.PAST_DUE);
        long expired = subscriptionRepository.countByStatus(SubscriptionStatus.EXPIRED)
                + subscriptionRepository.countByStatus(SubscriptionStatus.CANCELED);

        List<AdminDashboardResponse.PlanSubscriptionStat> byPlan = planRepository.findAll().stream()
                .sorted(Comparator.comparingInt(SubscriptionPlan::getSortOrder))
                .map(plan -> AdminDashboardResponse.PlanSubscriptionStat.builder()
                        .code(plan.getCode().name())
                        .name(plan.getName())
                        .businessCount(subscriptionRepository.countByPlanCode(plan.getCode()))
                        .commissionPercent(plan.getCommissionPercent() != null
                                ? plan.getCommissionPercent()
                                : BigDecimal.ZERO)
                        .build())
                .toList();

        return AdminDashboardResponse.builder()
                .customers(customers.count())
                .businesses(businesses.count())
                .venues(venues.count())
                .pendingVenues(venues.countByStatus(VenueStatus.PENDING_APPROVAL))
                .bookings(bookings.count())
                .payments(payments.count())
                .grossBookingValue(payments.sumSuccessfulPayments())
                .trialingCount(trialing)
                .activePaidCount(activePaid)
                .expiredCount(expired)
                .subscriptionByPlan(byPlan)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminBusinessResponse> businesses(Pageable pageable) {
        return businesses.findAll(pageable).map(this::business);
    }

    @Override
    @Transactional
    public AdminBusinessResponse setBusinessAccess(String id, boolean enabled, boolean locked, String reason, String adminEmail) {
        Business entity = findBusiness(id);
        User owner = users.findById(entity.getOwnerId()).orElseThrow(() -> new ResourceNotFoundException("Business owner not found"));
        owner.setEnabled(enabled);
        owner.setLocked(locked);
        users.save(owner);
        audit(adminEmail, "BUSINESS_ACCESS_CHANGED", "BUSINESS", id, reason);
        return business(entity);
    }

    @Override
    @Transactional
    public AdminBusinessResponse setCommission(String id, BigDecimal value, String reason, String adminEmail) {
        if (value == null || value.signum() < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("Commission must be between 0 and 100");
        }
        Business entity = findBusiness(id);
        entity.setCommissionPercent(value);
        businesses.save(entity);
        audit(adminEmail, "COMMISSION_CHANGED", "BUSINESS", id, reason);
        return business(entity);
    }

    @Override
    @Transactional
    public AdminBusinessResponse setBusinessSubscription(String id, AdminSubscriptionUpdateRequest request, String adminEmail) {
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BadRequestException("reason is required");
        }
        Business entity = findBusiness(id);
        BusinessSubscription subscription = ownerSubscriptionService.ensureSubscription(entity);

        if (request.getPlanCode() != null) {
            subscription.setPlanCode(request.getPlanCode());
        }
        if (request.getStatus() != null) {
            subscription.setStatus(request.getStatus());
        }
        if (request.getTrialEndsAt() != null) {
            subscription.setTrialEndsAt(request.getTrialEndsAt());
            if (subscription.getTrialStartsAt() == null) {
                subscription.setTrialStartsAt(LocalDateTime.now());
            }
        }

        // Convenience: extend trial without explicit trialEndsAt
        if (request.getPlanCode() == PlanCode.TRIAL
                && request.getStatus() == SubscriptionStatus.TRIALING
                && request.getTrialEndsAt() == null) {
            LocalDateTime now = LocalDateTime.now();
            subscription.setPlanCode(PlanCode.TRIAL);
            subscription.setStatus(SubscriptionStatus.TRIALING);
            subscription.setTrialStartsAt(now);
            subscription.setTrialEndsAt(now.plusDays(SubscriptionAccess.TRIAL_DURATION_DAYS));
        }

        // Force paid plan without period dates
        if (request.getPlanCode() != null
                && request.getPlanCode() != PlanCode.TRIAL
                && request.getStatus() == SubscriptionStatus.ACTIVE
                && subscription.getCurrentPeriodEnd() == null) {
            LocalDateTime now = LocalDateTime.now();
            subscription.setCurrentPeriodStart(now);
            subscription.setCurrentPeriodEnd(now.plusMonths(1));
        }

        subscriptionRepository.save(subscription);
        if (subscription.getPlanCode() != null) {
            ownerSubscriptionService.applyPlanCommission(entity.getId(), subscription.getPlanCode());
        }
        audit(adminEmail, "BUSINESS_SUBSCRIPTION_CHANGED", "BUSINESS", id, request.getReason());
        return business(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VenueResponse> venues(Pageable pageable) {
        return venues.findAll(pageable).map(this::venue);
    }

    @Override
    @Transactional
    public VenueResponse setVenueStatus(String id, VenueStatus status, String reason, String adminEmail) {
        Venue entity = venues.findById(id).orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        entity.setStatus(status);
        venues.save(entity);
        audit(adminEmail, "VENUE_STATUS_CHANGED", "VENUE", id, reason);
        return venue(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> listSubscriptionPlans() {
        return planRepository.findAll().stream()
                .sorted(Comparator.comparingInt(SubscriptionPlan::getSortOrder))
                .map(this::mapPlan)
                .toList();
    }

    @Override
    @Transactional
    public SubscriptionPlanResponse updateSubscriptionPlan(PlanCode code, AdminSubscriptionPlanUpdateRequest request, String adminEmail) {
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BadRequestException("reason is required");
        }
        SubscriptionPlan plan = planRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + code));
        if (request.getName() != null) plan.setName(request.getName());
        if (request.getDescription() != null) plan.setDescription(request.getDescription());
        if (request.getPriceMonthly() != null) plan.setPriceMonthly(request.getPriceMonthly());
        if (request.getPriceYearly() != null) plan.setPriceYearly(request.getPriceYearly());
        if (request.getCurrency() != null) plan.setCurrency(request.getCurrency());
        if (request.getHighlighted() != null) plan.setHighlighted(request.getHighlighted());
        if (request.getActive() != null) plan.setActive(request.getActive());
        if (request.getSortOrder() != null) plan.setSortOrder(request.getSortOrder());
        if (request.getFeatures() != null) plan.setFeatures(request.getFeatures());
        if (Boolean.TRUE.equals(request.getUnlimitedVenues())) {
            plan.setMaxVenues(null);
        } else if (Boolean.FALSE.equals(request.getUnlimitedVenues())) {
            if (request.getMaxVenues() == null || request.getMaxVenues() < 1) {
                throw new BadRequestException("maxVenues must be at least 1 when unlimitedVenues is false");
            }
            plan.setMaxVenues(request.getMaxVenues());
        } else if (request.getMaxVenues() != null) {
            if (request.getMaxVenues() < 1) {
                throw new BadRequestException("maxVenues must be at least 1");
            }
            plan.setMaxVenues(request.getMaxVenues());
        }
        if (Boolean.TRUE.equals(request.getUnlimitedCourtsPerVenue())) {
            plan.setMaxCourtsPerVenue(null);
        } else if (Boolean.FALSE.equals(request.getUnlimitedCourtsPerVenue())) {
            if (request.getMaxCourtsPerVenue() == null || request.getMaxCourtsPerVenue() < 1) {
                throw new BadRequestException("maxCourtsPerVenue must be at least 1 when unlimitedCourtsPerVenue is false");
            }
            plan.setMaxCourtsPerVenue(request.getMaxCourtsPerVenue());
        } else if (request.getMaxCourtsPerVenue() != null) {
            if (request.getMaxCourtsPerVenue() < 1) {
                throw new BadRequestException("maxCourtsPerVenue must be at least 1");
            }
            plan.setMaxCourtsPerVenue(request.getMaxCourtsPerVenue());
        }
        if (request.getCalendarEnabled() != null) plan.setCalendarEnabled(request.getCalendarEnabled());
        if (request.getWalkInEnabled() != null) plan.setWalkInEnabled(request.getWalkInEnabled());
        if (request.getEarningsEnabled() != null) plan.setEarningsEnabled(request.getEarningsEnabled());
        if (request.getReportsEnabled() != null) plan.setReportsEnabled(request.getReportsEnabled());
        if (request.getAdvancedReportsEnabled() != null) plan.setAdvancedReportsEnabled(request.getAdvancedReportsEnabled());
        if (request.getCommissionPercent() != null) {
            BigDecimal commission = request.getCommissionPercent();
            if (commission.signum() < 0 || commission.compareTo(new BigDecimal("100")) > 0) {
                throw new BadRequestException("Commission must be between 0 and 100");
            }
            plan.setCommissionPercent(commission);
        }
        if (Boolean.TRUE.equals(request.getUnlimitedStaff())) {
            plan.setMaxStaff(null);
        } else if (Boolean.FALSE.equals(request.getUnlimitedStaff())) {
            if (request.getMaxStaff() == null || request.getMaxStaff() < 1) {
                throw new BadRequestException("maxStaff must be at least 1 when unlimitedStaff is false");
            }
            plan.setMaxStaff(request.getMaxStaff());
        } else if (request.getMaxStaff() != null) {
            if (request.getMaxStaff() < 1) {
                throw new BadRequestException("maxStaff must be at least 1");
            }
            plan.setMaxStaff(request.getMaxStaff());
        }
        planRepository.save(plan);
        audit(adminEmail, "SUBSCRIPTION_PLAN_UPDATED", "SUBSCRIPTION_PLAN", code.name(), request.getReason());
        return mapPlan(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminAuditLog> audit(Pageable pageable) {
        return auditLogs.findAllByOrderByCreatedAtDesc(pageable);
    }

    private Business findBusiness(String id) {
        return businesses.findById(id).orElseThrow(() -> new ResourceNotFoundException("Business not found"));
    }

    private AdminBusinessResponse business(Business b) {
        User owner = users.findById(b.getOwnerId()).orElse(null);
        BusinessSubscription subscription = subscriptionRepository.findByBusinessId(b.getId()).orElse(null);
        SubscriptionPlan plan = null;
        if (subscription != null && subscription.getPlanCode() != null) {
            plan = planRepository.findByCode(subscription.getPlanCode()).orElse(null);
        }
        return AdminBusinessResponse.builder().id(b.getId()).name(b.getName()).ownerId(b.getOwnerId())
                .ownerEmail(owner == null ? null : owner.getEmail()).ownerName(b.getOwnerName()).contactEmail(b.getContactEmail())
                .contactPhone(b.getContactPhone()).commissionPercent(b.getCommissionPercent())
                .enabled(owner != null && owner.isEnabled()).locked(owner != null && !owner.isAccountNonLocked())
                .venueCount(venues.findByBusinessId(b.getId()).stream().filter(v -> v.getStatus() == VenueStatus.ACTIVE || v.getStatus() == VenueStatus.APPROVED).count())
                .logoUrl(b.getLogoUrl())
                .imageUrls(b.getImages() == null ? List.of() : b.getImages().stream().map(BusinessImage::getUrl).filter(url -> url != null && !url.isBlank()).toList())
                .planCode(subscription == null || subscription.getPlanCode() == null ? null : subscription.getPlanCode().name())
                .subscriptionStatus(subscription == null || subscription.getStatus() == null ? null : subscription.getStatus().name())
                .trialEndsAt(subscription == null ? null : subscription.getTrialEndsAt())
                .limits(plan == null ? null : SubscriptionResponse.PlanLimits.builder()
                        .maxVenues(plan.getMaxVenues())
                        .maxCourtsPerVenue(plan.getMaxCourtsPerVenue())
                        .calendarEnabled(plan.isCalendarEnabled())
                        .walkInEnabled(plan.isWalkInEnabled())
                        .earningsEnabled(plan.isEarningsEnabled())
                        .reportsEnabled(plan.isReportsEnabled())
                        .advancedReportsEnabled(plan.isAdvancedReportsEnabled())
                        .build())
                .build();
    }

    private SubscriptionPlanResponse mapPlan(SubscriptionPlan plan) {
        List<String> features = plan.getFeatures() == null || plan.getFeatures().isBlank()
                ? List.of()
                : Arrays.stream(plan.getFeatures().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
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

    private VenueResponse venue(Venue v) {
        return VenueResponse.builder().id(v.getId()).businessId(v.getBusiness().getId()).businessName(v.getBusiness().getName()).name(v.getName()).city(v.getCity()).venueType(v.getVenueType()).status(v.getStatus()).coverImageUrl(v.getCoverImageUrl()).build();
    }

    private void audit(String email, String action, String type, String id, String reason) {
        auditLogs.save(AdminAuditLog.builder().adminEmail(email).action(action).resourceType(type).resourceId(id).reason(reason).build());
    }
}
