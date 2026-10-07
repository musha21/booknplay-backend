package lk.booknplay.service;

import lk.booknplay.dto.request.BookingCreateRequest;
import lk.booknplay.dto.request.BookingConfirmRequest;
import lk.booknplay.dto.response.BookingCheckoutResponse;
import lk.booknplay.dto.response.BookingCancellationResponse;
import lk.booknplay.dto.response.BookingQuoteResponse;
import lk.booknplay.dto.response.BookingResponse;
import lk.booknplay.dto.response.CancellationPreviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {
    BookingCheckoutResponse createBooking(String customerEmail, BookingConfirmRequest request, String idempotencyKey);
    BookingQuoteResponse quoteBooking(String customerEmail, BookingCreateRequest request);
    BookingResponse getBookingById(String customerEmail, String bookingId);
    Page<BookingResponse> getMyBookings(String customerEmail, Pageable pageable);
    Page<BookingResponse> getUpcomingBookings(String customerEmail, Pageable pageable);
    Page<BookingResponse> getBookingHistory(String customerEmail, Pageable pageable);
    CancellationPreviewResponse previewCancellation(String customerEmail, String bookingId);
    BookingCancellationResponse cancelBooking(String customerEmail, String bookingId);
    BookingCancellationResponse cancelBookingAsOwner(String bookingId, String reason);
}
