package lk.booknplay.repository;

import lk.booknplay.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, String> {
    List<Promotion> findByBusinessIdOrderByCreatedAtDesc(String businessId);

    Optional<Promotion> findByIdAndBusinessId(String id, String businessId);

    Optional<Promotion> findByBusinessIdAndCodeIgnoreCase(String businessId, String code);

    boolean existsByBusinessIdAndCodeIgnoreCase(String businessId, String code);
}
