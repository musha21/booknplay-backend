package lk.booknplay.repository;

import lk.booknplay.entity.BlockedSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BlockedSlotRepository extends JpaRepository<BlockedSlot, String> {
    List<BlockedSlot> findByCourtIdAndDate(String courtId, LocalDate date);

    List<BlockedSlot> findByCourtIdInAndDateBetween(List<String> courtIds, LocalDate fromDate, LocalDate toDate);

    List<BlockedSlot> findByCourtId(String courtId);
}
