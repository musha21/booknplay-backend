package lk.booknplay.controller.admin;

import jakarta.validation.Valid;
import lk.booknplay.dto.request.CustomerLoginRequest;
import lk.booknplay.dto.request.RefreshTokenRequest;
import lk.booknplay.dto.response.AdminResponse;
import lk.booknplay.dto.response.AuthResponse;
import lk.booknplay.service.AdminAuthService;
import lk.booknplay.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/auth") @RequiredArgsConstructor
public class AdminAuthController {
    private final AdminAuthService service;
    @PostMapping("/login") public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody CustomerLoginRequest request) { return ResponseEntity.ok(ApiResponse.success(service.login(request))); }
    @PostMapping("/refresh") public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) { return ResponseEntity.ok(ApiResponse.success(service.refresh(request))); }
    @PostMapping("/logout") public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) { service.logout(request.getRefreshToken()); return ResponseEntity.ok(ApiResponse.success("Logged out")); }
    @GetMapping("/me") public ResponseEntity<ApiResponse<AdminResponse>> me(Authentication authentication) { return ResponseEntity.ok(ApiResponse.success(service.me(authentication.getName()))); }
}
