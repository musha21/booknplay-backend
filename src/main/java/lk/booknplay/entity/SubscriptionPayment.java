package lk.booknplay.entity;

import jakarta.persistence.*;
import lk.booknplay.enums.BillingInterval;
import lk.booknplay.enums.PaymentStatus;
import lk.booknplay.enums.PlanCode;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_code", nullable = false, length = 32)
    private PlanCode planCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false, length = 16)
    private BillingInterval billingInterval;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 8)
    @Builder.Default
    private String currency = "LKR";

    @Column(name = "payment_gateway")
    private String paymentGateway;

    @Column(name = "gateway_reference")
    private String gatewayReference;

    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "checkout_token")
    private String checkoutToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
