package lk.booknplay.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.booknplay.dto.request.BookingConfirmRequest;
import lk.booknplay.dto.request.BookingCreateRequest;
import lk.booknplay.dto.request.BookingReviewRequest;
import lk.booknplay.dto.request.SlotSelectionRequest;
import lk.booknplay.dto.response.*;
import lk.booknplay.entity.*;
import lk.booknplay.enums.BookingSource;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.enums.PaymentStatus;
import lk.booknplay.exception.*;
import lk.booknplay.repository.*;
import lk.booknplay.service.BookingRulesService;
import lk.booknplay.service.BookingService;
import lk.booknplay.service.NotificationService;
import lk.booknplay.service.OwnerPortalService;
import lk.booknplay.service.PayHereRefundClient;
import lk.booknplay.util.BookingTimeFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final CourtRepository courtRepository;
    private final SportRepository sportRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final BookingQuoteRepository quoteRepository;
    private final RefundRepository refundRepository;
    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final ReviewRepository reviewRepository;
    private final BookingRulesService bookingRulesService;
    private final NotificationService notificationService;
    private final PayHereRefundClient payHereRefundClient;
    private final ObjectMapper objectMapper;
    private final OwnerPortalService ownerPortalService;

    @Value("${booknplay.payments.mode:PAYHERE}")
    private String paymentMode;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            CustomerRepository customerRepository,
            CourtRepository courtRepository,
            SportRepository sportRepository,
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            BookingQuoteRepository quoteRepository,
            RefundRepository refundRepository,
            CancellationPolicyRepository cancellationPolicyRepository,
            ReviewRepository reviewRepository,
            BookingRulesService bookingRulesService,
            NotificationService notificationService,
            PayHereRefundClient payHereRefundClient,
            ObjectMapper objectMapper,
            @Lazy OwnerPortalService ownerPortalService) {
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
        this.courtRepository = courtRepository;
        this.sportRepository = sportRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.quoteRepository = quoteRepository;
        this.refundRepository = refundRepository;
        this.cancellationPolicyRepository = cancellationPolicyRepository;
        this.reviewRepository = reviewRepository;
        this.bookingRulesService = bookingRulesService;
        this.notificationService = notificationService;
        this.payHereRefundClient = payHereRefundClient;
        this.objectMapper = objectMapper;
        this.ownerPortalService = ownerPortalService;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(String customerEmail, String bookingId) {
        return mapToResponse(requireCustomerBooking(customerEmail, bookingId), getPaymentStatus(bookingId));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByRef(String customerEmail, String bookingRef) {
        Customer customer = requireCustomer(customerEmail);
        Booking booking = bookingRepository.findByBookingRef(bookingRef)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ref: " + bookingRef));
        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("Access denied to this booking");
        }
        return mapToResponse(booking, getPaymentStatus(booking.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getMyBookings(String customerEmail, Pageable pageable) {
        Customer customer = requireCustomer(customerEmail);
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId(), pageable)
                .map(b -> mapToResponse(b, getPaymentStatus(b.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getUpcomingBookings(String customerEmail, Pageable pageable) {
        Customer customer = requireCustomer(customerEmail);
        return bookingRepository.findByCustomerIdAndBookingDateGreaterThanEqualAndStatusOrderByBookingDateAscStartTimeAsc(
                customer.getId(), LocalDate.now(), BookingStatus.CONFIRMED, pageable)
                .map(b -> mapToResponse(b, getPaymentStatus(b.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookingHistory(String customerEmail, Pageable pageable) {
        Customer customer = requireCustomer(customerEmail);
        // Include CONFIRMED past-date bookings so customers can leave a review after play time.
        List<BookingStatus> statuses = List.of(
                BookingStatus.COMPLETED, BookingStatus.CONFIRMED, BookingStatus.CANCELLED, BookingStatus.NO_SHOW);
        return bookingRepository.findByCustomerIdAndBookingDateLessThanAndStatusInOrderByBookingDateDescStartTimeDesc(
                customer.getId(), LocalDate.now(), statuses, pageable)
                .map(b -> mapToResponse(b, getPaymentStatus(b.getId())));
    }

    @Override
    @Transactional
    public BookingQuoteResponse quoteBooking(String customerEmail, BookingCreateRequest request) {
        requirePayHereMode();
        Customer customer = requireCustomer(customerEmail);
        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Court not found with id: " + request.getCourtId()));
        Sport sport = sportRepository.findById(request.getSportId())
                .orElseThrow(() -> new ResourceNotFoundException("Sport not found with id: " + request.getSportId()));

        List<SlotSelectionRequest> selections = resolveSlotSelections(request);
        BookingRulesService.Result result = bookingRulesService.validateAndPrice(
                court, sport, request.getDate(), selections);

        LocalTime envelopeStart = result.slots().get(0).startTime();
        LocalTime envelopeEnd = result.slots().get(result.slots().size() - 1).endTime();

        BigDecimal discount = BigDecimal.ZERO;
        String promoCode = request.getPromoCode() != null ? request.getPromoCode().trim() : null;
        if (promoCode != null && !promoCode.isBlank()) {
            discount = ownerPortalService.previewPromotionDiscount(
                    court.getVenue().getBusiness().getId(),
                    promoCode,
                    court.getVenue().getId(),
                    sport.getId(),
                    result.totalAmount(),
                    request.getDate());
        }
        BigDecimal payable = result.totalAmount().subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        CancellationPolicy policy = cancellationPolicyRepository
                .findByBusinessId(court.getVenue().getBusiness().getId()).orElse(null);
        int hours = policy == null || policy.getHoursBeforeDeadline() == null ? 0 : policy.getHoursBeforeDeadline();
        BigDecimal lateRefund = policy == null || policy.getRefundPercentage() == null
                ? BigDecimal.ZERO : policy.getRefundPercentage();
        boolean cancellationAllowed = hours > 0 || lateRefund.signum() > 0;
        LocalDateTime now = LocalDateTime.now();
        BookingQuote quote = quoteRepository.save(BookingQuote.builder()
                .customer(customer).court(court).sport(sport)
                .bookingDate(request.getDate()).startTime(envelopeStart).endTime(envelopeEnd)
                .selectedSlots(writeSlotsJson(result.slots()))
                .totalAmount(payable)
                .promoCode(discount.signum() > 0 ? promoCode : null)
                .discountAmount(discount)
                .cancellationAllowed(cancellationAllowed)
                .cancellationDeadline(hours > 0
                        ? LocalDateTime.of(request.getDate(), envelopeStart).minusHours(hours) : null)
                .lateRefundPercentage(lateRefund)
                .createdAt(now).expiresAt(now.plusMinutes(10)).build());

        return BookingQuoteResponse.builder()
                .quoteId(quote.getId()).expiresAt(quote.getExpiresAt()).paymentMode("PAYHERE")
                .slots(result.slots().stream().map(slot -> BookingQuoteResponse.QuotedSlotResponse.builder()
                        .startTime(slot.startTime().toString()).endTime(slot.endTime().toString())
                        .price(slot.price()).build()).toList())
                .totalAmount(payable)
                .discountAmount(discount)
                .promoCode(discount.signum() > 0 ? promoCode : null)
                .payNow(payable).balanceDue(BigDecimal.ZERO)
                .balanceCollection("NONE").cancellationAllowed(cancellationAllowed)
                .cancellationDeadline(quote.getCancellationDeadline())
                .afterDeadlineSummary(cancellationAllowed
                        ? lateRefund.stripTrailingZeros().toPlainString() + "% refund after the deadline (processed by the venue)."
                        : null)
                .noShowSummary("No refund is issued for a no-show.")
                .build();
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingCheckoutResponse createBooking(String customerEmail, BookingConfirmRequest request, String idempotencyKey) {
        requirePayHereMode();
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new BadRequestException("A valid Idempotency-Key header is required");
        }
        Customer customer = requireCustomer(customerEmail);
        Booking existing = bookingRepository.findByCustomerIdAndIdempotencyKey(customer.getId(), idempotencyKey).orElse(null);
        if (existing != null) return checkoutResponse(existing);

        BookingQuote quote = quoteRepository.findById(request.getQuoteId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking quote not found"));
        if (!quote.getCustomer().getId().equals(customer.getId())) throw new ForbiddenException("Access denied to this quote");
        if (quote.getExpiresAt().isBefore(LocalDateTime.now())) throw new GoneException("The booking quote has expired");
        if (quote.getConsumedAt() != null) throw new ConflictException("QUOTE_CONSUMED", "This quote has already been used.");

        Court court = courtRepository.findByIdWithLock(quote.getCourt().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Court not found"));
        List<SlotSelectionRequest> selections = readSlotSelections(quote);
        BookingRulesService.Result current = bookingRulesService.validateAndPrice(
                court, quote.getSport(), quote.getBookingDate(), selections);
        BigDecimal expected = current.totalAmount();
        if (quote.getPromoCode() != null && !quote.getPromoCode().isBlank()) {
            BigDecimal discount = ownerPortalService.redeemPromotion(
                    court.getVenue().getBusiness().getId(),
                    quote.getPromoCode(),
                    court.getVenue().getId(),
                    quote.getSport().getId(),
                    current.totalAmount(),
                    quote.getBookingDate());
            expected = current.totalAmount().subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }
        if (expected.compareTo(quote.getTotalAmount()) != 0) {
            throw new ConflictException("QUOTE_CHANGED", "The price changed. Request a new quote before confirming.");
        }

        LocalTime envelopeStart = current.slots().get(0).startTime();
        LocalTime envelopeEnd = current.slots().get(current.slots().size() - 1).endTime();

        String ref = "BNP-" + quote.getBookingDate().toString().replace("-", "") + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Booking booking = Booking.builder()
                .bookingRef(ref).customer(customer).court(court).venue(court.getVenue()).sport(quote.getSport())
                .bookingDate(quote.getBookingDate()).startTime(envelopeStart).endTime(envelopeEnd)
                .totalAmount(quote.getTotalAmount()).status(BookingStatus.PENDING).source(BookingSource.ONLINE)
                .idempotencyKey(idempotencyKey)
                .contactName(request.getContact().getFullName().trim())
                .contactEmail(request.getContact().getEmail().trim())
                .contactPhone(request.getContact().getPhoneNumber().trim())
                .specialRequests(trimToNull(request.getContact().getSpecialRequests()))
                .cancellationAllowed(quote.isCancellationAllowed())
                .cancellationDeadline(quote.getCancellationDeadline())
                .lateRefundPercentage(quote.getLateRefundPercentage())
                .policySnapshotSource("CURRENT_BUSINESS_POLICY")
                .build();

        for (BookingRulesService.SlotPrice slot : current.slots()) {
            booking.getSlots().add(BookingSlot.builder()
                    .booking(booking)
                    .startTime(slot.startTime())
                    .endTime(slot.endTime())
                    .price(slot.price())
                    .build());
        }
        booking = bookingRepository.save(booking);

        Payment payment = paymentRepository.save(Payment.builder()
                .booking(booking).customer(customer).amount(booking.getTotalAmount()).currency("LKR")
                .paymentGateway("PAYHERE")
                .status(PaymentStatus.INITIATED).build());
        quote.setConsumedAt(LocalDateTime.now());
        quote.setBookingId(booking.getId());
        quoteRepository.save(quote);
        return BookingCheckoutResponse.builder().booking(mapToResponse(booking, payment.getStatus()))
                .payment(mapPayment(payment)).invoiceAvailable(false).paymentMode("PAYHERE").build();
    }

    @Override
    @Transactional(readOnly = true)
    public CancellationPreviewResponse previewCancellation(String customerEmail, String bookingId) {
        return buildCancellationPreview(requireCustomerBooking(customerEmail, bookingId));
    }

    @Override
    @Transactional
    public BookingCancellationResponse cancelBooking(String customerEmail, String bookingId) {
        Booking booking = requireCustomerBooking(customerEmail, bookingId);
        return cancelConfirmedBooking(booking, "CUSTOMER", "Customer requested cancellation");
    }

    @Override
    @Transactional
    public BookingCancellationResponse cancelBookingAsOwner(String bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        return cancelConfirmedBooking(booking, "OWNER", reason == null ? "Venue cancelled booking" : reason);
    }

    private List<SlotSelectionRequest> resolveSlotSelections(BookingCreateRequest request) {
        if (request.getSlots() != null && !request.getSlots().isEmpty()) {
            return request.getSlots().stream()
                    .sorted(Comparator.comparing(SlotSelectionRequest::getStartTime))
                    .toList();
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BadRequestException("Provide discrete slots or a continuous start and end time");
        }
        List<SlotSelectionRequest> expanded = new ArrayList<>();
        for (LocalTime cursor = request.getStartTime(); cursor.isBefore(request.getEndTime());
             cursor = cursor.plusMinutes(BookingRulesService.SLOT_MINUTES)) {
            expanded.add(SlotSelectionRequest.builder()
                    .startTime(cursor)
                    .endTime(cursor.plusMinutes(BookingRulesService.SLOT_MINUTES))
                    .build());
        }
        if (expanded.isEmpty()) {
            throw new BadRequestException("Select at least one time slot");
        }
        return expanded;
    }

    private List<SlotSelectionRequest> readSlotSelections(BookingQuote quote) {
        if (quote.getSelectedSlots() != null && !quote.getSelectedSlots().isBlank()) {
            try {
                List<Map<String, String>> raw = objectMapper.readValue(
                        quote.getSelectedSlots(), new TypeReference<>() {});
                List<SlotSelectionRequest> slots = new ArrayList<>();
                for (Map<String, String> row : raw) {
                    slots.add(SlotSelectionRequest.builder()
                            .startTime(LocalTime.parse(row.get("startTime")))
                            .endTime(LocalTime.parse(row.get("endTime")))
                            .build());
                }
                if (!slots.isEmpty()) {
                    return slots.stream()
                            .sorted(Comparator.comparing(SlotSelectionRequest::getStartTime))
                            .toList();
                }
            } catch (Exception ex) {
                throw new BadRequestException("Stored quote slots are invalid. Request a new quote.");
            }
        }
        List<SlotSelectionRequest> expanded = new ArrayList<>();
        for (LocalTime cursor = quote.getStartTime(); cursor.isBefore(quote.getEndTime());
             cursor = cursor.plusMinutes(BookingRulesService.SLOT_MINUTES)) {
            expanded.add(SlotSelectionRequest.builder()
                    .startTime(cursor)
                    .endTime(cursor.plusMinutes(BookingRulesService.SLOT_MINUTES))
                    .build());
        }
        if (expanded.isEmpty()) {
            throw new BadRequestException("Stored quote has no bookable slots. Request a new quote.");
        }
        return expanded;
    }

    private String writeSlotsJson(List<BookingRulesService.SlotPrice> slots) {
        try {
            List<Map<String, String>> payload = slots.stream()
                    .map(slot -> Map.of(
                            "startTime", slot.startTime().toString(),
                            "endTime", slot.endTime().toString()))
                    .toList();
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new BadRequestException("Could not store selected slots");
        }
    }

    private BookingCancellationResponse cancelConfirmedBooking(Booking booking, String actor, String reason) {
        Booking locked = bookingRepository.findByIdForUpdate(booking.getId()).orElse(booking);
        if (locked.getStatus() == BookingStatus.CANCELLED) return cancellationResponse(locked);
        CancellationPreviewResponse preview = buildCancellationPreview(locked);
        if (!preview.isEligible()) throw new BadRequestException(preview.getMessage());

        Payment payment = paymentRepository.findByBookingId(locked.getId()).orElse(null);
        boolean walkInCash = isWalkIn(locked)
                || (payment != null && "WALK_IN_CASH".equals(payment.getPaymentGateway()));
        boolean needsGatewayRefund = !walkInCash
                && payment != null
                && "PAYHERE".equals(payment.getPaymentGateway())
                && preview.getRefundAmount() != null
                && preview.getRefundAmount().signum() > 0
                && (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.PAID);

        if (needsGatewayRefund) {
            if (payment.getGatewayReference() == null || payment.getGatewayReference().isBlank()) {
                throw new BadRequestException(
                        "This payment has no PayHere payment id yet. Wait for payment confirmation, then try cancelling again."
                );
            }

            Refund existing = refundRepository.findByBookingId(locked.getId()).orElse(null);
            if (existing != null) {
                String refundStatus = existing.getStatus() == null ? "" : existing.getStatus().trim().toUpperCase();
                if ("SUCCEEDED".equals(refundStatus) || "PROCESSING".equals(refundStatus)) {
                    throw new BadRequestException(
                            "A refund is already in progress or completed for this booking."
                    );
                }
            }

            // Persist refund intent before calling PayHere so retries cannot double-refund.
            String pendingRef = "CANCEL-" + locked.getBookingRef() + "-" + System.currentTimeMillis();
            Refund refund = existing != null ? existing : Refund.builder()
                    .booking(locked)
                    .payment(payment)
                    .amount(preview.getRefundAmount())
                    .mode("PAYHERE")
                    .status("PROCESSING")
                    .reference(pendingRef)
                    .processedAt(LocalDateTime.now())
                    .build();
            refund.setAmount(preview.getRefundAmount());
            refund.setMode("PAYHERE");
            refund.setStatus("PROCESSING");
            refund.setProcessedAt(LocalDateTime.now());
            if (refund.getReference() == null || refund.getReference().isBlank()) {
                refund.setReference(pendingRef);
            }
            refundRepository.saveAndFlush(refund);

            String payHereRefundRef;
            try {
                payHereRefundRef = payHereRefundClient.refund(
                        payment.getGatewayReference(),
                        preview.getRefundAmount(),
                        preview.getAmountPaid(),
                        "Booking " + locked.getBookingRef() + " cancelled"
                );
            } catch (RuntimeException ex) {
                refund.setStatus("FAILED");
                refund.setProcessedAt(LocalDateTime.now());
                refundRepository.save(refund);
                throw ex;
            }

            refund.setStatus("SUCCEEDED");
            if (payHereRefundRef != null && !payHereRefundRef.isBlank()) {
                refund.setReference(payHereRefundRef);
            }
            refund.setProcessedAt(LocalDateTime.now());
            refundRepository.save(refund);
            payment.setStatus(preview.getRefundAmount().compareTo(preview.getAmountPaid()) >= 0
                    ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
            paymentRepository.save(payment);
        }

        locked.setStatus(BookingStatus.CANCELLED);
        locked.setCancelledAt(LocalDateTime.now());
        locked.setCancelledBy(actor);
        locked.setCancellationReason(reason);
        locked.setCancellationFee(preview.getCancellationFee());
        locked.setRefundAmount(preview.getRefundAmount());
        bookingRepository.save(locked);
        notifyAfterCommit(locked.getCustomer(), locked, "BOOKING_CANCELLED");
        return cancellationResponse(locked);
    }

    private CancellationPreviewResponse buildCancellationPreview(Booking booking) {
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        BigDecimal amountPaid = payment != null && (payment.getStatus() == PaymentStatus.SUCCESS
                || payment.getStatus() == PaymentStatus.PAID) ? payment.getAmount() : BigDecimal.ZERO;
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return closedPreview(amountPaid, booking, "This booking is already cancelled.");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            return closedPreview(amountPaid, booking, "Cancellation is closed for this booking.");
        }
        if (isWalkIn(booking) || (payment != null && "WALK_IN_CASH".equals(payment.getPaymentGateway()))) {
            return CancellationPreviewResponse.builder()
                    .eligible(true)
                    .amountPaid(amountPaid)
                    .cancellationFee(BigDecimal.ZERO)
                    .refundAmount(BigDecimal.ZERO)
                    .deadline(null)
                    .refundMode("NONE")
                    .policySnapshotSource(booking.getPolicySnapshotSource())
                    .message("Walk-in booking cancelled. Cash settlement is handled by the venue.")
                    .build();
        }
        if (!Boolean.TRUE.equals(booking.getCancellationAllowed())) {
            return closedPreview(amountPaid, booking, "Online cancellation is not available for this booking.");
        }
        LocalDateTime start = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
        if (!LocalDateTime.now().isBefore(start)) {
            return closedPreview(amountPaid, booking, "Cancellation is closed. The booking has already started.");
        }
        boolean free = booking.getCancellationDeadline() != null
                && LocalDateTime.now().isBefore(booking.getCancellationDeadline());
        BigDecimal percentage = booking.getLateRefundPercentage() == null ? BigDecimal.ZERO : booking.getLateRefundPercentage();
        BigDecimal refund = free ? amountPaid
                : amountPaid.multiply(percentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        boolean payHereRefund = refund.signum() > 0 && amountPaid.signum() > 0;
        return CancellationPreviewResponse.builder().eligible(true).amountPaid(amountPaid)
                .cancellationFee(amountPaid.subtract(refund).max(BigDecimal.ZERO)).refundAmount(refund)
                .refundMethod(payHereRefund ? "Refund via PayHere to the original payment method" : null)
                .deadline(booking.getCancellationDeadline()).refundMode(payHereRefund ? "PAYHERE" : "NONE")
                .policySnapshotSource(booking.getPolicySnapshotSource())
                .message(free ? "Cancelling now refunds the amount paid through PayHere to your original payment method."
                        : refund.signum() > 0 ? "Cancelling now issues a partial refund through PayHere to your original payment method."
                        : "Cancelling now does not issue a refund.").build();
    }

    private static boolean isWalkIn(Booking booking) {
        return booking.getSource() == BookingSource.WALK_IN;
    }

    private CancellationPreviewResponse closedPreview(BigDecimal paid, Booking booking, String message) {
        return CancellationPreviewResponse.builder().eligible(false).amountPaid(paid)
                .cancellationFee(BigDecimal.ZERO).refundAmount(BigDecimal.ZERO)
                .deadline(booking.getCancellationDeadline()).refundMode("NONE")
                .policySnapshotSource(booking.getPolicySnapshotSource()).message(message).build();
    }

    private BookingCancellationResponse cancellationResponse(Booking booking) {
        Refund refund = refundRepository.findByBookingId(booking.getId()).orElse(null);
        PaymentStatus paymentStatus = getPaymentStatus(booking.getId());
        RefundResponse refundResponse = refund == null
                ? RefundResponse.builder().status("NOT_REQUIRED").amount(BigDecimal.ZERO).mode("NONE")
                    .message("No refund was required.").build()
                : RefundResponse.builder().status(refund.getStatus()).amount(refund.getAmount()).mode(refund.getMode())
                    .reference(refund.getReference()).processedAt(refund.getProcessedAt())
                    .message("PAYHERE".equals(refund.getMode())
                            ? "Refund submitted to PayHere. Funds return to the original payment method."
                            : "Refund recorded.")
                    .build();
        return BookingCancellationResponse.builder().booking(mapToResponse(booking, paymentStatus))
                .cancelledAt(booking.getCancelledAt()).cancelledBy(booking.getCancelledBy())
                .cancellationFee(zero(booking.getCancellationFee())).refundAmount(zero(booking.getRefundAmount()))
                .refund(refundResponse).build();
    }

    private BookingCheckoutResponse checkoutResponse(Booking booking) {
        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new IllegalStateException("Booking has no payment"));
        return BookingCheckoutResponse.builder().booking(mapToResponse(booking, payment.getStatus()))
                .payment(mapPayment(payment)).invoiceAvailable(invoiceRepository.findByBookingId(booking.getId()).isPresent())
                .paymentMode("PAYHERE").build();
    }

    private Customer requireCustomer(String email) {
        return customerRepository.findByUser_Email(email)
                .orElseThrow(() -> new UnauthorizedException("Customer not found"));
    }

    private Booking requireCustomerBooking(String email, String bookingId) {
        Customer customer = requireCustomer(email);
        Booking booking = bookingRepository.findByIdWithSlots(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("Access denied to this booking");
        }
        return booking;
    }

    private void requirePayHereMode() {
        if (!"PAYHERE".equalsIgnoreCase(String.valueOf(paymentMode).trim())) {
            throw new BadRequestException("Booking checkout requires PayHere sandbox mode (booknplay.payments.mode=PAYHERE).");
        }
    }

    private PaymentStatus getPaymentStatus(String bookingId) {
        return paymentRepository.findByBookingId(bookingId).map(Payment::getStatus).orElse(null);
    }

    private PaymentResponse mapPayment(Payment payment) {
        return PaymentResponse.builder().id(payment.getId()).bookingId(payment.getBooking().getId())
                .bookingRef(payment.getBooking().getBookingRef()).amount(payment.getAmount()).currency(payment.getCurrency())
                .paymentGateway(payment.getPaymentGateway()).gatewayReference(payment.getGatewayReference())
                .status(payment.getStatus()).createdAt(payment.getCreatedAt()).build();
    }

    @Override
    @Transactional
    public OwnerReviewResponse createBookingReview(String customerEmail, String bookingId, BookingReviewRequest request) {
        Booking booking = requireCustomerBooking(customerEmail, bookingId);
        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new ConflictException("A review already exists for this booking");
        }
        if (!isBookingReviewable(booking)) {
            throw new BadRequestException("This booking is not eligible for a review yet");
        }
        Customer customer = booking.getCustomer();
        String comment = trimToNull(request.getComment());
        Review review = Review.builder()
                .venue(booking.getVenue())
                .customer(customer)
                .booking(booking)
                .rating(request.getRating())
                .comment(comment)
                .build();
        Review saved = reviewRepository.save(review);
        String customerName = customer.getFirstName() + " " + customer.getLastName();
        return OwnerReviewResponse.builder()
                .id(saved.getId())
                .venueId(booking.getVenue().getId())
                .venueName(booking.getVenue().getName())
                .customerName(customerName)
                .rating(saved.getRating())
                .comment(saved.getComment())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private boolean isBookingReviewable(Booking booking) {
        BookingStatus status = booking.getStatus();
        if (status == BookingStatus.CANCELLED
                || status == BookingStatus.FAILED
                || status == BookingStatus.NO_SHOW
                || status == BookingStatus.PENDING) {
            return false;
        }
        if (status != BookingStatus.CONFIRMED && status != BookingStatus.COMPLETED) {
            return false;
        }
        LocalDateTime endAt = LocalDateTime.of(booking.getBookingDate(), booking.getEndTime());
        return !LocalDateTime.now().isBefore(endAt);
    }

    private BookingResponse mapToResponse(Booking booking, PaymentStatus paymentStatus) {
        boolean hasReview = reviewRepository.existsByBookingId(booking.getId());
        boolean canReview = !hasReview && isBookingReviewable(booking);
        return BookingResponse.builder().id(booking.getId()).bookingRef(booking.getBookingRef())
                .customerId(booking.getCustomer() == null ? null : booking.getCustomer().getId())
                .customerName(booking.getCustomer() == null ? booking.getGuestName()
                        : booking.getCustomer().getFirstName() + " " + booking.getCustomer().getLastName())
                .venueId(booking.getVenue().getId()).venueName(booking.getVenue().getName())
                .courtId(booking.getCourt().getId()).courtName(booking.getCourt().getName())
                .sportId(booking.getSport().getId()).sportName(booking.getSport().getName())
                .date(booking.getBookingDate()).startTime(booking.getStartTime()).endTime(booking.getEndTime())
                .slots(BookingTimeFormat.toSlotResponses(booking.getSlots()))
                .totalAmount(booking.getTotalAmount()).currency("LKR").status(booking.getStatus())
                .paymentStatus(paymentStatus).source(booking.getSource()).guestName(booking.getGuestName())
                .guestPhone(booking.getGuestPhone()).createdAt(booking.getCreatedAt())
                .invoiceAvailable(invoiceRepository.findByBookingId(booking.getId()).isPresent())
                .contactName(booking.getContactName()).contactEmail(booking.getContactEmail())
                .contactPhone(booking.getContactPhone()).specialRequests(booking.getSpecialRequests())
                .refundAmount(booking.getRefundAmount())
                .cancellationFee(booking.getCancellationFee())
                .hasReview(hasReview)
                .canReview(canReview)
                .build();
    }

    private void notifyAfterCommit(Customer customer, Booking booking, String event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notificationService.sendBookingNotification(customer, booking, event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                notificationService.sendBookingNotification(customer, booking, event);
            }
        });
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
