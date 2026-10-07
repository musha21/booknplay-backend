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
    List<Booking> findByCourtIdInAndBookingDateBetweenAndStatus(
            List<String> courtIds, LocalDate fromDate, LocalDate toDate, BookingStatus status);

    /**
     * Prefers discrete {@code BookingSlot} rows when present so gapped bookings
     * only block the hours that were actually reserved. Falls back to the booking
     * envelope for legacy rows without slot children.
     * Soft-holds ({@code PENDING}) and confirmed bookings both block the court.
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b LEFT JOIN b.slots s " +
           "WHERE b.court.id = :courtId " +
           "AND b.bookingDate = :bookingDate " +
           "AND b.status IN (lk.booknplay.enums.BookingStatus.PENDING, lk.booknplay.enums.BookingStatus.CONFIRMED) " +
           "AND ((s.id IS NOT NULL AND s.startTime < :endTime AND s.endTime > :startTime) " +
           "OR (s.id IS NULL AND b.startTime < :endTime AND b.endTime > :startTime))")
    boolean existsOverlappingBooking(
            @Param("courtId") String courtId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /**
     * Same as {@link #existsOverlappingBooking} but ignores one booking (used when confirming payment).
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b LEFT JOIN b.slots s " +
           "WHERE b.court.id = :courtId " +
           "AND b.bookingDate = :bookingDate " +
           "AND b.id <> :excludeBookingId " +
           "AND b.status IN (lk.booknplay.enums.BookingStatus.PENDING, lk.booknplay.enums.BookingStatus.CONFIRMED) " +
           "AND ((s.id IS NOT NULL AND s.startTime < :endTime AND s.endTime > :startTime) " +
           "OR (s.id IS NULL AND b.startTime < :endTime AND b.endTime > :startTime))")
    boolean existsOverlappingBookingExcluding(
            @Param("courtId") String courtId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeBookingId") String excludeBookingId
    );

    @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.slots " +
           "WHERE b.court.id IN :courtIds AND b.bookingDate BETWEEN :fromDate AND :toDate AND b.status = :status")
    List<Booking> findWithSlotsByCourtIdInAndBookingDateBetweenAndStatus(
            @Param("courtIds") List<String> courtIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("status") BookingStatus status
    );

    @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.slots " +
           "WHERE b.court.id IN :courtIds AND b.bookingDate BETWEEN :fromDate AND :toDate AND b.status IN :statuses")
    List<Booking> findWithSlotsByCourtIdInAndBookingDateBetweenAndStatusIn(
            @Param("courtIds") List<String> courtIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") List<BookingStatus> statuses
    );

    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, java.time.LocalDateTime createdAt);

    @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.slots " +
           "WHERE b.court.id IN :courtIds AND b.bookingDate BETWEEN :fromDate AND :toDate AND b.status <> :status")
    List<Booking> findWithSlotsByCourtIdInAndBookingDateBetweenAndStatusNot(
            @Param("courtIds") List<String> courtIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("status") BookingStatus status
    );

    java.util.Optional<Booking> findByCustomerIdAndIdempotencyKey(String customerId, String idempotencyKey);

    java.util.Optional<Booking> findByBookingRef(String bookingRef);

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

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
           "WHERE b.venue.id = :venueId AND b.status IN :statuses AND " +
           "(b.bookingDate > :today OR (b.bookingDate = :today AND b.endTime > :now))")
    boolean existsFutureBooking(
            @Param("venueId") String venueId,
            @Param("statuses") List<BookingStatus> statuses,
            @Param("today") LocalDate today,
            @Param("now") LocalTime now
    );

    @Query("SELECT b FROM Booking b " +
           "WHERE b.venue.business.id = :businessId " +
           "AND (:fromDate IS NULL OR b.bookingDate >= :fromDate) " +
           "AND (:toDate IS NULL OR b.bookingDate <= :toDate) " +
           "AND (:venueId IS NULL OR b.venue.id = :venueId) " +
           "AND (:courtId IS NULL OR b.court.id = :courtId) " +
           "AND (:status IS NULL OR b.status = :status) " +
           "AND (:q IS NULL OR LOWER(b.bookingRef) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR LOWER(COALESCE(b.guestName, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR LOWER(COALESCE(b.guestPhone, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR LOWER(COALESCE(b.contactName, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR LOWER(COALESCE(b.contactPhone, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR LOWER(COALESCE(b.customer.firstName, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR LOWER(COALESCE(b.customer.lastName, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR LOWER(COALESCE(b.customer.phone, '')) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY b.bookingDate DESC, b.startTime DESC")
    Page<Booking> findForOwner(
            @Param("businessId") String businessId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("venueId") String venueId,
            @Param("courtId") String courtId,
            @Param("status") BookingStatus status,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.slots "
            + "WHERE b.id = :id AND b.venue.business.id = :businessId")
    java.util.Optional<Booking> findByIdAndBusinessId(
            @Param("id") String id,
            @Param("businessId") String businessId
    );

    @Query("SELECT DISTINCT b FROM Booking b "
            + "LEFT JOIN FETCH b.slots "
            + "LEFT JOIN FETCH b.venue "
            + "LEFT JOIN FETCH b.court "
            + "LEFT JOIN FETCH b.sport "
            + "WHERE b.id = :id")
    java.util.Optional<Booking> findByIdWithSlots(@Param("id") String id);

    @Query("SELECT b FROM Booking b WHERE b.venue.business.id = :businessId " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findRecentByBusinessId(@Param("businessId") String businessId, Pageable pageable);
}
