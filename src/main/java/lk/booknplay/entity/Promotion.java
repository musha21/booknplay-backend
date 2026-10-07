package lk.booknplay.entity;

import jakarta.persistence.*;
import lk.booknplay.enums.PromotionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_promotion_business_code", columnNames = {"business_id", "code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PromotionType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Comma-separated venue IDs; blank = all venues. */
    @Column(name = "venue_ids", length = 1000)
    private String venueIds;

    /** Comma-separated sport IDs; blank = all sports. */
    @Column(name = "sport_ids", length = 1000)
    private String sportIds;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Builder.Default
    @Column(name = "redemption_count", nullable = false)
    private int redemptionCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
