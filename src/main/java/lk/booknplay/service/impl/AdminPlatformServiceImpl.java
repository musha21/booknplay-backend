package lk.booknplay.service.impl;

import lk.booknplay.dto.response.*;
import lk.booknplay.entity.*;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.repository.*;
import lk.booknplay.service.AdminPlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service @RequiredArgsConstructor
public class AdminPlatformServiceImpl implements AdminPlatformService {
    private final CustomerRepository customers;
    private final BusinessRepository businesses;
    private final VenueRepository venues;
    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final AdminAuditLogRepository auditLogs;

    @Override @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        return AdminDashboardResponse.builder().customers(customers.count()).businesses(businesses.count())
                .venues(venues.count()).pendingVenues(venues.countByStatus(VenueStatus.PENDING_APPROVAL))
                .bookings(bookings.count()).payments(payments.count()).grossBookingValue(payments.sumSuccessfulPayments()).build();
    }

    @Override @Transactional(readOnly = true)
    public Page<AdminBusinessResponse> businesses(Pageable pageable) { return businesses.findAll(pageable).map(this::business); }

    @Override @Transactional
    public AdminBusinessResponse setBusinessAccess(String id, boolean enabled, boolean locked, String reason, String adminEmail) {
        Business entity = findBusiness(id);
        User owner = users.findById(entity.getOwnerId()).orElseThrow(() -> new ResourceNotFoundException("Business owner not found"));
        owner.setEnabled(enabled); owner.setLocked(locked); users.save(owner);
        audit(adminEmail, "BUSINESS_ACCESS_CHANGED", "BUSINESS", id, reason);
        return business(entity);
    }

    @Override @Transactional
    public AdminBusinessResponse setCommission(String id, BigDecimal value, String reason, String adminEmail) {
        if (value == null || value.signum() < 0 || value.compareTo(new BigDecimal("100")) > 0) throw new BadRequestException("Commission must be between 0 and 100");
        Business entity = findBusiness(id); entity.setCommissionPercent(value); businesses.save(entity);
        audit(adminEmail, "COMMISSION_CHANGED", "BUSINESS", id, reason);
        return business(entity);
    }

    @Override @Transactional(readOnly = true)
    public Page<VenueResponse> venues(Pageable pageable) { return venues.findAll(pageable).map(this::venue); }

    @Override @Transactional
    public VenueResponse setVenueStatus(String id, VenueStatus status, String reason, String adminEmail) {
        Venue entity = venues.findById(id).orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        entity.setStatus(status); venues.save(entity); audit(adminEmail, "VENUE_STATUS_CHANGED", "VENUE", id, reason); return venue(entity);
    }

    @Override @Transactional(readOnly = true)
    public Page<AdminAuditLog> audit(Pageable pageable) { return auditLogs.findAllByOrderByCreatedAtDesc(pageable); }

    private Business findBusiness(String id) { return businesses.findById(id).orElseThrow(() -> new ResourceNotFoundException("Business not found")); }
    private AdminBusinessResponse business(Business b) {
        User owner = users.findById(b.getOwnerId()).orElse(null);
        return AdminBusinessResponse.builder().id(b.getId()).name(b.getName()).ownerId(b.getOwnerId())
                .ownerEmail(owner == null ? null : owner.getEmail()).ownerName(b.getOwnerName()).contactEmail(b.getContactEmail())
                .contactPhone(b.getContactPhone()).commissionPercent(b.getCommissionPercent())
                .enabled(owner != null && owner.isEnabled()).locked(owner != null && !owner.isAccountNonLocked())
                .venueCount(venues.findByBusinessId(b.getId()).stream().filter(v -> v.getStatus() == VenueStatus.ACTIVE || v.getStatus() == VenueStatus.APPROVED).count())
                .logoUrl(b.getLogoUrl())
                .imageUrls(b.getImages() == null ? List.of() : b.getImages().stream().map(BusinessImage::getUrl).filter(url -> url != null && !url.isBlank()).toList())
                .build();
    }
    private VenueResponse venue(Venue v) { return VenueResponse.builder().id(v.getId()).businessId(v.getBusiness().getId()).businessName(v.getBusiness().getName()).name(v.getName()).city(v.getCity()).venueType(v.getVenueType()).status(v.getStatus()).coverImageUrl(v.getCoverImageUrl()).build(); }
    private void audit(String email, String action, String type, String id, String reason) { auditLogs.save(AdminAuditLog.builder().adminEmail(email).action(action).resourceType(type).resourceId(id).reason(reason).build()); }
}
