package lk.booknplay.repository;

import lk.booknplay.entity.PlatformPayoutConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformPayoutConfigRepository extends JpaRepository<PlatformPayoutConfig, String> {
}
