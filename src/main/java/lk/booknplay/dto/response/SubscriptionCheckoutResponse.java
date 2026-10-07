package lk.booknplay.dto.response;

import lk.booknplay.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionCheckoutResponse {
    private String paymentId;
    private String paymentUrl;
    private PaymentStatus status;
    private String paymentGateway;
}
