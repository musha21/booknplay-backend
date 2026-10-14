package lk.booknplay.dto.response;

import lk.booknplay.enums.BankVerificationStatus;
import lk.booknplay.enums.PayoutFrequency;
import lk.booknplay.enums.PayoutStatus;
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
public class AdminPayoutBusinessResponse {
    private String businessId;
    private String businessName;
    private String ownerEmail;
    private PayoutFrequency payoutFrequency;
    private BankVerificationStatus bankVerificationStatus;
    private String bankName;
    private String bankAccountName;
    private String bankAccountNumberMasked;
    private String bankAccountNumber;
    private String bankBranch;
    /** Sum of open (PENDING/PROCESSING/ON_HOLD) payout nets — ready to transfer. */
    private BigDecimal outstandingBalance;
    /** Net estimate of SUCCESS online payments not yet on any payout row. */
    private BigDecimal unsettledAccrued;
    /** Plan-synced commission used for settlement math (0–100). */
    private BigDecimal commissionPercent;
    private String latestPayoutId;
    private PayoutStatus latestPayoutStatus;
    private LocalDate latestPeriodStart;
    private LocalDate latestPeriodEnd;
    private BigDecimal latestNet;
}
