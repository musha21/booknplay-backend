package lk.booknplay.repository;

import lk.booknplay.entity.SubscriptionPlan;
import lk.booknplay.enums.PlanCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, String> {
    Optional<SubscriptionPlan> findByCode(PlanCode code);

    List<SubscriptionPlan> findByActiveTrueOrderBySortOrderAsc();
}
