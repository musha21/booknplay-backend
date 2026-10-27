package lk.booknplay.controller.admin;

import jakarta.validation.Valid;
import lk.booknplay.dto.request.AdminMarketingRequest;
import lk.booknplay.dto.request.CustomerRegisterRequest;
import lk.booknplay.dto.request.CustomerUpdateRequest;
import lk.booknplay.dto.response.AdminMarketingResponse;
import lk.booknplay.dto.response.CustomerResponse;
import lk.booknplay.service.AdminMarketingService;
import lk.booknplay.service.CustomerService;
import lk.booknplay.util.ApiResponse;
import lk.booknplay.util.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class CustomerAdminController {

    private final CustomerService customerService;
    private final AdminMarketingService adminMarketingService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(@Valid @RequestBody CustomerRegisterRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> searchCustomers(
            @RequestParam(required = false) String query,
            Pageable pageable) {
        PageResponse<CustomerResponse> response = PageResponse.from(customerService.searchCustomers(query, pageable));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCustomers() {
        String csv = adminMarketingService.exportCustomersCsv();
        byte[] body = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"customers.csv\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(body);
    }

    @PostMapping("/marketing")
    public ResponseEntity<ApiResponse<AdminMarketingResponse>> sendMarketing(
            @Valid @RequestBody AdminMarketingRequest request,
            Authentication auth) {
        AdminMarketingResponse response = adminMarketingService.sendMarketing(request, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Marketing campaign processed", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable String id) {
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable String id,
            @Valid @RequestBody CustomerUpdateRequest request) {
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable String id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully"));
    }
}
