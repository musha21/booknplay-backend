package lk.booknplay.repository;

import lk.booknplay.entity.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, String> {
    List<Payout> findByBusinessIdOrderByPeriodStartDesc(String businessId);
}
