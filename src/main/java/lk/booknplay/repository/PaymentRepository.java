package lk.booknplay.repository;

import jakarta.persistence.LockModeType;
import lk.booknplay.entity.Payment;
import lk.booknplay.enums.BookingSource;
import lk.booknplay.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByBookingId(String bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.booking.id = :bookingId")
    Optional<Payment> findByBookingIdForUpdate(@Param("bookingId") String bookingId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = lk.booknplay.enums.PaymentStatus.SUCCESS")
    BigDecimal sumSuccessfulPayments();

    @Query("SELECT p FROM Payment p WHERE p.booking.venue.business.id = :businessId " +
           "AND (:fromDate IS NULL OR p.booking.bookingDate >= :fromDate) " +
           "AND (:toDate IS NULL OR p.booking.bookingDate <= :toDate) " +
           "AND (:venueId IS NULL OR p.booking.venue.id = :venueId) " +
           "ORDER BY p.createdAt DESC")
    Page<Payment> findForOwner(
            @Param("businessId") String businessId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("venueId") String venueId,
            Pageable pageable
    );

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.booking b
            JOIN FETCH b.venue v
            WHERE v.business.id = :businessId
              AND b.source = :source
              AND p.status = :status
              AND p.updatedAt >= :fromInclusive
              AND p.updatedAt < :toExclusive
            ORDER BY p.updatedAt ASC
            """)
    List<Payment> findEligibleForPayout(
            @Param("businessId") String businessId,
            @Param("source") BookingSource source,
            @Param("status") PaymentStatus status,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive
    );

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.booking b
            JOIN FETCH b.venue v
            WHERE v.business.id = :businessId
              AND b.source = :source
              AND p.status = :status
            ORDER BY p.updatedAt ASC
            """)
    List<Payment> findByBusinessSourceAndStatus(
            @Param("businessId") String businessId,
            @Param("source") BookingSource source,
            @Param("status") PaymentStatus status
    );
}
