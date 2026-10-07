package lk.booknplay.repository;

import lk.booknplay.entity.BusinessStaff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessStaffRepository extends JpaRepository<BusinessStaff, String> {
    List<BusinessStaff> findByBusinessIdOrderByCreatedAtDesc(String businessId);

    long countByBusinessIdAndActiveTrue(String businessId);

    Optional<BusinessStaff> findByUserId(String userId);

    Optional<BusinessStaff> findByIdAndBusinessId(String id, String businessId);

    boolean existsByBusinessIdAndEmailIgnoreCase(String businessId, String email);
}
