package lk.booknplay.repository;

import lk.booknplay.entity.BusinessImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessImageRepository extends JpaRepository<BusinessImage, String> {
    List<BusinessImage> findByBusinessIdOrderBySortOrderAsc(String businessId);
    void deleteByBusinessIdAndLogoFalse(String businessId);
    long countByBusinessId(String businessId);
}
