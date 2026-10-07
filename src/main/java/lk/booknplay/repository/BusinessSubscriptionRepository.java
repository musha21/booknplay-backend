package lk.booknplay.repository;

import lk.booknplay.entity.BusinessSubscription;
import lk.booknplay.enums.PlanCode;
import lk.booknplay.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessSubscriptionRepository extends JpaRepository<BusinessSubscription, String> {
    Optional<BusinessSubscription> findByBusinessId(String businessId);

    boolean existsByBusinessId(String businessId);

    long countByStatus(SubscriptionStatus status);

    long countByPlanCode(PlanCode planCode);

    long countByPlanCodeAndStatus(PlanCode planCode, SubscriptionStatus status);
}
