package lk.booknplay.repository;

import jakarta.persistence.LockModeType;
import lk.booknplay.entity.Court;
import lk.booknplay.enums.CourtStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourtRepository extends JpaRepository<Court, String> {

    List<Court> findByVenueIdAndStatus(String venueId, CourtStatus status);

    @Query("SELECT c FROM Court c JOIN FETCH c.sport JOIN FETCH c.venue WHERE c.venue.id = :venueId")
    List<Court> findByVenueId(@Param("venueId") String venueId);

    @Query("SELECT c FROM Court c JOIN FETCH c.sport JOIN FETCH c.venue " +
           "WHERE c.venue.id = :venueId AND c.status <> :deleted ORDER BY c.id")
    List<Court> findCalendarCourts(
            @Param("venueId") String venueId,
            @Param("deleted") CourtStatus deleted,
            Pageable pageable);

    @Query("SELECT c FROM Court c JOIN FETCH c.sport JOIN FETCH c.venue " +
           "WHERE c.id = :courtId AND c.venue.id = :venueId AND c.status <> :deleted")
    Optional<Court> findCalendarCourt(
            @Param("courtId") String courtId,
            @Param("venueId") String venueId,
            @Param("deleted") CourtStatus deleted);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Court c WHERE c.id = :id")
    Optional<Court> findByIdWithLock(@Param("id") String id);
}
