package lk.booknplay.repository;

import lk.booknplay.entity.VenueImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueImageRepository extends JpaRepository<VenueImage, String> {
    List<VenueImage> findByVenueIdOrderBySortOrderAsc(String venueId);
    void deleteByVenueId(String venueId);
}
