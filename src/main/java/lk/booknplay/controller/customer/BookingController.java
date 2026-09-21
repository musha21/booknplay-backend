package lk.booknplay.controller.customer;

import jakarta.validation.Valid;
import lk.booknplay.dto.request.BookingCreateRequest;
import lk.booknplay.dto.response.BookingResponse;
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
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            Authentication authentication,
            @Valid @RequestBody BookingCreateRequest request) {
        BookingResponse response = bookingService.createBooking(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", response));
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

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            Authentication authentication,
            @PathVariable String id) {
        BookingResponse response = bookingService.cancelBooking(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", response));
    }
}
