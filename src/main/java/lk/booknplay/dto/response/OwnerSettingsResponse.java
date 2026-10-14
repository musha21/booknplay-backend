package lk.booknplay.dto.response;

import lk.booknplay.enums.BankVerificationStatus;
import lk.booknplay.enums.PayoutFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerSettingsResponse {
    private String businessId;
    private String businessName;
    private String contactEmail;
    private String contactPhone;
    private String bankName;
    private String bankAccountName;
    /** Masked for display (****1234). Full number is never returned on GET. */
    private String bankAccountNumberMasked;
    private String bankBranch;
    private PayoutFrequency payoutFrequency;
    private BankVerificationStatus bankVerificationStatus;
    private boolean notifyBookingEmail;
    private boolean notifyBookingSms;
    private boolean notifyPaymentEmail;
    private boolean notifyTrialEmail;
}
