package lk.booknplay.repository;

import lk.booknplay.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByBookingId(String bookingId);
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = lk.booknplay.enums.PaymentStatus.SUCCESS")
    BigDecimal sumSuccessfulPayments();
}
