package lk.booknplay.service.impl;

import lk.booknplay.dto.request.OwnerStaffInviteRequest;
import lk.booknplay.dto.request.OwnerStaffUpdateRequest;
import lk.booknplay.dto.response.OwnerStaffResponse;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.BusinessStaff;
import lk.booknplay.entity.User;
import lk.booknplay.enums.Role;
import lk.booknplay.exception.ConflictException;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.repository.BusinessStaffRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.service.OwnerAccessService;
import lk.booknplay.service.OwnerStaffService;
import lk.booknplay.service.PlanEntitlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerStaffServiceImpl implements OwnerStaffService {

    private final OwnerAccessService ownerAccessService;
    private final BusinessStaffRepository staffRepository;
    private final UserRepository userRepository;
    private final PlanEntitlementService planEntitlementService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<OwnerStaffResponse> list(String ownerEmail) {
        Business business = ownerAccessService.requireOwner(ownerEmail);
        Integer maxStaff = resolveMaxStaff(business);
        long activeCount = staffRepository.countByBusinessIdAndActiveTrue(business.getId());
        return staffRepository.findByBusinessIdOrderByCreatedAtDesc(business.getId()).stream()
                .map(staff -> toResponse(staff, maxStaff, activeCount))
                .toList();
    }

    @Override
    @Transactional
    public OwnerStaffResponse invite(String ownerEmail, OwnerStaffInviteRequest request) {
        Business business = ownerAccessService.requireOwner(ownerEmail);
        planEntitlementService.assertCanAddStaff(business);

        String email = request.getEmail().trim().toLowerCase();
        if (staffRepository.existsByBusinessIdAndEmailIgnoreCase(business.getId(), email)) {
            throw new ConflictException("STAFF_EXISTS", "A staff member with this email already exists");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("EMAIL_IN_USE", "This email is already registered");
        }

        User user = userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getTemporaryPassword()))
                .role(Role.STAFF)
                .isEnabled(true)
                .isLocked(false)
                .build());

        BusinessStaff staff = staffRepository.save(BusinessStaff.builder()
                .businessId(business.getId())
                .userId(user.getId())
                .email(email)
                .name(request.getName().trim())
                .active(true)
                .canCalendar(bool(request.getCanCalendar(), true))
                .canWalkIns(bool(request.getCanWalkIns(), true))
                .canCourts(bool(request.getCanCourts(), true))
                .canReports(bool(request.getCanReports(), false))
                .canEarnings(bool(request.getCanEarnings(), false))
                .canVenues(bool(request.getCanVenues(), false))
                .canBilling(bool(request.getCanBilling(), false))
                .build());

        Integer maxStaff = resolveMaxStaff(business);
        long activeCount = staffRepository.countByBusinessIdAndActiveTrue(business.getId());
        return toResponse(staff, maxStaff, activeCount);
    }

    @Override
    @Transactional
    public OwnerStaffResponse update(String ownerEmail, String staffId, OwnerStaffUpdateRequest request) {
        Business business = ownerAccessService.requireOwner(ownerEmail);
        BusinessStaff staff = staffRepository.findByIdAndBusinessId(staffId, business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            staff.setName(request.getName().trim());
        }
        if (request.getActive() != null) {
            if (Boolean.TRUE.equals(request.getActive()) && !staff.isActive()) {
                planEntitlementService.assertCanAddStaff(business);
            }
            staff.setActive(request.getActive());
            userRepository.findById(staff.getUserId()).ifPresent(user -> {
                user.setEnabled(Boolean.TRUE.equals(request.getActive()));
                userRepository.save(user);
            });
        }
        if (request.getCanCalendar() != null) staff.setCanCalendar(request.getCanCalendar());
        if (request.getCanWalkIns() != null) staff.setCanWalkIns(request.getCanWalkIns());
        if (request.getCanCourts() != null) staff.setCanCourts(request.getCanCourts());
        if (request.getCanReports() != null) staff.setCanReports(request.getCanReports());
        if (request.getCanEarnings() != null) staff.setCanEarnings(request.getCanEarnings());
        if (request.getCanVenues() != null) staff.setCanVenues(request.getCanVenues());
        if (request.getCanBilling() != null) staff.setCanBilling(request.getCanBilling());

        staffRepository.save(staff);
        Integer maxStaff = resolveMaxStaff(business);
        long activeCount = staffRepository.countByBusinessIdAndActiveTrue(business.getId());
        return toResponse(staff, maxStaff, activeCount);
    }

    @Override
    @Transactional
    public void delete(String ownerEmail, String staffId) {
        Business business = ownerAccessService.requireOwner(ownerEmail);
        BusinessStaff staff = staffRepository.findByIdAndBusinessId(staffId, business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));
        staff.setActive(false);
        staffRepository.save(staff);
        userRepository.findById(staff.getUserId()).ifPresent(user -> {
            user.setEnabled(false);
            userRepository.save(user);
        });
    }

    private Integer resolveMaxStaff(Business business) {
        return planEntitlementService.resolvePlan(business).getMaxStaff();
    }

    private static boolean bool(Boolean value, boolean fallback) {
        return value != null ? value : fallback;
    }

    private OwnerStaffResponse toResponse(BusinessStaff staff, Integer maxStaff, long activeCount) {
        return OwnerStaffResponse.builder()
                .id(staff.getId())
                .userId(staff.getUserId())
                .email(staff.getEmail())
                .name(staff.getName())
                .active(staff.isActive())
                .canCalendar(staff.isCanCalendar())
                .canWalkIns(staff.isCanWalkIns())
                .canCourts(staff.isCanCourts())
                .canReports(staff.isCanReports())
                .canEarnings(staff.isCanEarnings())
                .canVenues(staff.isCanVenues())
                .canBilling(staff.isCanBilling())
                .createdAt(staff.getCreatedAt())
                .maxStaff(maxStaff)
                .activeStaffCount(activeCount)
                .build();
    }
}
