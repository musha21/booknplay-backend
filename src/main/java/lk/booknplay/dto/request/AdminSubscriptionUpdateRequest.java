package lk.booknplay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lk.booknplay.enums.PlanCode;
import lk.booknplay.enums.SubscriptionStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminSubscriptionUpdateRequest {
    private PlanCode planCode;
    private SubscriptionStatus status;
    private LocalDateTime trialEndsAt;

    @NotBlank
    private String reason;
}
