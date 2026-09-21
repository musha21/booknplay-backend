package lk.booknplay.controller.public_api;

import lk.booknplay.dto.response.AvailabilityResponse;
import lk.booknplay.service.AvailabilityService;
import lk.booknplay.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/public/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping
    public ResponseEntity<ApiResponse<AvailabilityResponse>> getAvailability(
            @RequestParam String courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AvailabilityResponse response = availabilityService.getAvailability(courtId, date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
