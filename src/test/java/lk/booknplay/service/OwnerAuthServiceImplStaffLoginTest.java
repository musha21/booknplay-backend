package lk.booknplay.service;

import lk.booknplay.dto.request.CustomerLoginRequest;
import lk.booknplay.dto.response.AuthResponse;
import lk.booknplay.dto.response.OwnerResponse;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.BusinessStaff;
import lk.booknplay.entity.User;
import lk.booknplay.enums.Role;
import lk.booknplay.exception.ForbiddenException;
import lk.booknplay.exception.UnauthorizedException;
import lk.booknplay.repository.BusinessImageRepository;
import lk.booknplay.repository.BusinessRepository;
import lk.booknplay.repository.BusinessStaffRepository;
import lk.booknplay.repository.RefreshTokenRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.security.jwt.JwtProperties;
import lk.booknplay.security.jwt.JwtTokenProvider;
import lk.booknplay.service.impl.OwnerAuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerAuthServiceImplStaffLoginTest {

    @Mock private UserRepository userRepository;
    @Mock private BusinessRepository businessRepository;
    @Mock private BusinessStaffRepository businessStaffRepository;
    @Mock private BusinessImageRepository businessImageRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private JwtProperties jwtProperties;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private FileStorageService fileStorageService;
    @Mock private OwnerAccessService ownerAccessService;
    @Mock private OwnerSubscriptionService ownerSubscriptionService;

    @InjectMocks
    private OwnerAuthServiceImpl ownerAuthService;

    private Business business;
    private User staffUser;
    private User ownerUser;

    @BeforeEach
    void setUp() {
        business = Business.builder()
                .id("biz-1")
                .ownerId("u-owner")
                .name("Arena Co")
                .ownerName("Owner Person")
                .build();
        staffUser = User.builder()
                .id("u-staff")
                .email("staff@example.com")
                .role(Role.STAFF)
                .isEnabled(true)
                .isLocked(false)
                .build();
        ownerUser = User.builder()
                .id("u-owner")
                .email("owner@example.com")
                .role(Role.BUSINESS_OWNER)
                .isEnabled(true)
                .isLocked(false)
                .build();
    }

    @Test
    void login_StaffWithActiveMembership_ReturnsStaffRoleAndDisplayName() {
        CustomerLoginRequest request = new CustomerLoginRequest();
        request.setEmail("Staff@Example.com");
        request.setPassword("temp-pass-1");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("staff@example.com", "temp-pass-1"));
        when(userRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(staffUser));
        when(ownerAccessService.requireBusiness("staff@example.com")).thenReturn(business);
        when(tokenProvider.generateAccessToken(eq(staffUser), isNull(), eq("biz-1"))).thenReturn("access");
        when(jwtProperties.getAccessTokenExpiryMs()).thenReturn(3600000L);
        when(jwtProperties.getRefreshTokenExpiryMs()).thenReturn(86400000L);
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessImageRepository.findByBusinessIdOrderBySortOrderAsc("biz-1")).thenReturn(List.of());
        when(businessStaffRepository.findByUserId("u-staff")).thenReturn(Optional.of(
                BusinessStaff.builder()
                        .id("s-1")
                        .businessId("biz-1")
                        .userId("u-staff")
                        .email("staff@example.com")
                        .name("Desk Staff")
                        .active(true)
                        .build()
        ));

        AuthResponse response = ownerAuthService.login(request);

        assertEquals(Role.STAFF, response.getRole());
        assertEquals("access", response.getAccessToken());
        assertEquals("Desk Staff", response.getOwner().getOwnerName());
        assertEquals("Arena Co", response.getOwner().getBusinessName());
        assertEquals("biz-1", response.getOwner().getBusinessId());
        verify(businessRepository, never()).findByOwnerId(any());
    }

    @Test
    void login_StaffWithoutMembership_PropagatesForbidden() {
        CustomerLoginRequest request = new CustomerLoginRequest();
        request.setEmail("staff@example.com");
        request.setPassword("temp-pass-1");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("staff@example.com", "temp-pass-1"));
        when(userRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(staffUser));
        when(ownerAccessService.requireBusiness("staff@example.com"))
                .thenThrow(new ForbiddenException("Staff membership not found"));

        assertThrows(ForbiddenException.class, () -> ownerAuthService.login(request));
    }

    @Test
    void login_InactiveStaff_PropagatesForbidden() {
        CustomerLoginRequest request = new CustomerLoginRequest();
        request.setEmail("staff@example.com");
        request.setPassword("temp-pass-1");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("staff@example.com", "temp-pass-1"));
        when(userRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(staffUser));
        when(ownerAccessService.requireBusiness("staff@example.com"))
                .thenThrow(new ForbiddenException("Staff account is inactive"));

        assertThrows(ForbiddenException.class, () -> ownerAuthService.login(request));
    }

    @Test
    void login_BusinessOwner_StillResolvesBusiness() {
        CustomerLoginRequest request = new CustomerLoginRequest();
        request.setEmail("owner@example.com");
        request.setPassword("owner-pass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("owner@example.com", "owner-pass"));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(ownerAccessService.requireBusiness("owner@example.com")).thenReturn(business);
        when(tokenProvider.generateAccessToken(eq(ownerUser), isNull(), eq("biz-1"))).thenReturn("owner-access");
        when(jwtProperties.getAccessTokenExpiryMs()).thenReturn(3600000L);
        when(jwtProperties.getRefreshTokenExpiryMs()).thenReturn(86400000L);
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessImageRepository.findByBusinessIdOrderBySortOrderAsc("biz-1")).thenReturn(List.of());

        AuthResponse response = ownerAuthService.login(request);

        assertEquals(Role.BUSINESS_OWNER, response.getRole());
        assertEquals("Owner Person", response.getOwner().getOwnerName());
        assertEquals("owner-access", response.getAccessToken());
    }

    @Test
    void getCurrentOwner_Staff_UsesStaffDisplayName() {
        when(userRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(staffUser));
        when(ownerAccessService.requireBusiness("staff@example.com")).thenReturn(business);
        when(businessImageRepository.findByBusinessIdOrderBySortOrderAsc("biz-1")).thenReturn(List.of());
        when(businessStaffRepository.findByUserId("u-staff")).thenReturn(Optional.of(
                BusinessStaff.builder()
                        .id("s-1")
                        .businessId("biz-1")
                        .userId("u-staff")
                        .email("staff@example.com")
                        .name("Desk Staff")
                        .active(true)
                        .build()
        ));

        OwnerResponse response = ownerAuthService.getCurrentOwner("staff@example.com");

        assertEquals("Desk Staff", response.getOwnerName());
        assertEquals("u-staff", response.getUserId());
    }

    @Test
    void getCurrentOwner_MissingUser_ThrowsUnauthorized() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThrows(UnauthorizedException.class, () -> ownerAuthService.getCurrentOwner("missing@example.com"));
        verify(ownerAccessService, never()).requireBusiness(any());
    }
}
