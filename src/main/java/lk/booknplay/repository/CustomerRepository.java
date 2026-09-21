package lk.booknplay.repository;

import lk.booknplay.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    Optional<Customer> findByUserId(String userId);
    Optional<Customer> findByUser_Email(String email);
    Optional<Customer> findByPhone(String phone);
    boolean existsByPhone(String phone);

    @Query("SELECT c FROM Customer c WHERE " +
           "(:query IS NULL OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.user.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "c.phone LIKE CONCAT('%', :query, '%'))")
    Page<Customer> searchCustomers(@Param("query") String query, Pageable pageable);
}
