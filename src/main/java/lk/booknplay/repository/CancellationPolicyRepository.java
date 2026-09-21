package lk.booknplay.repository;

import lk.booknplay.entity.CancellationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, String> {
    Optional<CancellationPolicy> findByBusinessId(String businessId);
}
