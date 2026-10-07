package lk.booknplay.service;

import lk.booknplay.dto.request.AdminSubscriptionPlanUpdateRequest;
import lk.booknplay.dto.request.AdminSubscriptionUpdateRequest;
import lk.booknplay.dto.response.AdminBusinessResponse;
import lk.booknplay.dto.response.AdminDashboardResponse;
import lk.booknplay.dto.response.SubscriptionPlanResponse;
import lk.booknplay.dto.response.VenueResponse;
import lk.booknplay.entity.AdminAuditLog;
import lk.booknplay.enums.PlanCode;
import lk.booknplay.enums.VenueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface AdminPlatformService {
    AdminDashboardResponse dashboard();
    Page<AdminBusinessResponse> businesses(Pageable pageable);
    AdminBusinessResponse setBusinessAccess(String id, boolean enabled, boolean locked, String reason, String adminEmail);
    AdminBusinessResponse setCommission(String id, BigDecimal commissionPercent, String reason, String adminEmail);
    AdminBusinessResponse setBusinessSubscription(String id, AdminSubscriptionUpdateRequest request, String adminEmail);
    List<SubscriptionPlanResponse> listSubscriptionPlans();
    SubscriptionPlanResponse updateSubscriptionPlan(PlanCode code, AdminSubscriptionPlanUpdateRequest request, String adminEmail);
    Page<VenueResponse> venues(Pageable pageable);
    VenueResponse setVenueStatus(String id, VenueStatus status, String reason, String adminEmail);
    Page<AdminAuditLog> audit(Pageable pageable);
}
