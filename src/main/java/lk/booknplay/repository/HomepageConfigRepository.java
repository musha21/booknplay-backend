package lk.booknplay.repository;

import lk.booknplay.entity.HomepageConfig;
import lk.booknplay.enums.HomepageConfigStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HomepageConfigRepository extends JpaRepository<HomepageConfig, String> {
    Optional<HomepageConfig> findFirstByStatusOrderByVersionDesc(HomepageConfigStatus status);
    List<HomepageConfig> findAllByOrderByVersionDesc();
}
