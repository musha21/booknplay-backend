package lk.booknplay.repository;

import lk.booknplay.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, String> {
    List<Promotion> findByBusinessIdOrderByCreatedAtDesc(String businessId);

    @Query("SELECT p FROM Promotion p WHERE p.active = true AND p.startDate <= :today AND p.endDate >= :today "
            + "ORDER BY p.startDate ASC")
    List<Promotion> findActivePublic(@Param("today") LocalDate today);

    Optional<Promotion> findByIdAndBusinessId(String id, String businessId);

    Optional<Promotion> findByBusinessIdAndCodeIgnoreCase(String businessId, String code);

    boolean existsByBusinessIdAndCodeIgnoreCase(String businessId, String code);
}
