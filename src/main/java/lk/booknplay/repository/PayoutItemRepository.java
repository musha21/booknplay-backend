package lk.booknplay.repository;

import lk.booknplay.entity.PayoutItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface PayoutItemRepository extends JpaRepository<PayoutItem, String> {

    List<PayoutItem> findByPayoutIdOrderByCreatedAtAsc(String payoutId);

    boolean existsByPaymentIdAndAdjustmentFalse(String paymentId);

    boolean existsByRefundId(String refundId);

    @Query("SELECT pi.paymentId FROM PayoutItem pi WHERE pi.adjustment = false AND pi.paymentId IN :paymentIds")
    Set<String> findSettledPaymentIds(@Param("paymentIds") Collection<String> paymentIds);

    @Query("""
            SELECT pi FROM PayoutItem pi
            JOIN FETCH pi.payout p
            WHERE pi.adjustment = false
              AND pi.paymentId = :paymentId
              AND p.status = lk.booknplay.enums.PayoutStatus.PAID
            """)
    List<PayoutItem> findPaidPrimaryItemsByPaymentId(@Param("paymentId") String paymentId);
}
