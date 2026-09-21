package lk.booknplay.repository;

import lk.booknplay.entity.Venue;
import lk.booknplay.enums.VenueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueRepository extends JpaRepository<Venue, String> {

    @Query("SELECT v FROM Venue v " +
           "WHERE v.status IN :statuses " +
           "AND (:city IS NULL OR LOWER(v.city) LIKE LOWER(CONCAT('%', :city, '%')) " +
           "     OR LOWER(COALESCE(v.formattedAddress, '')) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "AND (:name IS NULL OR LOWER(v.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (:sportId IS NULL OR EXISTS (" +
           "     SELECT 1 FROM Court c WHERE c.venue.id = v.id AND c.sport.id = :sportId" +
           "))")
    Page<Venue> searchVenues(
            @Param("statuses") List<VenueStatus> statuses,
            @Param("city") String city,
            @Param("sportId") String sportId,
            @Param("name") String name,
            Pageable pageable
    );

    List<Venue> findByBusinessId(String businessId);
    List<Venue> findByStatus(VenueStatus status);
    long countByStatus(VenueStatus status);

    @Modifying
    @Query("UPDATE Venue v SET v.status = :active WHERE v.status IN :fromStatuses "
            + "AND EXISTS (SELECT 1 FROM Court c WHERE c.venue.id = v.id)")
    int activateVenuesWithCourts(
            @Param("active") VenueStatus active,
            @Param("fromStatuses") List<VenueStatus> fromStatuses
    );
}
