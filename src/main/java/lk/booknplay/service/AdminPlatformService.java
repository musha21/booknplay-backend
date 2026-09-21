package lk.booknplay.service;

import lk.booknplay.dto.response.AdminBusinessResponse;
import lk.booknplay.dto.response.AdminDashboardResponse;
import lk.booknplay.dto.response.VenueResponse;
import lk.booknplay.entity.AdminAuditLog;
import lk.booknplay.enums.VenueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;

public interface AdminPlatformService {
    AdminDashboardResponse dashboard();
    Page<AdminBusinessResponse> businesses(Pageable pageable);
    AdminBusinessResponse setBusinessAccess(String id, boolean enabled, boolean locked, String reason, String adminEmail);
    AdminBusinessResponse setCommission(String id, BigDecimal commissionPercent, String reason, String adminEmail);
    Page<VenueResponse> venues(Pageable pageable);
    VenueResponse setVenueStatus(String id, VenueStatus status, String reason, String adminEmail);
    Page<AdminAuditLog> audit(Pageable pageable);
}
