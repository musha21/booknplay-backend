package lk.booknplay.controller.admin;

import lk.booknplay.dto.response.*;
import lk.booknplay.entity.AdminAuditLog;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.service.AdminPlatformService;
import lk.booknplay.util.ApiResponse;
import lk.booknplay.util.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController @RequestMapping("/api/v1/admin") @RequiredArgsConstructor @PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminPlatformController {
    private final AdminPlatformService service;
    @GetMapping("/dashboard") public ResponseEntity<ApiResponse<AdminDashboardResponse>> dashboard() { return ResponseEntity.ok(ApiResponse.success(service.dashboard())); }
    @GetMapping("/businesses") public ResponseEntity<ApiResponse<PageResponse<AdminBusinessResponse>>> businesses(Pageable pageable) { return ResponseEntity.ok(ApiResponse.success(PageResponse.from(service.businesses(pageable)))); }
    @PatchMapping("/businesses/{id}/access") public ResponseEntity<ApiResponse<AdminBusinessResponse>> access(@PathVariable String id, @RequestParam boolean enabled, @RequestParam boolean locked, @RequestParam(required=false) String reason, Authentication auth) { return ResponseEntity.ok(ApiResponse.success(service.setBusinessAccess(id, enabled, locked, reason, auth.getName()))); }
    @PatchMapping("/businesses/{id}/commission") public ResponseEntity<ApiResponse<AdminBusinessResponse>> commission(@PathVariable String id, @RequestParam BigDecimal value, @RequestParam(required=false) String reason, Authentication auth) { return ResponseEntity.ok(ApiResponse.success(service.setCommission(id, value, reason, auth.getName()))); }
    @GetMapping("/venues") public ResponseEntity<ApiResponse<PageResponse<VenueResponse>>> venues(Pageable pageable) { return ResponseEntity.ok(ApiResponse.success(PageResponse.from(service.venues(pageable)))); }
    @PatchMapping("/venues/{id}/status") public ResponseEntity<ApiResponse<VenueResponse>> venueStatus(@PathVariable String id, @RequestParam VenueStatus status, @RequestParam(required=false) String reason, Authentication auth) { return ResponseEntity.ok(ApiResponse.success(service.setVenueStatus(id, status, reason, auth.getName()))); }
    @GetMapping("/audit") public ResponseEntity<ApiResponse<PageResponse<AdminAuditLog>>> audit(Pageable pageable) { return ResponseEntity.ok(ApiResponse.success(PageResponse.from(service.audit(pageable)))); }
}
