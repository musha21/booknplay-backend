package lk.booknplay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout_items", indexes = {
        @Index(name = "idx_payout_item_payment", columnList = "payment_id"),
        @Index(name = "idx_payout_item_payout", columnList = "payout_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payout_id", nullable = false)
    private Payout payout;

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Column(name = "booking_id", nullable = false)
    private String bookingId;

    @Column(name = "refund_id")
    private String refundId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal gross;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal refund = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal commission;

    @Builder.Default
    @Column(name = "gateway_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal gatewayFee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal net;

    @Builder.Default
    @Column(name = "adjustment", nullable = false)
    private boolean adjustment = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (refund == null) {
            refund = BigDecimal.ZERO;
        }
        if (gatewayFee == null) {
            gatewayFee = BigDecimal.ZERO;
        }
    }
}
