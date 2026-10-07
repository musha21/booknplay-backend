package lk.booknplay.service;

import lk.booknplay.dto.request.OwnerRefundRequest;
import lk.booknplay.dto.request.OwnerSettingsUpdateRequest;
import lk.booknplay.dto.request.PromotionRequest;
import lk.booknplay.dto.response.BookingCancellationResponse;
import lk.booknplay.dto.response.BookingResponse;
import lk.booknplay.dto.response.OwnerActivityResponse;
import lk.booknplay.dto.response.OwnerCustomerResponse;
import lk.booknplay.dto.response.OwnerPaymentResponse;
import lk.booknplay.dto.response.OwnerRefundListItemResponse;
import lk.booknplay.dto.response.OwnerReviewResponse;
import lk.booknplay.dto.response.OwnerSettingsResponse;
import lk.booknplay.dto.response.OwnerSportResponse;
import lk.booknplay.dto.response.PromotionResponse;
import lk.booknplay.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface OwnerPortalService {
    Page<BookingResponse> listBookings(String email, LocalDate from, LocalDate to,
                                       String venueId, String courtId, BookingStatus status, String q, Pageable pageable);

    BookingResponse getBooking(String email, String bookingId);

    Page<OwnerPaymentResponse> listPayments(String email, LocalDate from, LocalDate to, String venueId, Pageable pageable);

    Page<OwnerRefundListItemResponse> listRefunds(String email, Pageable pageable);

    BookingCancellationResponse requestRefund(String email, OwnerRefundRequest request);

    List<OwnerCustomerResponse> listCustomers(String email, String q);

    List<OwnerSportResponse> listSports(String email);

    List<PromotionResponse> listPromotions(String email);

    PromotionResponse createPromotion(String email, PromotionRequest request);

    PromotionResponse updatePromotion(String email, String promotionId, PromotionRequest request);

    void deletePromotion(String email, String promotionId);

    /** Preview discount without consuming a redemption. */
    BigDecimal previewPromotionDiscount(String businessId, String code, String venueId, String sportId, BigDecimal amount, LocalDate date);

    /** Apply and consume one redemption. */
    BigDecimal redeemPromotion(String businessId, String code, String venueId, String sportId, BigDecimal amount, LocalDate date);

    OwnerSettingsResponse getSettings(String email);

    OwnerSettingsResponse updateSettings(String email, OwnerSettingsUpdateRequest request);

    Page<OwnerReviewResponse> listReviews(String email, String venueId, Pageable pageable);

    List<OwnerActivityResponse> listActivity(String email, int limit);
}
