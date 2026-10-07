package lk.booknplay.service.impl;

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
import lk.booknplay.entity.Booking;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.Payment;
import lk.booknplay.entity.Promotion;
import lk.booknplay.entity.Refund;
import lk.booknplay.entity.Review;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.enums.CourtStatus;
import lk.booknplay.enums.BookingSource;
import lk.booknplay.enums.PromotionType;
import lk.booknplay.enums.StaffPermission;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.exception.ConflictException;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.repository.BookingRepository;
import lk.booknplay.repository.BusinessRepository;
import lk.booknplay.repository.CourtRepository;
import lk.booknplay.repository.PaymentRepository;
import lk.booknplay.repository.PromotionRepository;
import lk.booknplay.repository.RefundRepository;
import lk.booknplay.repository.ReviewRepository;
import lk.booknplay.repository.VenueRepository;
import lk.booknplay.service.BookingService;
import lk.booknplay.service.OwnerAccessService;
import lk.booknplay.service.OwnerPortalService;
import lk.booknplay.service.PlanEntitlementService;
import lk.booknplay.util.BookingTimeFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerPortalServiceImpl implements OwnerPortalService {

    private static final List<BookingStatus> SPEND_STATUSES =
            List.copyOf(EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED));

    private final OwnerAccessService ownerAccessService;
    private final PlanEntitlementService planEntitlementService;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final ReviewRepository reviewRepository;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final PromotionRepository promotionRepository;
    private final BusinessRepository businessRepository;
    private final BookingService bookingService;

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> listBookings(String email, LocalDate from, LocalDate to,
                                              String venueId, String courtId, BookingStatus status,
                                              String q, Pageable pageable) {
        ownerAccessService.requireStaffPermission(email, StaffPermission.CALENDAR);
        Business business = ownerAccessService.requireBusiness(email);
        String query = blankToNull(q);
        return bookingRepository.findForOwner(
                        business.getId(), from, to, blankToNull(venueId), blankToNull(courtId), status, query, pageable)
                .map(this::mapBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBooking(String email, String bookingId) {
        ownerAccessService.requireStaffPermission(email, StaffPermission.CALENDAR);
        Business business = ownerAccessService.requireBusiness(email);
        Booking booking = bookingRepository.findByIdAndBusinessId(bookingId, business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return mapBooking(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OwnerPaymentResponse> listPayments(String email, LocalDate from, LocalDate to,
                                                   String venueId, Pageable pageable) {
        ownerAccessService.requireStaffPermission(email, StaffPermission.EARNINGS);
        Business business = ownerAccessService.requireBusiness(email);
        planEntitlementService.assertEarnings(business);
        return paymentRepository.findForOwner(business.getId(), from, to, blankToNull(venueId), pageable)
                .map(this::mapPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OwnerRefundListItemResponse> listRefunds(String email, Pageable pageable) {
        ownerAccessService.requireStaffPermission(email, StaffPermission.EARNINGS);
        Business business = ownerAccessService.requireBusiness(email);
        planEntitlementService.assertEarnings(business);
        return refundRepository.findForOwner(business.getId(), pageable).map(this::mapRefund);
    }

    @Override
    @Transactional
    public BookingCancellationResponse requestRefund(String email, OwnerRefundRequest request) {
        ownerAccessService.requireStaffPermission(email, StaffPermission.EARNINGS);
        Business business = ownerAccessService.requireMutableBusiness(email);
        Booking booking = bookingRepository.findByIdAndBusinessId(request.getBookingId(), business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled");
        }
        String reason = request.getReason() != null && !request.getReason().isBlank()
                ? request.getReason().trim()
                : "Refund requested by venue owner";
        return bookingService.cancelBookingAsOwner(booking.getId(), reason);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnerCustomerResponse> listCustomers(String email, String q) {
        ownerAccessService.requireStaffPermission(email, StaffPermission.CALENDAR);
        Business business = ownerAccessService.requireBusiness(email);
        List<Booking> bookings = bookingRepository.findForEarnings(
                business.getId(), LocalDate.now().minusYears(2), LocalDate.now().plusDays(1), SPEND_STATUSES);
        Map<String, OwnerCustomerResponse> byKey = new LinkedHashMap<>();
        for (Booking booking : bookings) {
            String name = resolveCustomerName(booking);
            String phone = resolvePhone(booking);
            String emailAddr = booking.getContactEmail();
            String customerId = booking.getCustomer() != null ? booking.getCustomer().getId() : null;
            String key = customerId != null ? "c:" + customerId
                    : phone != null ? "p:" + phone.toLowerCase(Locale.ROOT)
                    : "n:" + (name != null ? name.toLowerCase(Locale.ROOT) : booking.getId());
            OwnerCustomerResponse existing = byKey.get(key);
            BigDecimal amount = booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;
            if (existing == null) {
                byKey.put(key, OwnerCustomerResponse.builder()
                        .key(key)
                        .customerId(customerId)
                        .name(name)
                        .phone(phone)
                        .email(emailAddr)
                        .visitCount(1)
                        .totalSpend(amount)
                        .lastBookingDate(booking.getBookingDate())
                        .lastBookingRef(booking.getBookingRef())
                        .build());
            } else {
                existing.setVisitCount(existing.getVisitCount() + 1);
                existing.setTotalSpend(existing.getTotalSpend().add(amount));
                if (existing.getLastBookingDate() == null
                        || (booking.getBookingDate() != null
                        && booking.getBookingDate().isAfter(existing.getLastBookingDate()))) {
                    existing.setLastBookingDate(booking.getBookingDate());
                    existing.setLastBookingRef(booking.getBookingRef());
                }
            }
        }
        String query = blankToNull(q);
        return byKey.values().stream()
                .filter(c -> query == null
                        || contains(c.getName(), query)
                        || contains(c.getPhone(), query)
                        || contains(c.getEmail(), query))
                .sorted(Comparator.comparing(OwnerCustomerResponse::getLastBookingDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnerSportResponse> listSports(String email) {
        ownerAccessService.requireStaffPermission(email, StaffPermission.COURTS);
        Business business = ownerAccessService.requireBusiness(email);
        List<Venue> venues = venueRepository.findByBusinessIdAndStatusNot(business.getId(), VenueStatus.DELETED);
        Map<String, OwnerSportResponse> sports = new LinkedHashMap<>();
        for (Venue venue : venues) {
            for (Court court : courtRepository.findByVenueId(venue.getId())) {
                if (court.getStatus() == CourtStatus.DELETED || court.getSport() == null) continue;
                String sportId = court.getSport().getId();
                OwnerSportResponse row = sports.get(sportId);
                if (row == null) {
                    row = OwnerSportResponse.builder()
                            .sportId(sportId)
                            .sportName(court.getSport().getName())
                            .courtCount(0)
                            .venueNames(new ArrayList<>())
                            .build();
                    sports.put(sportId, row);
                }
                row.setCourtCount(row.getCourtCount() + 1);
                if (!row.getVenueNames().contains(venue.getName())) {
                    row.getVenueNames().add(venue.getName());
                }
            }
        }
        return new ArrayList<>(sports.values());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponse> listPromotions(String email) {
        Business business = ownerAccessService.requireOwner(email);
        planEntitlementService.assertPromotions(business);
        return promotionRepository.findByBusinessIdOrderByCreatedAtDesc(business.getId()).stream()
                .map(this::mapPromotion)
                .toList();
    }

    @Override
    @Transactional
    public PromotionResponse createPromotion(String email, PromotionRequest request) {
        Business business = ownerAccessService.requireMutableOwner(email);
        planEntitlementService.assertPromotions(business);
        validatePromotion(request);
        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (promotionRepository.existsByBusinessIdAndCodeIgnoreCase(business.getId(), code)) {
            throw new ConflictException("PROMO_EXISTS", "A promotion with this code already exists");
        }
        Promotion promotion = Promotion.builder()
                .businessId(business.getId())
                .code(code)
                .name(request.getName().trim())
                .type(request.getType())
                .value(request.getValue())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .venueIds(joinIds(request.getVenueIds()))
                .sportIds(joinIds(request.getSportIds()))
                .maxRedemptions(request.getMaxRedemptions())
                .active(request.getActive() == null || request.getActive())
                .build();
        return mapPromotion(promotionRepository.save(promotion));
    }

    @Override
    @Transactional
    public PromotionResponse updatePromotion(String email, String promotionId, PromotionRequest request) {
        Business business = ownerAccessService.requireMutableOwner(email);
        planEntitlementService.assertPromotions(business);
        Promotion promotion = promotionRepository.findByIdAndBusinessId(promotionId, business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
        validatePromotion(request);
        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        promotionRepository.findByBusinessIdAndCodeIgnoreCase(business.getId(), code)
                .filter(other -> !other.getId().equals(promotionId))
                .ifPresent(other -> {
                    throw new ConflictException("PROMO_EXISTS", "A promotion with this code already exists");
                });
        promotion.setCode(code);
        promotion.setName(request.getName().trim());
        promotion.setType(request.getType());
        promotion.setValue(request.getValue());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setVenueIds(joinIds(request.getVenueIds()));
        promotion.setSportIds(joinIds(request.getSportIds()));
        promotion.setMaxRedemptions(request.getMaxRedemptions());
        if (request.getActive() != null) promotion.setActive(request.getActive());
        return mapPromotion(promotionRepository.save(promotion));
    }

    @Override
    @Transactional
    public void deletePromotion(String email, String promotionId) {
        Business business = ownerAccessService.requireMutableOwner(email);
        planEntitlementService.assertPromotions(business);
        Promotion promotion = promotionRepository.findByIdAndBusinessId(promotionId, business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
        promotionRepository.delete(promotion);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal previewPromotionDiscount(String businessId, String code, String venueId, String sportId,
                                               BigDecimal amount, LocalDate date) {
        Promotion promotion = resolveValidPromotion(businessId, code, venueId, sportId, amount, date);
        if (promotion == null) return BigDecimal.ZERO;
        return computeDiscount(promotion, amount);
    }

    @Override
    @Transactional
    public BigDecimal redeemPromotion(String businessId, String code, String venueId, String sportId,
                                      BigDecimal amount, LocalDate date) {
        Promotion promotion = resolveValidPromotion(businessId, code, venueId, sportId, amount, date);
        if (promotion == null) return BigDecimal.ZERO;
        BigDecimal discount = computeDiscount(promotion, amount);
        promotion.setRedemptionCount(promotion.getRedemptionCount() + 1);
        promotionRepository.save(promotion);
        return discount;
    }

    private Promotion resolveValidPromotion(String businessId, String code, String venueId, String sportId,
                                            BigDecimal amount, LocalDate date) {
        if (code == null || code.isBlank() || amount == null || amount.signum() <= 0) {
            return null;
        }
        Promotion promotion = promotionRepository.findByBusinessIdAndCodeIgnoreCase(businessId, code.trim())
                .orElseThrow(() -> new BadRequestException("Invalid promotion code"));
        if (!promotion.isActive()) throw new BadRequestException("Promotion is inactive");
        if (date.isBefore(promotion.getStartDate()) || date.isAfter(promotion.getEndDate())) {
            throw new BadRequestException("Promotion is not valid for this date");
        }
        if (promotion.getMaxRedemptions() != null && promotion.getRedemptionCount() >= promotion.getMaxRedemptions()) {
            throw new BadRequestException("Promotion has reached its redemption limit");
        }
        if (!matchesScope(promotion.getVenueIds(), venueId) || !matchesScope(promotion.getSportIds(), sportId)) {
            throw new BadRequestException("Promotion does not apply to this booking");
        }
        return promotion;
    }

    private static BigDecimal computeDiscount(Promotion promotion, BigDecimal amount) {
        if (promotion.getType() == PromotionType.PERCENT) {
            return amount.multiply(promotion.getValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        return promotion.getValue().min(amount).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerSettingsResponse getSettings(String email) {
        Business business = ownerAccessService.requireOwner(email);
        return mapSettings(business);
    }

    @Override
    @Transactional
    public OwnerSettingsResponse updateSettings(String email, OwnerSettingsUpdateRequest request) {
        Business business = ownerAccessService.requireMutableOwner(email);
        if (request.getBankName() != null) business.setBankName(blankToNull(request.getBankName()));
        if (request.getBankAccountName() != null) business.setBankAccountName(blankToNull(request.getBankAccountName()));
        if (request.getBankAccountNumber() != null) business.setBankAccountNumber(blankToNull(request.getBankAccountNumber()));
        if (request.getBankBranch() != null) business.setBankBranch(blankToNull(request.getBankBranch()));
        if (request.getNotifyBookingEmail() != null) business.setNotifyBookingEmail(request.getNotifyBookingEmail());
        if (request.getNotifyBookingSms() != null) business.setNotifyBookingSms(request.getNotifyBookingSms());
        if (request.getNotifyPaymentEmail() != null) business.setNotifyPaymentEmail(request.getNotifyPaymentEmail());
        if (request.getNotifyTrialEmail() != null) business.setNotifyTrialEmail(request.getNotifyTrialEmail());
        if (request.getContactPhone() != null) business.setContactPhone(blankToNull(request.getContactPhone()));
        if (request.getContactEmail() != null) business.setContactEmail(blankToNull(request.getContactEmail()));
        return mapSettings(businessRepository.save(business));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OwnerReviewResponse> listReviews(String email, String venueId, Pageable pageable) {
        Business business = ownerAccessService.requireBusiness(email);
        return reviewRepository.findForOwner(business.getId(), blankToNull(venueId), pageable)
                .map(this::mapReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnerActivityResponse> listActivity(String email, int limit) {
        Business business = ownerAccessService.requireBusiness(email);
        int size = Math.min(Math.max(limit, 1), 50);
        return bookingRepository.findRecentByBusinessId(business.getId(), PageRequest.of(0, size)).stream()
                .map(b -> OwnerActivityResponse.builder()
                        .id(b.getId())
                        .type(activityType(b))
                        .summary(activitySummary(b))
                        .bookingRef(b.getBookingRef())
                        .venueName(b.getVenue() != null ? b.getVenue().getName() : null)
                        .occurredAt(b.getUpdatedAt() != null ? b.getUpdatedAt() : b.getCreatedAt())
                        .build())
                .toList();
    }

    private String activityType(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED) return "BOOKING_CANCELLED";
        if (booking.getSource() == BookingSource.WALK_IN) return "WALK_IN";
        if (booking.getStatus() == BookingStatus.CONFIRMED) return "BOOKING_CONFIRMED";
        return "BOOKING_" + booking.getStatus().name();
    }

    private String activitySummary(Booking booking) {
        String who = resolveCustomerName(booking);
        return booking.getStatus().name() + " · " + (who != null ? who : "Guest")
                + " · " + booking.getBookingRef();
    }

    private void validatePromotion(PromotionRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Promotion end date must be on or after start date");
        }
        if (request.getType() == PromotionType.PERCENT
                && request.getValue().compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("Percent promotions cannot exceed 100");
        }
    }

    private BookingResponse mapBooking(Booking booking) {
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        String customerName = resolveCustomerName(booking);
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingRef(booking.getBookingRef())
                .customerId(booking.getCustomer() != null ? booking.getCustomer().getId() : null)
                .customerName(customerName)
                .venueId(booking.getVenue().getId())
                .venueName(booking.getVenue().getName())
                .courtId(booking.getCourt().getId())
                .courtName(booking.getCourt().getName())
                .sportId(booking.getSport().getId())
                .sportName(booking.getSport().getName())
                .date(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .slots(BookingTimeFormat.toSlotResponses(booking.getSlots()))
                .totalAmount(booking.getTotalAmount())
                .currency("LKR")
                .status(booking.getStatus())
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .source(booking.getSource())
                .guestName(booking.getGuestName())
                .guestPhone(booking.getGuestPhone())
                .contactName(booking.getContactName())
                .contactPhone(booking.getContactPhone())
                .contactEmail(booking.getContactEmail())
                .specialRequests(booking.getSpecialRequests())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private OwnerPaymentResponse mapPayment(Payment payment) {
        Booking booking = payment.getBooking();
        return OwnerPaymentResponse.builder()
                .id(payment.getId())
                .bookingId(booking.getId())
                .bookingRef(booking.getBookingRef())
                .venueId(booking.getVenue().getId())
                .venueName(booking.getVenue().getName())
                .customerName(resolveCustomerName(booking))
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentGateway(payment.getPaymentGateway())
                .gatewayReference(payment.getGatewayReference())
                .bookingDate(booking.getBookingDate())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private OwnerRefundListItemResponse mapRefund(Refund refund) {
        Booking booking = refund.getBooking();
        return OwnerRefundListItemResponse.builder()
                .id(refund.getId())
                .bookingId(booking.getId())
                .bookingRef(booking.getBookingRef())
                .venueName(booking.getVenue().getName())
                .customerName(resolveCustomerName(booking))
                .amount(refund.getAmount())
                .mode(refund.getMode())
                .status(refund.getStatus())
                .reference(refund.getReference())
                .processedAt(refund.getProcessedAt())
                .build();
    }

    private PromotionResponse mapPromotion(Promotion promotion) {
        return PromotionResponse.builder()
                .id(promotion.getId())
                .code(promotion.getCode())
                .name(promotion.getName())
                .type(promotion.getType())
                .value(promotion.getValue())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .venueIds(splitIds(promotion.getVenueIds()))
                .sportIds(splitIds(promotion.getSportIds()))
                .maxRedemptions(promotion.getMaxRedemptions())
                .redemptionCount(promotion.getRedemptionCount())
                .active(promotion.isActive())
                .build();
    }

    private OwnerSettingsResponse mapSettings(Business business) {
        return OwnerSettingsResponse.builder()
                .businessId(business.getId())
                .businessName(business.getName())
                .contactEmail(business.getContactEmail())
                .contactPhone(business.getContactPhone())
                .bankName(business.getBankName())
                .bankAccountName(business.getBankAccountName())
                .bankAccountNumber(business.getBankAccountNumber())
                .bankBranch(business.getBankBranch())
                .notifyBookingEmail(business.isNotifyBookingEmail())
                .notifyBookingSms(business.isNotifyBookingSms())
                .notifyPaymentEmail(business.isNotifyPaymentEmail())
                .notifyTrialEmail(business.isNotifyTrialEmail())
                .build();
    }

    private OwnerReviewResponse mapReview(Review review) {
        String customerName = review.getCustomer() != null
                ? review.getCustomer().getFirstName() + " " + review.getCustomer().getLastName()
                : "Customer";
        return OwnerReviewResponse.builder()
                .id(review.getId())
                .venueId(review.getVenue().getId())
                .venueName(review.getVenue().getName())
                .customerName(customerName)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private static String resolveCustomerName(Booking booking) {
        if (booking.getCustomer() != null) {
            return booking.getCustomer().getFirstName() + " " + booking.getCustomer().getLastName();
        }
        if (booking.getGuestName() != null && !booking.getGuestName().isBlank()) return booking.getGuestName();
        return booking.getContactName();
    }

    private static String resolvePhone(Booking booking) {
        if (booking.getCustomer() != null && booking.getCustomer().getPhone() != null) {
            return booking.getCustomer().getPhone();
        }
        if (booking.getGuestPhone() != null && !booking.getGuestPhone().isBlank()) return booking.getGuestPhone();
        return booking.getContactPhone();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT));
    }

    private static String joinIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
    }

    private static List<String> splitIds(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static boolean matchesScope(String csv, String id) {
        if (csv == null || csv.isBlank()) return true;
        if (id == null) return false;
        return splitIds(csv).contains(id);
    }
}
