package lk.booknplay.repository;

import lk.booknplay.entity.OtpVerification;
import lk.booknplay.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, String> {

    Optional<OtpVerification> findFirstByPhoneAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
            String phone, OtpPurpose purpose);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE OtpVerification o SET o.verified = true WHERE o.phone = :phone AND o.purpose = :purpose AND o.verified = false")
    int invalidateActiveOtps(@Param("phone") String phone, @Param("purpose") OtpPurpose purpose);
}
