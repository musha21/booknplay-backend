package lk.booknplay.repository;

import lk.booknplay.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {
    Page<Review> findByVenueIdOrderByCreatedAtDesc(String venueId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.venue.business.id = :businessId " +
           "AND (:venueId IS NULL OR r.venue.id = :venueId) " +
           "ORDER BY r.createdAt DESC")
    Page<Review> findForOwner(
            @Param("businessId") String businessId,
            @Param("venueId") String venueId,
            Pageable pageable
    );
}
