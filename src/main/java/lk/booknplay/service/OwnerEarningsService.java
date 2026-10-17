package lk.booknplay.service;

import lk.booknplay.dto.response.DailyEarningsResponse;
import lk.booknplay.dto.response.EarningsSummaryResponse;
import lk.booknplay.dto.response.OwnerPayoutSummaryResponse;
import lk.booknplay.dto.response.PayoutResponse;

import java.time.LocalDate;
import java.util.List;

public interface OwnerEarningsService {
    EarningsSummaryResponse getSummary(
            String ownerEmail, LocalDate from, LocalDate to, String view, String sportId, String venueId);

    List<DailyEarningsResponse> getDaily(
            String ownerEmail, LocalDate from, LocalDate to, String view, String sportId, String venueId);

    List<PayoutResponse> listPayouts(String ownerEmail);

    PayoutResponse getPayout(String ownerEmail, String payoutId);

    OwnerPayoutSummaryResponse payoutSummary(String ownerEmail);
}
