package lk.booknplay.repository;

import lk.booknplay.entity.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, String> {
    Optional<Refund> findByBookingId(String bookingId);

    @Query("SELECT r FROM Refund r WHERE r.booking.venue.business.id = :businessId ORDER BY r.id DESC")
    Page<Refund> findForOwner(@Param("businessId") String businessId, Pageable pageable);
}
