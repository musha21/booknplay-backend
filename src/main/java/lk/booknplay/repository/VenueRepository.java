package lk.booknplay.repository;

import lk.booknplay.entity.Venue;
import lk.booknplay.enums.FacilityType;
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
           "AND (:businessId IS NULL OR v.business.id = :businessId) " +
           "AND (:city IS NULL OR LOWER(v.city) LIKE LOWER(CONCAT('%', :city, '%')) " +
           "     OR LOWER(COALESCE(v.formattedAddress, '')) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "AND (:name IS NULL OR LOWER(v.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (" +
           "     (:sportId IS NULL AND :facilityType IS NULL) " +
           "     OR EXISTS (" +
           "         SELECT 1 FROM Court c WHERE c.venue.id = v.id " +
           "         AND c.status = lk.booknplay.enums.CourtStatus.ACTIVE " +
           "         AND (:sportId IS NULL OR c.sport.id = :sportId) " +
           "         AND (:facilityType IS NULL OR c.facilityType = :facilityType)" +
           "     )" +
           ")")
    Page<Venue> searchVenues(
            @Param("statuses") List<VenueStatus> statuses,
            @Param("businessId") String businessId,
            @Param("city") String city,
            @Param("sportId") String sportId,
            @Param("facilityType") FacilityType facilityType,
            @Param("name") String name,
            Pageable pageable
    );

    List<Venue> findByBusinessId(String businessId);
    List<Venue> findByBusinessIdAndStatusNot(String businessId, VenueStatus status);
    List<Venue> findByBusinessIdAndStatus(String businessId, VenueStatus status);
    List<Venue> findByBusinessIdAndStatusIn(String businessId, List<VenueStatus> statuses);
    List<Venue> findByBusinessIdAndStatusAndArchivedBySubscriptionTrue(String businessId, VenueStatus status);
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
