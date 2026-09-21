package lk.booknplay.controller.owner;

import io.swagger.v3.oas.annotations.tags.Tag;
import lk.booknplay.dto.response.DailyEarningsResponse;
import lk.booknplay.dto.response.EarningsSummaryResponse;
import lk.booknplay.dto.response.PayoutResponse;
import lk.booknplay.service.OwnerEarningsService;
import lk.booknplay.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
@Tag(name = "Owner Earnings")
public class OwnerEarningsController {

    private final OwnerEarningsService ownerEarningsService;

    @GetMapping("/earnings/summary")
    public ResponseEntity<ApiResponse<EarningsSummaryResponse>> summary(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerEarningsService.getSummary(authentication.getName(), from, to)));
    }

    @GetMapping("/earnings/daily")
    public ResponseEntity<ApiResponse<List<DailyEarningsResponse>>> daily(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerEarningsService.getDaily(authentication.getName(), from, to)));
    }

    @GetMapping("/payouts")
    public ResponseEntity<ApiResponse<List<PayoutResponse>>> payouts(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(ownerEarningsService.listPayouts(authentication.getName())));
    }
}
