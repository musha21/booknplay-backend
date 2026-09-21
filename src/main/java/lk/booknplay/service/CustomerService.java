package lk.booknplay.service;

import lk.booknplay.dto.request.CustomerRegisterRequest;
import lk.booknplay.dto.request.CustomerUpdateRequest;
import lk.booknplay.dto.response.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {
    CustomerResponse createCustomer(CustomerRegisterRequest request);
    CustomerResponse getCustomerById(String id);
    CustomerResponse getCustomerByEmail(String email);
    CustomerResponse updateCustomer(String id, CustomerUpdateRequest request);
    void deleteCustomer(String id);
    Page<CustomerResponse> searchCustomers(String query, Pageable pageable);
    CustomerResponse getCurrentCustomer(String email);
}
