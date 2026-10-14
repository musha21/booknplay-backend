package lk.booknplay.entity;

import jakarta.persistence.*;
import lk.booknplay.enums.CourtStatus;
import lk.booknplay.enums.FacilityType;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "courts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sport_id", nullable = false)
    private Sport sport;

    @Column(nullable = false)
    private String name;

    @Column(name = "hourly_rate", nullable = false)
    private BigDecimal hourlyRate;

    @Builder.Default
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 60;

    /**
     * Indoor/Outdoor classification. Nullable at the database level until legacy
     * rows are backfilled and confirmed by owners; required for new/updated courts.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "facility_type", length = 20)
    private FacilityType facilityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CourtStatus status = CourtStatus.ACTIVE;
}
