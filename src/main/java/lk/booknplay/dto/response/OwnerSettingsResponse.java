package lk.booknplay.dto.response;

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
    private String bankAccountNumber;
    private String bankBranch;
    private boolean notifyBookingEmail;
    private boolean notifyBookingSms;
    private boolean notifyPaymentEmail;
    private boolean notifyTrialEmail;
}
