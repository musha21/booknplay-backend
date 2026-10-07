package lk.booknplay.service.impl;

import lk.booknplay.entity.Business;
import lk.booknplay.entity.BusinessStaff;
import lk.booknplay.entity.BusinessSubscription;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.User;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.Role;
import lk.booknplay.enums.StaffPermission;
import lk.booknplay.exception.ForbiddenException;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.exception.UnauthorizedException;
import lk.booknplay.repository.BusinessRepository;
import lk.booknplay.repository.BusinessStaffRepository;
import lk.booknplay.repository.CourtRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.repository.VenueRepository;
import lk.booknplay.service.OwnerAccessService;
import lk.booknplay.service.OwnerSubscriptionService;
import lk.booknplay.util.SubscriptionAccess;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class OwnerAccessServiceImpl implements OwnerAccessService {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final BusinessStaffRepository businessStaffRepository;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final OwnerSubscriptionService ownerSubscriptionService;

    public OwnerAccessServiceImpl(
            UserRepository userRepository,
            BusinessRepository businessRepository,
            BusinessStaffRepository businessStaffRepository,
            VenueRepository venueRepository,
            CourtRepository courtRepository,
            @Lazy OwnerSubscriptionService ownerSubscriptionService) {
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.businessStaffRepository = businessStaffRepository;
        this.venueRepository = venueRepository;
        this.courtRepository = courtRepository;
        this.ownerSubscriptionService = ownerSubscriptionService;
    }

    @Override
    public Business requireBusiness(String ownerEmail) {
        User user = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UnauthorizedException("Owner user not found"));
        if (user.getRole() == Role.BUSINESS_OWNER) {
            return businessRepository.findByOwnerId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Business not found for this owner"));
        }
        if (user.getRole() == Role.STAFF) {
            BusinessStaff staff = businessStaffRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ForbiddenException("Staff membership not found"));
            if (!staff.isActive()) {
                throw new ForbiddenException("Staff account is inactive");
            }
            return businessRepository.findById(staff.getBusinessId())
                    .orElseThrow(() -> new ResourceNotFoundException("Business not found for this staff member"));
        }
        throw new ForbiddenException("Not an owner portal account");
    }

    @Override
    public Business requireOwner(String ownerEmail) {
        User user = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UnauthorizedException("Owner user not found"));
        if (user.getRole() != Role.BUSINESS_OWNER) {
            throw new ForbiddenException("Only the business owner can perform this action");
        }
        return businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found for this owner"));
    }

    @Override
    public Venue requireVenue(String ownerEmail, String venueId) {
        Business business = requireBusiness(ownerEmail);
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + venueId));
        if (!venue.getBusiness().getId().equals(business.getId())) {
            throw new ForbiddenException("Venue does not belong to this business");
        }
        return venue;
    }

    @Override
    public Court requireCourt(String ownerEmail, String courtId) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Court not found with id: " + courtId));
        requireVenue(ownerEmail, court.getVenue().getId());
        return court;
    }

    @Override
    public Business requireMutableBusiness(String ownerEmail) {
        Business business = requireBusiness(ownerEmail);
        requireMutableSubscription(business);
        return business;
    }

    @Override
    public Business requireMutableOwner(String ownerEmail) {
        Business business = requireOwner(ownerEmail);
        requireMutableSubscription(business);
        return business;
    }

    @Override
    public Venue requireMutableVenue(String ownerEmail, String venueId) {
        Venue venue = requireVenue(ownerEmail, venueId);
        requireMutableSubscription(venue.getBusiness());
        return venue;
    }

    @Override
    public Court requireMutableCourt(String ownerEmail, String courtId) {
        Court court = requireCourt(ownerEmail, courtId);
        requireMutableSubscription(court.getVenue().getBusiness());
        return court;
    }

    @Override
    public void requireMutableSubscription(Business business) {
        if (business == null) {
            throw new ForbiddenException("Business subscription is required");
        }
        BusinessSubscription subscription = ownerSubscriptionService.ensureSubscription(business);
        if (!SubscriptionAccess.canMutate(subscription, LocalDateTime.now())) {
            throw new ForbiddenException(
                    "Your free trial has ended. Subscribe on Billing to create or edit venues, courts, and calendar.");
        }
    }

    @Override
    public void requireStaffPermission(String ownerEmail, StaffPermission permission) {
        User user = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UnauthorizedException("Owner user not found"));
        if (user.getRole() == Role.BUSINESS_OWNER) {
            return;
        }
        if (user.getRole() != Role.STAFF) {
            throw new ForbiddenException("Not an owner portal account");
        }
        BusinessStaff staff = businessStaffRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("Staff membership not found"));
        if (!staff.isActive()) {
            throw new ForbiddenException("Staff account is inactive");
        }
        boolean allowed = switch (permission) {
            case CALENDAR -> staff.isCanCalendar();
            case WALK_INS -> staff.isCanWalkIns();
            case COURTS -> staff.isCanCourts();
            case REPORTS -> staff.isCanReports();
            case EARNINGS -> staff.isCanEarnings();
            case VENUES -> staff.isCanVenues();
            case BILLING -> staff.isCanBilling();
        };
        if (!allowed) {
            throw new ForbiddenException("STAFF_PERMISSION",
                    "Your staff role does not allow " + permission.name().toLowerCase().replace('_', ' '));
        }
    }
}
