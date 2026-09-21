package lk.booknplay.repository;

import lk.booknplay.entity.Sport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportRepository extends JpaRepository<Sport, String> {
    List<Sport> findByIsActiveTrue();
    Optional<Sport> findByNameIgnoreCase(String name);
}
