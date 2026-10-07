package lk.booknplay.controller.owner;

import jakarta.validation.Valid;
import lk.booknplay.dto.request.OwnerStaffInviteRequest;
import lk.booknplay.dto.request.OwnerStaffUpdateRequest;
import lk.booknplay.util.ApiResponse;
import lk.booknplay.dto.response.OwnerStaffResponse;
import lk.booknplay.service.OwnerStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/owner/staff")
@RequiredArgsConstructor
public class OwnerStaffController {

    private final OwnerStaffService ownerStaffService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OwnerStaffResponse>>> list(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(ownerStaffService.list(auth.getName())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OwnerStaffResponse>> invite(
            Authentication auth,
            @Valid @RequestBody OwnerStaffInviteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ownerStaffService.invite(auth.getName(), request)));
    }

    @PatchMapping("/{staffId}")
    public ResponseEntity<ApiResponse<OwnerStaffResponse>> update(
            Authentication auth,
            @PathVariable String staffId,
            @RequestBody OwnerStaffUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ownerStaffService.update(auth.getName(), staffId, request)));
    }

    @DeleteMapping("/{staffId}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication auth, @PathVariable String staffId) {
        ownerStaffService.delete(auth.getName(), staffId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
