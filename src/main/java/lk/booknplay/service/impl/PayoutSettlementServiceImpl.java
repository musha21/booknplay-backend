package lk.booknplay.service.impl;

import lk.booknplay.dto.request.AdminBankVerificationRequest;
import lk.booknplay.dto.request.AdminPayoutConfigUpdateRequest;
import lk.booknplay.dto.request.AdminPayoutSettleRequest;
import lk.booknplay.dto.request.AdminPayoutStatusUpdateRequest;
import lk.booknplay.dto.response.AdminPayoutBusinessResponse;
import lk.booknplay.dto.response.OwnerPayoutSummaryResponse;
import lk.booknplay.dto.response.PlatformPayoutConfigResponse;
import lk.booknplay.dto.response.PayoutItemResponse;
import lk.booknplay.dto.response.PayoutResponse;
import lk.booknplay.entity.AdminAuditLog;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.Payment;
import lk.booknplay.entity.Payout;
import lk.booknplay.entity.PayoutItem;
import lk.booknplay.entity.PlatformPayoutConfig;
import lk.booknplay.entity.Refund;
import lk.booknplay.entity.User;
import lk.booknplay.enums.BankVerificationStatus;
import lk.booknplay.enums.BookingSource;
import lk.booknplay.enums.PaymentStatus;
import lk.booknplay.enums.PayoutFrequency;
import lk.booknplay.enums.PayoutStatus;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.repository.AdminAuditLogRepository;
import lk.booknplay.repository.BusinessRepository;
import lk.booknplay.repository.PaymentRepository;
import lk.booknplay.repository.PayoutItemRepository;
import lk.booknplay.repository.PayoutRepository;
import lk.booknplay.repository.PlatformPayoutConfigRepository;
import lk.booknplay.repository.RefundRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.service.FileStorageService;
import lk.booknplay.service.OwnerAccessService;
import lk.booknplay.service.PayoutSettlementService;
import lk.booknplay.service.PlanEntitlementService;
import lk.booknplay.service.SmsLenzService;
import lk.booknplay.enums.StaffPermission;
import lk.booknplay.util.BankAccountMasker;
import lk.booknplay.util.PayoutPeriodCalculator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayoutSettlementServiceImpl implements PayoutSettlementService {

    private static final Logger log = LoggerFactory.getLogger(PayoutSettlementServiceImpl.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");
    private static final List<PayoutStatus> OUTSTANDING_STATUSES =
            List.copyOf(EnumSet.of(PayoutStatus.PENDING, PayoutStatus.PROCESSING, PayoutStatus.ON_HOLD));

    private final BusinessRepository businessRepository;
    private final PayoutRepository payoutRepository;
    private final PayoutItemRepository payoutItemRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PlatformPayoutConfigRepository configRepository;
    private final AdminAuditLogRepository auditLogs;
    private final UserRepository userRepository;
    private final OwnerAccessService ownerAccessService;
    private final PlanEntitlementService planEntitlementService;
    private final FileStorageService fileStorageService;
    private final SmsLenzService smsLenzService;

    @Override
    @Transactional
    public int generateSettlements(LocalDate asOf) {
        LocalDate day = asOf != null ? asOf : LocalDate.now(PayoutPeriodCalculator.ZONE);
        int changed = 0;
        for (PayoutFrequency frequency : PayoutFrequency.values()) {
            PayoutPeriodCalculator.Period completed = PayoutPeriodCalculator.completedPeriod(frequency, day);
            PayoutPeriodCalculator.Period current = PayoutPeriodCalculator.currentPeriod(frequency, day);
            List<Business> businesses = businessRepository.findByPayoutFrequency(frequency);
            for (Business business : businesses) {
                if (refreshOrCreateSettlement(business, frequency, completed)) {
                    changed++;
                }
                if (!completed.start().equals(current.start()) || !completed.end().equals(current.end())) {
                    if (refreshOrCreateSettlement(business, frequency, current)) {
                        changed++;
                    }
                }
            }
        }
        return changed;
    }

    /**
     * Creates a PENDING payout for the period, or appends newly eligible payments onto an open
     * (non-PAID) payout. PAID rows are never mutated. Gateway fees are always zero; commission
     * comes from the business (plan-synced) percent.
     */
    private boolean refreshOrCreateSettlement(
            Business business, PayoutFrequency frequency, PayoutPeriodCalculator.Period period) {
        var existingOpt = payoutRepository.findByBusinessIdAndPeriodStartAndPeriodEnd(
                business.getId(), period.start(), period.end());
        if (existingOpt.isPresent() && existingOpt.get().getStatus() == PayoutStatus.PAID) {
            return false;
        }

        LocalDateTime fromInclusive = period.start().atStartOfDay();
        LocalDateTime toExclusive = period.end().plusDays(1).atStartOfDay();
        List<Payment> payments = paymentRepository.findEligibleForPayout(
                business.getId(),
                BookingSource.ONLINE,
                PaymentStatus.SUCCESS,
                fromInclusive,
                toExclusive);

        List<String> paymentIds = payments.stream().map(Payment::getId).toList();
        Set<String> alreadySettled = paymentIds.isEmpty()
                ? Set.of()
                : payoutItemRepository.findSettledPaymentIds(paymentIds);
        List<Payment> eligible = payments.stream()
                .filter(p -> !alreadySettled.contains(p.getId()))
                .toList();

        List<PayoutItem> newPrimaryItems = buildPrimaryItems(business, eligible);
        List<PayoutItem> adjustments = buildPostSettlementAdjustments(business.getId(), period);

        if (existingOpt.isEmpty()) {
            if (newPrimaryItems.isEmpty() && adjustments.isEmpty()) {
                return false;
            }
            List<PayoutItem> items = new ArrayList<>(newPrimaryItems);
            items.addAll(adjustments);
            Totals totals = sumItems(items);
            Payout payout = Payout.builder()
                    .business(business)
                    .periodStart(period.start())
                    .periodEnd(period.end())
                    .frequency(frequency)
                    .gross(totals.gross)
                    .refundsTotal(totals.refunds)
                    .commission(totals.commission)
                    .gatewayFees(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .net(totals.net)
                    .status(PayoutStatus.PENDING)
                    .build();
            Payout saved = payoutRepository.save(payout);
            for (PayoutItem item : items) {
                item.setPayout(saved);
            }
            payoutItemRepository.saveAll(items);
            return true;
        }

        Payout payout = existingOpt.get();
        if (!OUTSTANDING_STATUSES.contains(payout.getStatus())) {
            return false;
        }
        if (newPrimaryItems.isEmpty() && adjustments.isEmpty()) {
            return false;
        }

        List<PayoutItem> existingItems = payoutItemRepository.findByPayoutIdOrderByCreatedAtAsc(payout.getId());
        List<PayoutItem> toSave = new ArrayList<>();
        toSave.addAll(newPrimaryItems);
        toSave.addAll(adjustments);
        for (PayoutItem item : toSave) {
            item.setPayout(payout);
        }
        payoutItemRepository.saveAll(toSave);

        List<PayoutItem> allItems = new ArrayList<>(existingItems);
        allItems.addAll(toSave);
        Totals totals = sumItems(allItems);
        payout.setGross(totals.gross);
        payout.setRefundsTotal(totals.refunds);
        payout.setCommission(totals.commission);
        payout.setGatewayFees(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        payout.setNet(totals.net);
        payoutRepository.save(payout);
        return true;
    }

    private List<PayoutItem> buildPrimaryItems(Business business, List<Payment> eligible) {
        if (eligible.isEmpty()) {
            return List.of();
        }
        Map<String, Refund> refundsByPayment = refundRepository.findSucceededByPaymentIds(
                        eligible.stream().map(Payment::getId).toList())
                .stream()
                .collect(Collectors.toMap(r -> r.getPayment().getId(), r -> r, (a, b) -> a));

        BigDecimal commissionPercent = resolveCommissionPercent(business);
        List<PayoutItem> items = new ArrayList<>();
        for (Payment payment : eligible) {
            BigDecimal paymentGross = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
            Refund refund = refundsByPayment.get(payment.getId());
            BigDecimal refundAmount = refund != null && refund.getAmount() != null
                    ? refund.getAmount()
                    : BigDecimal.ZERO;
            BigDecimal commission = percentOf(paymentGross, commissionPercent);
            BigDecimal gatewayFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal net = paymentGross.subtract(refundAmount).subtract(commission).subtract(gatewayFee);
            items.add(PayoutItem.builder()
                    .paymentId(payment.getId())
                    .bookingId(payment.getBooking().getId())
                    .gross(paymentGross)
                    .refund(refundAmount)
                    .commission(commission)
                    .gatewayFee(gatewayFee)
                    .net(net)
                    .adjustment(false)
                    .build());
        }
        return items;
    }

    private static BigDecimal resolveCommissionPercent(Business business) {
        return business.getCommissionPercent() != null
                ? business.getCommissionPercent()
                : new BigDecimal("10.00");
    }

    private record Totals(BigDecimal gross, BigDecimal refunds, BigDecimal commission, BigDecimal net) {
    }

    private Totals sumItems(List<PayoutItem> items) {
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;
        BigDecimal commission = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        for (PayoutItem item : items) {
            if (!item.isAdjustment()) {
                gross = gross.add(nullToZero(item.getGross()));
                commission = commission.add(nullToZero(item.getCommission()));
            }
            refunds = refunds.add(nullToZero(item.getRefund()));
            net = net.add(nullToZero(item.getNet()));
        }
        return new Totals(
                gross.setScale(2, RoundingMode.HALF_UP),
                refunds.setScale(2, RoundingMode.HALF_UP),
                commission.setScale(2, RoundingMode.HALF_UP),
                net.setScale(2, RoundingMode.HALF_UP));
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** Net of SUCCESS online payments not yet on any payout line item. */
    private BigDecimal computeUnsettledAccrued(Business business) {
        List<Payment> payments = paymentRepository.findByBusinessSourceAndStatus(
                business.getId(), BookingSource.ONLINE, PaymentStatus.SUCCESS);
        if (payments.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        List<String> paymentIds = payments.stream().map(Payment::getId).toList();
        Set<String> settled = payoutItemRepository.findSettledPaymentIds(paymentIds);
        List<Payment> unsettled = payments.stream()
                .filter(p -> !settled.contains(p.getId()))
                .toList();
        if (unsettled.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        List<PayoutItem> items = buildPrimaryItems(business, unsettled);
        return sumItems(items).net();
    }

    /**
     * Refunds that succeeded after a payment was already included in a PAID payout become
     * negative adjustment items on the next open settlement (never mutate PAID payouts).
     */
    private List<PayoutItem> buildPostSettlementAdjustments(
            String businessId, PayoutPeriodCalculator.Period period) {
        LocalDateTime fromInclusive = period.start().atStartOfDay();
        LocalDateTime toExclusive = period.end().plusDays(1).atStartOfDay();
        List<Refund> refunds = refundRepository.findSucceededForBusinessInWindow(
                businessId, fromInclusive, toExclusive);
        if (refunds.isEmpty()) {
            return List.of();
        }

        List<PayoutItem> adjustments = new ArrayList<>();
        for (Refund refund : refunds) {
            if (payoutItemRepository.existsByRefundId(refund.getId())) {
                continue;
            }
            String paymentId = refund.getPayment().getId();
            if (!payoutItemRepository.existsByPaymentIdAndAdjustmentFalse(paymentId)) {
                continue;
            }
            if (payoutItemRepository.findPaidPrimaryItemsByPaymentId(paymentId).isEmpty()) {
                continue;
            }
            BigDecimal refundAmount = refund.getAmount() != null ? refund.getAmount() : BigDecimal.ZERO;
            adjustments.add(PayoutItem.builder()
                    .paymentId(paymentId)
                    .bookingId(refund.getBooking().getId())
                    .refundId(refund.getId())
                    .gross(BigDecimal.ZERO)
                    .refund(refundAmount)
                    .commission(BigDecimal.ZERO)
                    .gatewayFee(BigDecimal.ZERO)
                    .net(refundAmount.negate())
                    .adjustment(true)
                    .build());
        }
        return adjustments;
    }

    private BigDecimal percentOf(BigDecimal amount, BigDecimal percent) {
        if (amount == null || percent == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private PlatformPayoutConfig getOrCreateConfig() {
        return configRepository.findById(PlatformPayoutConfig.SINGLETON_ID)
                .orElseGet(() -> configRepository.save(PlatformPayoutConfig.builder()
                        .id(PlatformPayoutConfig.SINGLETON_ID)
                        .gatewayFeePercent(BigDecimal.ZERO)
                        .build()));
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerPayoutSummaryResponse ownerSummary(String ownerEmail) {
        ownerAccessService.requireStaffPermission(ownerEmail, StaffPermission.EARNINGS);
        Business business = ownerAccessService.requireBusiness(ownerEmail);
        planEntitlementService.assertEarnings(business);
        PayoutFrequency frequency = business.getPayoutFrequency() != null
                ? business.getPayoutFrequency()
                : PayoutFrequency.WEEKLY;
        PayoutPeriodCalculator.Period upcoming = PayoutPeriodCalculator.currentPeriod(
                frequency, LocalDate.now(PayoutPeriodCalculator.ZONE));
        BigDecimal outstanding = payoutRepository.sumNetByBusinessAndStatuses(
                business.getId(), OUTSTANDING_STATUSES);
        return OwnerPayoutSummaryResponse.builder()
                .outstandingBalance(outstanding)
                .upcomingPeriodStart(upcoming.start())
                .upcomingPeriodEnd(upcoming.end())
                .payoutFrequency(frequency)
                .bankVerificationStatus(business.getBankVerificationStatus() != null
                        ? business.getBankVerificationStatus()
                        : BankVerificationStatus.UNVERIFIED)
                .bankName(business.getBankName())
                .bankAccountName(business.getBankAccountName())
                .bankAccountNumberMasked(BankAccountMasker.mask(business.getBankAccountNumber()))
                .bankBranch(business.getBankBranch())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayoutResponse> listOwnerPayouts(String ownerEmail) {
        ownerAccessService.requireStaffPermission(ownerEmail, StaffPermission.EARNINGS);
        Business business = ownerAccessService.requireBusiness(ownerEmail);
        planEntitlementService.assertEarnings(business);
        return payoutRepository.findByBusinessIdOrderByPeriodStartDesc(business.getId()).stream()
                .map(p -> mapPayout(p, false, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PayoutResponse getOwnerPayout(String ownerEmail, String payoutId) {
        ownerAccessService.requireStaffPermission(ownerEmail, StaffPermission.EARNINGS);
        Business business = ownerAccessService.requireBusiness(ownerEmail);
        planEntitlementService.assertEarnings(business);
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found"));
        if (!payout.getBusiness().getId().equals(business.getId())) {
            throw new ResourceNotFoundException("Payout not found");
        }
        return mapPayout(payout, true, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminPayoutBusinessResponse> listBusinesses() {
        List<Business> businesses = businessRepository.findAll();
        List<AdminPayoutBusinessResponse> rows = new ArrayList<>();
        for (Business business : businesses) {
            BigDecimal outstanding = payoutRepository.sumNetByBusinessAndStatuses(
                    business.getId(), OUTSTANDING_STATUSES);
            List<Payout> payouts = payoutRepository.findByBusinessIdOrderByPeriodStartDesc(business.getId());
            Payout latest = payouts.isEmpty() ? null : payouts.get(0);
            String ownerEmail = userRepository.findById(business.getOwnerId())
                    .map(User::getEmail)
                    .orElse(null);
            rows.add(AdminPayoutBusinessResponse.builder()
                    .businessId(business.getId())
                    .businessName(business.getName())
                    .ownerEmail(ownerEmail)
                    .payoutFrequency(business.getPayoutFrequency())
                    .bankVerificationStatus(business.getBankVerificationStatus())
                    .bankName(business.getBankName())
                    .bankAccountName(business.getBankAccountName())
                    .bankAccountNumberMasked(BankAccountMasker.mask(business.getBankAccountNumber()))
                    .bankAccountNumber(business.getBankAccountNumber())
                    .bankBranch(business.getBankBranch())
                    .outstandingBalance(outstanding)
                    .unsettledAccrued(computeUnsettledAccrued(business))
                    .commissionPercent(resolveCommissionPercent(business))
                    .latestPayoutId(latest != null ? latest.getId() : null)
                    .latestPayoutStatus(latest != null ? latest.getStatus() : null)
                    .latestPeriodStart(latest != null ? latest.getPeriodStart() : null)
                    .latestPeriodEnd(latest != null ? latest.getPeriodEnd() : null)
                    .latestNet(latest != null ? latest.getNet() : null)
                    .build());
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayoutResponse> listAdminPayouts(
            String businessId, PayoutStatus status, PayoutFrequency frequency, Pageable pageable) {
        return payoutRepository.findFiltered(
                        blankToNull(businessId), status, frequency, pageable)
                .map(p -> mapPayout(p, false, true));
    }

    @Override
    @Transactional(readOnly = true)
    public PayoutResponse getAdminPayout(String payoutId) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found"));
        return mapPayout(payout, true, true);
    }

    @Override
    @Transactional
    public PayoutResponse updateStatus(String adminEmail, String payoutId, AdminPayoutStatusUpdateRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new BadRequestException("status is required");
        }
        PayoutStatus next = request.getStatus();
        if (next != PayoutStatus.PROCESSING && next != PayoutStatus.ON_HOLD && next != PayoutStatus.PENDING) {
            throw new BadRequestException("Use settle endpoint to mark PAID");
        }
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found"));
        if (payout.getStatus() == PayoutStatus.PAID) {
            throw new BadRequestException("Paid payouts cannot change status");
        }
        payout.setStatus(next);
        if (request.getReason() != null && !request.getReason().isBlank()) {
            payout.setNote(request.getReason().trim());
        }
        Payout saved = payoutRepository.save(payout);
        audit(adminEmail, "PAYOUT_STATUS_CHANGED", payoutId,
                request.getReason() != null ? request.getReason() : next.name());
        return mapPayout(saved, true, true);
    }

    @Override
    @Transactional
    public PayoutResponse settle(
            String adminEmail, String payoutId, AdminPayoutSettleRequest request, MultipartFile receipt) {
        if (request == null) {
            throw new BadRequestException("Settlement details are required");
        }
        if (request.getTransferReference() == null || request.getTransferReference().isBlank()) {
            throw new BadRequestException("transferReference is required");
        }
        if (request.getTransferDate() == null) {
            throw new BadRequestException("transferDate is required");
        }
        if (request.getTransferAmount() == null) {
            throw new BadRequestException("transferAmount is required");
        }
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found"));
        if (payout.getStatus() == PayoutStatus.PAID) {
            throw new BadRequestException("Payout is already paid");
        }
        if (payout.getStatus() == PayoutStatus.ON_HOLD) {
            throw new BadRequestException("Release hold before settling");
        }

        Business business = payout.getBusiness();
        boolean verified = business.getBankVerificationStatus() == BankVerificationStatus.VERIFIED;
        if (!verified) {
            if (request.getOverrideReason() == null || request.getOverrideReason().isBlank()) {
                throw new BadRequestException("Bank account is not verified; provide overrideReason to settle");
            }
        }

        BigDecimal expected = payout.getNet() != null ? payout.getNet() : BigDecimal.ZERO;
        if (request.getTransferAmount().subtract(expected).abs().compareTo(AMOUNT_TOLERANCE) > 0) {
            throw new BadRequestException("transferAmount must match payout net (" + expected + ")");
        }

        String receiptUrl = payout.getReceiptUrl();
        if (receipt != null && !receipt.isEmpty()) {
            receiptUrl = fileStorageService.store(receipt, "payout-receipts");
        }

        payout.setTransferReference(request.getTransferReference().trim());
        payout.setTransferDate(request.getTransferDate());
        payout.setTransferAmount(request.getTransferAmount());
        payout.setReceiptUrl(receiptUrl);
        payout.setPaidAt(LocalDateTime.now());
        payout.setPaidByAdminEmail(adminEmail);
        payout.setStatus(PayoutStatus.PAID);
        if (request.getNote() != null && !request.getNote().isBlank()) {
            payout.setNote(request.getNote().trim());
        }
        Payout saved = payoutRepository.save(payout);

        String reason = "ref=" + saved.getTransferReference()
                + (verified ? "" : "; override=" + request.getOverrideReason().trim());
        audit(adminEmail, "PAYOUT_MARKED_PAID", payoutId, reason);
        notifyBusinessPayoutPaid(business, saved);
        return mapPayout(saved, true, true);
    }

    /** Fail-soft SMS so delivery issues never undo a recorded settlement. */
    private void notifyBusinessPayoutPaid(Business business, Payout payout) {
        String phone = business.getContactPhone();
        if (phone == null || phone.isBlank()) {
            log.info("Skipping payout paid SMS: no contactPhone for businessId={}", business.getId());
            return;
        }
        String message = String.format(
                "BooknPlay: Payout of LKR %s for %s–%s has been paid. Ref: %s",
                payout.getNet() != null ? payout.getNet().toPlainString() : "0",
                payout.getPeriodStart(),
                payout.getPeriodEnd(),
                payout.getTransferReference() != null ? payout.getTransferReference() : "n/a"
        );
        try {
            smsLenzService.sendSms(phone.trim(), message);
            log.info("Payout paid SMS sent. payoutId={}, businessId={}", payout.getId(), business.getId());
        } catch (Exception ex) {
            log.error("Payout paid SMS failed. payoutId={}, businessId={}, error={}",
                    payout.getId(), business.getId(), ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional
    public void verifyBank(String adminEmail, String businessId, AdminBankVerificationRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new BadRequestException("status is required");
        }
        BankVerificationStatus status = request.getStatus();
        if (status != BankVerificationStatus.VERIFIED
                && status != BankVerificationStatus.UNVERIFIED
                && status != BankVerificationStatus.PENDING_REVIEW) {
            throw new BadRequestException("Invalid bank verification status");
        }
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        business.setBankVerificationStatus(status);
        if (status == BankVerificationStatus.VERIFIED) {
            business.setBankVerifiedAt(LocalDateTime.now());
            business.setBankVerifiedBy(adminEmail);
        } else {
            business.setBankVerifiedAt(null);
            business.setBankVerifiedBy(null);
        }
        businessRepository.save(business);
        audit(adminEmail, "BANK_VERIFICATION_CHANGED", businessId,
                request.getReason() != null ? request.getReason() : status.name());
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformPayoutConfigResponse getConfig() {
        PlatformPayoutConfig config = getOrCreateConfig();
        return PlatformPayoutConfigResponse.builder()
                .gatewayFeePercent(config.getGatewayFeePercent())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public PlatformPayoutConfigResponse updateConfig(String adminEmail, AdminPayoutConfigUpdateRequest request) {
        if (request == null || request.getGatewayFeePercent() == null) {
            throw new BadRequestException("gatewayFeePercent is required");
        }
        if (request.getGatewayFeePercent().compareTo(BigDecimal.ZERO) < 0
                || request.getGatewayFeePercent().compareTo(HUNDRED) > 0) {
            throw new BadRequestException("gatewayFeePercent must be between 0 and 100");
        }
        PlatformPayoutConfig config = getOrCreateConfig();
        config.setGatewayFeePercent(request.getGatewayFeePercent().setScale(2, RoundingMode.HALF_UP));
        PlatformPayoutConfig saved = configRepository.save(config);
        audit(adminEmail, "PAYOUT_CONFIG_UPDATED", saved.getId(),
                request.getReason() != null ? request.getReason() : "gatewayFeePercent=" + saved.getGatewayFeePercent());
        return PlatformPayoutConfigResponse.builder()
                .gatewayFeePercent(saved.getGatewayFeePercent())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    private PayoutResponse mapPayout(Payout payout, boolean includeItems, boolean adminView) {
        List<PayoutItemResponse> items = null;
        if (includeItems) {
            items = payoutItemRepository.findByPayoutIdOrderByCreatedAtAsc(payout.getId()).stream()
                    .map(this::mapItem)
                    .toList();
        }
        return PayoutResponse.builder()
                .id(payout.getId())
                .businessId(payout.getBusiness().getId())
                .businessName(payout.getBusiness().getName())
                .periodStart(payout.getPeriodStart())
                .periodEnd(payout.getPeriodEnd())
                .frequency(payout.getFrequency())
                .gross(payout.getGross())
                .refundsTotal(payout.getRefundsTotal())
                .commission(payout.getCommission())
                .gatewayFees(payout.getGatewayFees())
                .net(payout.getNet())
                .status(payout.getStatus())
                .note(payout.getNote())
                .transferReference(payout.getTransferReference())
                .transferDate(payout.getTransferDate())
                .transferAmount(payout.getTransferAmount())
                .receiptUrl(payout.getReceiptUrl())
                .paidAt(payout.getPaidAt())
                .createdAt(payout.getCreatedAt())
                .items(items)
                .build();
    }

    private PayoutItemResponse mapItem(PayoutItem item) {
        return PayoutItemResponse.builder()
                .id(item.getId())
                .paymentId(item.getPaymentId())
                .bookingId(item.getBookingId())
                .gross(item.getGross())
                .refund(item.getRefund())
                .commission(item.getCommission())
                .gatewayFee(item.getGatewayFee())
                .net(item.getNet())
                .adjustment(item.isAdjustment())
                .build();
    }

    private void audit(String email, String action, String id, String reason) {
        auditLogs.save(AdminAuditLog.builder()
                .adminEmail(email)
                .action(action)
                .resourceType("PAYOUT")
                .resourceId(id)
                .reason(reason)
                .build());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
