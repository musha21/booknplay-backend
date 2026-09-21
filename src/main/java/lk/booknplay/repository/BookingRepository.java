package lk.booknplay.repository;

import lk.booknplay.entity.Booking;
import lk.booknplay.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    List<Booking> findByCourtIdAndBookingDateAndStatusNot(String courtId, LocalDate bookingDate, BookingStatus status);

    List<Booking> findByCourtIdInAndBookingDateBetweenAndStatusNot(
            List<String> courtIds, LocalDate fromDate, LocalDate toDate, BookingStatus status);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.court.id = :courtId " +
           "AND b.bookingDate = :bookingDate " +
           "AND b.status != 'CANCELLED' " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    boolean existsOverlappingBooking(
            @Param("courtId") String courtId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    Page<Booking> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);

    Page<Booking> findByCustomerIdAndBookingDateGreaterThanEqualAndStatusOrderByBookingDateAscStartTimeAsc(
            String customerId, LocalDate bookingDate, BookingStatus status, Pageable pageable
    );

    Page<Booking> findByCustomerIdAndBookingDateLessThanAndStatusInOrderByBookingDateDescStartTimeDesc(
            String customerId, LocalDate bookingDate, List<BookingStatus> statuses, Pageable pageable
    );

    @Query("SELECT b FROM Booking b WHERE b.venue.business.id = :businessId " +
           "AND b.bookingDate >= :fromDate AND b.bookingDate <= :toDate " +
           "AND b.status IN :statuses")
    List<Booking> findForEarnings(
            @Param("businessId") String businessId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") List<BookingStatus> statuses
    );

    List<Booking> findByVenueIdAndBookingDate(String venueId, LocalDate bookingDate);
}
