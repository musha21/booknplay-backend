package lk.booknplay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lk.booknplay.enums.BillingInterval;
import lk.booknplay.enums.PlanCode;
import lombok.Data;

@Data
public class SubscriptionCheckoutRequest {
    @NotNull
    private PlanCode planCode;

    @NotNull
    private BillingInterval billingInterval;

    /** Optional; defaults to configured payments mode / PAYHERE. */
    private String gateway;
}
