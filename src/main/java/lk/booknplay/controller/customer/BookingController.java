package lk.booknplay.controller.customer;

import jakarta.validation.Valid;
import lk.booknplay.dto.request.BookingCreateRequest;
import lk.booknplay.dto.request.BookingConfirmRequest;
import lk.booknplay.dto.response.BookingCheckoutResponse;
import lk.booknplay.dto.response.BookingCancellationResponse;
import lk.booknplay.dto.response.BookingQuoteResponse;
import lk.booknplay.dto.response.BookingResponse;
import lk.booknplay.dto.response.CancellationPreviewResponse;
import lk.booknplay.service.BookingService;
import lk.booknplay.util.ApiResponse;
import lk.booknplay.util.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingCheckoutResponse>> createBooking(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody BookingConfirmRequest request) {
        BookingCheckoutResponse response = bookingService.createBooking(authentication.getName(), request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", response));
    }

    @PostMapping("/quote")
    public ResponseEntity<ApiResponse<BookingQuoteResponse>> quoteBooking(
            Authentication authentication,
            @Valid @RequestBody BookingCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.quoteBooking(authentication.getName(), request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            Authentication authentication,
            @PathVariable String id) {
        BookingResponse response = bookingService.getBookingById(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getMyBookings(
            Authentication authentication,
            Pageable pageable) {
        PageResponse<BookingResponse> response = PageResponse.from(bookingService.getMyBookings(authentication.getName(), pageable));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getUpcomingBookings(
            Authentication authentication,
            Pageable pageable) {
        PageResponse<BookingResponse> response = PageResponse.from(bookingService.getUpcomingBookings(authentication.getName(), pageable));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getBookingHistory(
            Authentication authentication,
            Pageable pageable) {
        PageResponse<BookingResponse> response = PageResponse.from(bookingService.getBookingHistory(authentication.getName(), pageable));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/cancellation-preview")
    public ResponseEntity<ApiResponse<CancellationPreviewResponse>> previewCancellation(
            Authentication authentication,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.previewCancellation(authentication.getName(), id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingCancellationResponse>> cancelBooking(
            Authentication authentication,
            @PathVariable String id) {
        BookingCancellationResponse response = bookingService.cancelBooking(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", response));
    }
}
