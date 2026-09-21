package lk.booknplay.controller.admin;

import jakarta.validation.Valid;
import lk.booknplay.dto.request.HomepageConfigRequest;
import lk.booknplay.dto.response.HomepageConfigResponse;
import lk.booknplay.service.HomepageConfigService;
import lk.booknplay.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/admin/homepage") @RequiredArgsConstructor @PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminHomepageController {
    private final HomepageConfigService service;
    @GetMapping public ResponseEntity<ApiResponse<HomepageConfigResponse>> draft() { return ResponseEntity.ok(ApiResponse.success(service.draft())); }
    @PutMapping("/draft") public ResponseEntity<ApiResponse<HomepageConfigResponse>> save(@Valid @RequestBody HomepageConfigRequest request, Authentication auth) { return ResponseEntity.ok(ApiResponse.success(service.saveDraft(request, auth.getName()))); }
    @PostMapping("/publish") public ResponseEntity<ApiResponse<HomepageConfigResponse>> publish(@RequestParam(required=false) String reason, Authentication auth) { return ResponseEntity.ok(ApiResponse.success(service.publish(reason, auth.getName()))); }
    @GetMapping("/versions") public ResponseEntity<ApiResponse<List<HomepageConfigResponse>>> versions() { return ResponseEntity.ok(ApiResponse.success(service.versions())); }
    @PostMapping("/versions/{id}/restore") public ResponseEntity<ApiResponse<HomepageConfigResponse>> restore(@PathVariable String id, @RequestParam(required=false) String reason, Authentication auth) { return ResponseEntity.ok(ApiResponse.success(service.restore(id, reason, auth.getName()))); }
}
