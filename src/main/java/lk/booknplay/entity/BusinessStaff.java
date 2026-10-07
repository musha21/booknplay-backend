package lk.booknplay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "business_staff", uniqueConstraints = {
        @UniqueConstraint(name = "uk_business_staff_user", columnNames = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "can_calendar", nullable = false)
    private boolean canCalendar = true;

    @Builder.Default
    @Column(name = "can_walk_ins", nullable = false)
    private boolean canWalkIns = true;

    @Builder.Default
    @Column(name = "can_courts", nullable = false)
    private boolean canCourts = true;

    @Builder.Default
    @Column(name = "can_reports", nullable = false)
    private boolean canReports = false;

    @Builder.Default
    @Column(name = "can_earnings", nullable = false)
    private boolean canEarnings = false;

    @Builder.Default
    @Column(name = "can_venues", nullable = false)
    private boolean canVenues = false;

    @Builder.Default
    @Column(name = "can_billing", nullable = false)
    private boolean canBilling = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
