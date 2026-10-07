package lk.booknplay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "booking_quotes", indexes = {
        @Index(name = "idx_booking_quote_customer_expiry", columnList = "customer_id,expires_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingQuote {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sport_id", nullable = false)
    private Sport sport;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    /** JSON array of discrete selected hours: [{"startTime":"10:00:00","endTime":"11:00:00"}, ...] */
    @Column(name = "selected_slots", columnDefinition = "TEXT")
    private String selectedSlots;
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "promo_code", length = 40)
    private String promoCode;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;
    @Column(name = "cancellation_allowed", nullable = false)
    private boolean cancellationAllowed;
    @Column(name = "cancellation_deadline")
    private LocalDateTime cancellationDeadline;
    @Column(name = "late_refund_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal lateRefundPercentage;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;
    @Column(name = "booking_id")
    private String bookingId;
}
