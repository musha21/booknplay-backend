package lk.booknplay.repository;

import lk.booknplay.entity.Payout;
import lk.booknplay.enums.PayoutFrequency;
import lk.booknplay.enums.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, String> {

    List<Payout> findByBusinessIdOrderByPeriodStartDesc(String businessId);

    Optional<Payout> findByBusinessIdAndPeriodStartAndPeriodEnd(
            String businessId, LocalDate periodStart, LocalDate periodEnd);

    boolean existsByBusinessIdAndPeriodStartAndPeriodEnd(
            String businessId, LocalDate periodStart, LocalDate periodEnd);

    @Query("""
            SELECT COALESCE(SUM(p.net), 0) FROM Payout p
            WHERE p.business.id = :businessId
              AND p.status IN :statuses
            """)
    BigDecimal sumNetByBusinessAndStatuses(
            @Param("businessId") String businessId,
            @Param("statuses") List<PayoutStatus> statuses);

    @Query("""
            SELECT p FROM Payout p
            WHERE (:businessId IS NULL OR p.business.id = :businessId)
              AND (:status IS NULL OR p.status = :status)
              AND (:frequency IS NULL OR p.frequency = :frequency)
            ORDER BY p.periodStart DESC, p.createdAt DESC
            """)
    Page<Payout> findFiltered(
            @Param("businessId") String businessId,
            @Param("status") PayoutStatus status,
            @Param("frequency") PayoutFrequency frequency,
            Pageable pageable);
}
