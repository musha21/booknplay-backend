package lk.booknplay.repository;

import lk.booknplay.entity.BookingSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingSlotRepository extends JpaRepository<BookingSlot, String> {
    List<BookingSlot> findByBookingId(String bookingId);
}
