package lk.booknplay.service;

import lk.booknplay.dto.request.AdminMarketingRequest;
import lk.booknplay.dto.response.AdminMarketingResponse;

public interface AdminMarketingService {
    AdminMarketingResponse sendMarketing(AdminMarketingRequest request, String adminEmail);

    /** CSV body for all non-deleted registered customers. */
    String exportCustomersCsv();
}
