package lk.booknplay.dto.response;

import lk.booknplay.enums.BankVerificationStatus;
import lk.booknplay.enums.PayoutFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerPayoutSummaryResponse {
    private BigDecimal outstandingBalance;
    private LocalDate upcomingPeriodStart;
    private LocalDate upcomingPeriodEnd;
    private PayoutFrequency payoutFrequency;
    private BankVerificationStatus bankVerificationStatus;
    private String bankName;
    private String bankAccountName;
    private String bankAccountNumberMasked;
    private String bankBranch;
}
