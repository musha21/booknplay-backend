package lk.booknplay.controller.owner;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lk.booknplay.dto.request.BlockedSlotRequest;
import lk.booknplay.dto.request.BookingStatusUpdateRequest;
import lk.booknplay.dto.request.MaintenanceWindowRequest;
import lk.booknplay.dto.request.WalkInBookingRequest;
import lk.booknplay.dto.response.BlockedSlotResponse;
import lk.booknplay.dto.response.BookingResponse;
import lk.booknplay.dto.response.MaintenanceWindowResponse;
import lk.booknplay.dto.response.OwnerCalendarResponse;
import lk.booknplay.service.OwnerCalendarService;
import lk.booknplay.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
@Tag(name = "Owner Calendar")
public class OwnerCalendarController {

    private final OwnerCalendarService ownerCalendarService;

    @GetMapping("/venues/{venueId}/calendar")
    public ResponseEntity<ApiResponse<OwnerCalendarResponse>> getCalendar(
            Authentication authentication,
            @PathVariable String venueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String courtId) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerCalendarService.getCalendar(authentication.getName(), venueId, date, courtId)));
    }

    @PostMapping("/bookings/walk-in")
    public ResponseEntity<ApiResponse<BookingResponse>> walkIn(
            Authentication authentication,
            @Valid @RequestBody WalkInBookingRequest request) {
        BookingResponse response = ownerCalendarService.createWalkIn(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Walk-in booking created", response));
    }

    @PatchMapping("/bookings/{bookingId}/status")
    public ResponseEntity<ApiResponse<BookingResponse>> updateStatus(
            Authentication authentication,
            @PathVariable String bookingId,
            @Valid @RequestBody BookingStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerCalendarService.updateBookingStatus(authentication.getName(), bookingId, request)));
    }

    @GetMapping("/courts/{courtId}/maintenance")
    public ResponseEntity<ApiResponse<List<MaintenanceWindowResponse>>> listMaintenance(
            Authentication authentication,
            @PathVariable String courtId) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerCalendarService.listMaintenance(authentication.getName(), courtId)));
    }

    @PostMapping("/courts/{courtId}/maintenance")
    public ResponseEntity<ApiResponse<MaintenanceWindowResponse>> addMaintenance(
            Authentication authentication,
            @PathVariable String courtId,
            @Valid @RequestBody MaintenanceWindowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Maintenance window created",
                ownerCalendarService.addMaintenance(authentication.getName(), courtId, request)));
    }

    @DeleteMapping("/maintenance/{maintenanceId}")
    public ResponseEntity<ApiResponse<Void>> deleteMaintenance(
            Authentication authentication,
            @PathVariable String maintenanceId) {
        ownerCalendarService.deleteMaintenance(authentication.getName(), maintenanceId);
        return ResponseEntity.ok(ApiResponse.success("Maintenance window deleted"));
    }

    @GetMapping("/courts/{courtId}/blocked-slots")
    public ResponseEntity<ApiResponse<List<BlockedSlotResponse>>> listBlocked(
            Authentication authentication,
            @PathVariable String courtId) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerCalendarService.listBlockedSlots(authentication.getName(), courtId)));
    }

    @PostMapping("/courts/{courtId}/blocked-slots")
    public ResponseEntity<ApiResponse<BlockedSlotResponse>> addBlocked(
            Authentication authentication,
            @PathVariable String courtId,
            @Valid @RequestBody BlockedSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Slot blocked",
                ownerCalendarService.addBlockedSlot(authentication.getName(), courtId, request)));
    }

    @DeleteMapping("/blocked-slots/{blockedSlotId}")
    public ResponseEntity<ApiResponse<Void>> deleteBlocked(
            Authentication authentication,
            @PathVariable String blockedSlotId) {
        ownerCalendarService.deleteBlockedSlot(authentication.getName(), blockedSlotId);
        return ResponseEntity.ok(ApiResponse.success("Blocked slot removed"));
    }
}
