package lk.booknplay.repository;

import lk.booknplay.entity.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, String> {
    Optional<Refund> findByBookingId(String bookingId);

    @Query("SELECT r FROM Refund r WHERE r.booking.venue.business.id = :businessId ORDER BY r.id DESC")
    Page<Refund> findForOwner(@Param("businessId") String businessId, Pageable pageable);

    @Query("SELECT r FROM Refund r WHERE r.payment.id IN :paymentIds AND UPPER(r.status) = 'SUCCEEDED'")
    List<Refund> findSucceededByPaymentIds(@Param("paymentIds") Collection<String> paymentIds);

    Optional<Refund> findByPaymentId(String paymentId);

    @Query("""
            SELECT r FROM Refund r
            JOIN FETCH r.payment
            JOIN FETCH r.booking b
            JOIN FETCH b.venue v
            WHERE v.business.id = :businessId
              AND UPPER(r.status) = 'SUCCEEDED'
              AND r.processedAt >= :fromInclusive
              AND r.processedAt < :toExclusive
            """)
    List<Refund> findSucceededForBusinessInWindow(
            @Param("businessId") String businessId,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive);
}
