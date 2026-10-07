package lk.booknplay.entity;

import jakarta.persistence.*;
import lk.booknplay.enums.PlanCode;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 32)
    private PlanCode code;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "price_monthly", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceMonthly;

    @Column(name = "price_yearly", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceYearly;

    @Column(nullable = false, length = 8)
    @Builder.Default
    private String currency = "LKR";

    @Column(nullable = false)
    @Builder.Default
    private boolean highlighted = false;

    /** Comma-separated marketing bullets. */
    @Column(length = 1000)
    private String features;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Null = unlimited. */
    @Column(name = "max_venues")
    private Integer maxVenues;

    /** Null = unlimited. */
    @Column(name = "max_courts_per_venue")
    private Integer maxCourtsPerVenue;

    @Column(name = "calendar_enabled", nullable = false)
    @Builder.Default
    private boolean calendarEnabled = true;

    @Column(name = "walk_in_enabled", nullable = false)
    @Builder.Default
    private boolean walkInEnabled = true;

    @Column(name = "earnings_enabled", nullable = false)
    @Builder.Default
    private boolean earningsEnabled = true;

    @Column(name = "reports_enabled", nullable = false)
    @Builder.Default
    private boolean reportsEnabled = true;

    @Column(name = "advanced_reports_enabled", nullable = false)
    @Builder.Default
    private boolean advancedReportsEnabled = false;

    @Column(name = "promotions_enabled", nullable = false)
    @Builder.Default
    private boolean promotionsEnabled = false;

    /** Platform booking commission share (0–100). Synced onto Business when plan is assigned. */
    @Column(name = "commission_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal commissionPercent = new BigDecimal("10.00");

    /** Max active staff seats for the business. Null = unlimited. */
    @Column(name = "max_staff")
    private Integer maxStaff;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
