package lk.booknplay.dto.request;

import lk.booknplay.enums.PayoutStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPayoutStatusUpdateRequest {
    private PayoutStatus status;
    private String reason;
}
