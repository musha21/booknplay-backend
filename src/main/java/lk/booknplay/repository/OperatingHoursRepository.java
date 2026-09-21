package lk.booknplay.repository;

import lk.booknplay.entity.OperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface OperatingHoursRepository extends JpaRepository<OperatingHours, String> {
    List<OperatingHours> findByVenueId(String venueId);
    Optional<OperatingHours> findByVenueIdAndDayOfWeek(String venueId, DayOfWeek dayOfWeek);
    void deleteByVenueId(String venueId);
}
