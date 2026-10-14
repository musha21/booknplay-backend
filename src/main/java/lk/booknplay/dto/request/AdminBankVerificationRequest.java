package lk.booknplay.dto.request;

import lk.booknplay.enums.BankVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBankVerificationRequest {
    private BankVerificationStatus status;
    private String reason;
}
