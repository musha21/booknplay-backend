package lk.booknplay.service;

import lk.booknplay.dto.request.AdminBankVerificationRequest;
import lk.booknplay.dto.request.AdminPayoutConfigUpdateRequest;
import lk.booknplay.dto.request.AdminPayoutSettleRequest;
import lk.booknplay.dto.request.AdminPayoutStatusUpdateRequest;
import lk.booknplay.dto.response.AdminPayoutBusinessResponse;
import lk.booknplay.dto.response.OwnerPayoutSummaryResponse;
import lk.booknplay.dto.response.PlatformPayoutConfigResponse;
import lk.booknplay.dto.response.PayoutResponse;
import lk.booknplay.enums.PayoutFrequency;
import lk.booknplay.enums.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface PayoutSettlementService {

    int generateSettlements(LocalDate asOf);

    OwnerPayoutSummaryResponse ownerSummary(String ownerEmail);

    List<PayoutResponse> listOwnerPayouts(String ownerEmail);

    PayoutResponse getOwnerPayout(String ownerEmail, String payoutId);

    List<AdminPayoutBusinessResponse> listBusinesses();

    Page<PayoutResponse> listAdminPayouts(
            String businessId, PayoutStatus status, PayoutFrequency frequency, Pageable pageable);

    PayoutResponse getAdminPayout(String payoutId);

    PayoutResponse updateStatus(String adminEmail, String payoutId, AdminPayoutStatusUpdateRequest request);

    PayoutResponse settle(
            String adminEmail, String payoutId, AdminPayoutSettleRequest request, MultipartFile receipt);

    void verifyBank(String adminEmail, String businessId, AdminBankVerificationRequest request);

    PlatformPayoutConfigResponse getConfig();

    PlatformPayoutConfigResponse updateConfig(String adminEmail, AdminPayoutConfigUpdateRequest request);
}
