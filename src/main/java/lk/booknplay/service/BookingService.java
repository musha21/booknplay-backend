package lk.booknplay.service;

import lk.booknplay.dto.request.BookingCreateRequest;
import lk.booknplay.dto.response.BookingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {
    BookingResponse createBooking(String customerEmail, BookingCreateRequest request);
    BookingResponse getBookingById(String customerEmail, String bookingId);
    Page<BookingResponse> getMyBookings(String customerEmail, Pageable pageable);
    Page<BookingResponse> getUpcomingBookings(String customerEmail, Pageable pageable);
    Page<BookingResponse> getBookingHistory(String customerEmail, Pageable pageable);
    BookingResponse cancelBooking(String customerEmail, String bookingId);
}
