package lk.booknplay.repository;

import lk.booknplay.entity.SubscriptionPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, String> {
    Optional<SubscriptionPayment> findByOrderId(String orderId);

    Optional<SubscriptionPayment> findByIdAndBusinessId(String id, String businessId);
}
