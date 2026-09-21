package lk.booknplay.repository;

import lk.booknplay.entity.CourtPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface CourtPricingRepository extends JpaRepository<CourtPricing, String> {
    List<CourtPricing> findByCourtIdAndDayOfWeek(String courtId, DayOfWeek dayOfWeek);

    List<CourtPricing> findByCourtIdInAndDayOfWeek(List<String> courtIds, DayOfWeek dayOfWeek);

    List<CourtPricing> findByCourtId(String courtId);

    void deleteByCourtId(String courtId);
}
