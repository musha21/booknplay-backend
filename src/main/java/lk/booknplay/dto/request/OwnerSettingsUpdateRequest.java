package lk.booknplay.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerSettingsUpdateRequest {
    private String bankName;
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankBranch;
    private Boolean notifyBookingEmail;
    private Boolean notifyBookingSms;
    private Boolean notifyPaymentEmail;
    private Boolean notifyTrialEmail;
    private String contactPhone;
    private String contactEmail;
}
