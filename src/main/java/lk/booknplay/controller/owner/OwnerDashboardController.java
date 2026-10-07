package lk.booknplay.controller.owner;

import io.swagger.v3.oas.annotations.tags.Tag;
import lk.booknplay.dto.response.OwnerDashboardResponse;
import lk.booknplay.service.OwnerDashboardService;
import lk.booknplay.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owner/dashboard")
@RequiredArgsConstructor
@Tag(name = "Owner Dashboard")
public class OwnerDashboardController {

    private final OwnerDashboardService ownerDashboardService;

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<OwnerDashboardResponse>> today(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerDashboardService.getToday(authentication.getName())));
    }
}
