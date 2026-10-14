package lk.booknplay.entity;

import jakarta.persistence.*;
import lk.booknplay.enums.PayoutFrequency;
import lk.booknplay.enums.PayoutStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payouts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payout_business_period", columnNames = {"business_id", "period_start", "period_end"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayoutFrequency frequency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal gross;

    @Builder.Default
    @Column(name = "refunds_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundsTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal commission;

    @Builder.Default
    @Column(name = "gateway_fees", nullable = false, precision = 12, scale = 2)
    private BigDecimal gatewayFees = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal net;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PayoutStatus status = PayoutStatus.PENDING;

    private String note;

    @Column(name = "transfer_reference")
    private String transferReference;

    @Column(name = "transfer_date")
    private LocalDate transferDate;

    @Column(name = "transfer_amount", precision = 12, scale = 2)
    private BigDecimal transferAmount;

    @Column(name = "receipt_url")
    private String receiptUrl;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "paid_by_admin_email")
    private String paidByAdminEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (refundsTotal == null) {
            refundsTotal = BigDecimal.ZERO;
        }
        if (gatewayFees == null) {
            gatewayFees = BigDecimal.ZERO;
        }
        if (status == null) {
            status = PayoutStatus.PENDING;
        }
    }
}
