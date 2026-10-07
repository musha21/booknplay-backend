package lk.booknplay.entity;

import jakarta.persistence.*;
import lk.booknplay.enums.PricingRuleType;
import lombok.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "court_pricings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourtPricing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private BigDecimal price;

    /** Null on legacy rows; resolved via {@link lk.booknplay.util.CourtPriceResolver#effectiveType}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", length = 20)
    private PricingRuleType ruleType;

    @Column(name = "priority")
    private Integer priority;

    @Column(length = 80)
    private String label;
}
