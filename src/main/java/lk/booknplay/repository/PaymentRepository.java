package lk.booknplay.repository;

import lk.booknplay.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByBookingId(String bookingId);

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
}
