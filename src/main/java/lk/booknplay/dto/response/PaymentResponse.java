package lk.booknplay.dto.response;

import lk.booknplay.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private String id;
    private String bookingId;
    private String bookingRef;
    private BigDecimal amount;
    private String currency;
    private String paymentGateway;
    private String gatewayReference;
    private PaymentStatus status;
    private String paymentUrl;
    private LocalDateTime createdAt;
}
