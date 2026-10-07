package lk.booknplay.dto.response;

import lk.booknplay.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerPaymentResponse {
    private String id;
    private String bookingId;
    private String bookingRef;
    private String venueId;
    private String venueName;
    private String customerName;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String paymentGateway;
    private String gatewayReference;
    private LocalDate bookingDate;
    private LocalDateTime createdAt;
}
