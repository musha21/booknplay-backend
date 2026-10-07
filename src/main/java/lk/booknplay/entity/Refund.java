package lk.booknplay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds", uniqueConstraints = {
        @UniqueConstraint(name = "uk_refund_booking", columnNames = "booking_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 20)
    private String mode;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(nullable = false, unique = true)
    private String reference;
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}
