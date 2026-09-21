package lk.booknplay.controller.customer;

import jakarta.validation.Valid;
import lk.booknplay.dto.request.CustomerUpdateRequest;
import lk.booknplay.dto.response.CustomerResponse;
import lk.booknplay.service.CustomerService;
import lk.booknplay.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/profile")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> getProfile(Authentication authentication) {
        CustomerResponse response = customerService.getCurrentCustomer(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody CustomerUpdateRequest request) {
        CustomerResponse current = customerService.getCurrentCustomer(authentication.getName());
        CustomerResponse updated = customerService.updateCustomer(current.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }
}
