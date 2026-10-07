package lk.booknplay.repository;

import lk.booknplay.entity.BookingQuote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingQuoteRepository extends JpaRepository<BookingQuote, String> {
}
