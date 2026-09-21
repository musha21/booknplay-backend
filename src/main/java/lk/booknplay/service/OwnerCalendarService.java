package lk.booknplay.service;

import lk.booknplay.dto.request.BlockedSlotRequest;
import lk.booknplay.dto.request.BookingStatusUpdateRequest;
import lk.booknplay.dto.request.MaintenanceWindowRequest;
import lk.booknplay.dto.request.WalkInBookingRequest;
import lk.booknplay.dto.response.BlockedSlotResponse;
import lk.booknplay.dto.response.BookingResponse;
import lk.booknplay.dto.response.MaintenanceWindowResponse;
import lk.booknplay.dto.response.OwnerCalendarResponse;

import java.time.LocalDate;
import java.util.List;

public interface OwnerCalendarService {
    OwnerCalendarResponse getCalendar(String ownerEmail, String venueId, LocalDate date, String courtId);
    BookingResponse createWalkIn(String ownerEmail, WalkInBookingRequest request);
    BookingResponse updateBookingStatus(String ownerEmail, String bookingId, BookingStatusUpdateRequest request);
    MaintenanceWindowResponse addMaintenance(String ownerEmail, String courtId, MaintenanceWindowRequest request);
    List<MaintenanceWindowResponse> listMaintenance(String ownerEmail, String courtId);
    void deleteMaintenance(String ownerEmail, String maintenanceId);
    BlockedSlotResponse addBlockedSlot(String ownerEmail, String courtId, BlockedSlotRequest request);
    List<BlockedSlotResponse> listBlockedSlots(String ownerEmail, String courtId);
    void deleteBlockedSlot(String ownerEmail, String blockedSlotId);
}
