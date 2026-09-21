package lk.booknplay.repository;

import lk.booknplay.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import lk.booknplay.enums.VenueStatus;
import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessRepository extends JpaRepository<Business, String> {
    Optional<Business> findByOwnerId(String ownerId);

    @Query("SELECT DISTINCT b FROM Business b, Venue v, User u WHERE v.business = b AND u.id = b.ownerId " +
            "AND v.status IN :statuses AND u.isEnabled = true AND u.isLocked = false ORDER BY b.createdAt DESC")
    List<Business> findPublicBusinesses(@Param("statuses") List<VenueStatus> statuses);
}
