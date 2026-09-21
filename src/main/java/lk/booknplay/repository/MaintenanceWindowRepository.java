package lk.booknplay.repository;

import lk.booknplay.entity.MaintenanceWindow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MaintenanceWindowRepository extends JpaRepository<MaintenanceWindow, String> {

    @Query("SELECT m FROM MaintenanceWindow m WHERE m.court.id = :courtId AND " +
           "m.startDateTime < :endDateTime AND m.endDateTime > :startDateTime")
    List<MaintenanceWindow> findOverlappingMaintenance(
            @Param("courtId") String courtId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query("SELECT m FROM MaintenanceWindow m WHERE m.court.id IN :courtIds AND " +
           "m.startDateTime < :endDateTime AND m.endDateTime > :startDateTime")
    List<MaintenanceWindow> findOverlappingMaintenanceForCourts(
            @Param("courtIds") List<String> courtIds,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    List<MaintenanceWindow> findByCourtId(String courtId);
}
