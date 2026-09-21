package lk.booknplay.service.impl;

import lk.booknplay.dto.request.BusinessUpdateRequest;
import lk.booknplay.dto.request.ChangePasswordRequest;
import lk.booknplay.dto.request.CustomerLoginRequest;
import lk.booknplay.dto.request.OwnerRegisterRequest;
import lk.booknplay.dto.request.RefreshTokenRequest;
import lk.booknplay.dto.response.AuthResponse;
import lk.booknplay.dto.response.OwnerResponse;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.BusinessImage;
import lk.booknplay.entity.RefreshToken;
import lk.booknplay.entity.User;
import lk.booknplay.enums.Role;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.exception.ConflictException;
import lk.booknplay.exception.UnauthorizedException;
import lk.booknplay.repository.BusinessImageRepository;
import lk.booknplay.repository.BusinessRepository;
import lk.booknplay.repository.RefreshTokenRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.security.jwt.JwtProperties;
import lk.booknplay.security.jwt.JwtTokenProvider;
import lk.booknplay.service.FileStorageService;
import lk.booknplay.service.OwnerAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OwnerAuthServiceImpl implements OwnerAuthService {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final BusinessImageRepository businessImageRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public AuthResponse register(OwnerRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("EMAIL_EXISTS", "Email already in use");
        }

        User user = userRepository.save(User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.BUSINESS_OWNER)
                .isEnabled(true)
                .isLocked(false)
                .build());

        Business business = businessRepository.save(Business.builder()
                .name(request.getBusinessName())
                .ownerId(user.getId())
                .ownerName(request.getOwnerName())
                .address(request.getAddress())
                .contactEmail(request.getContactEmail() != null ? request.getContactEmail() : request.getEmail())
                .contactPhone(request.getContactPhone())
                .commissionPercent(new BigDecimal("10.00"))
                .build());

        String accessToken = tokenProvider.generateAccessToken(user, null, business.getId());
        String refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresInMs(jwtProperties.getAccessTokenExpiryMs())
                .role(user.getRole())
                .owner(toOwnerResponse(user, business))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(CustomerLoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (user.getRole() != Role.BUSINESS_OWNER && user.getRole() != Role.STAFF) {
            throw new UnauthorizedException("Not a venue owner account");
        }

        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new UnauthorizedException("Business record not found"));

        String accessToken = tokenProvider.generateAccessToken(user, null, business.getId());
        String refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresInMs(jwtProperties.getAccessTokenExpiryMs())
                .role(user.getRole())
                .owner(toOwnerResponse(user, business))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (token.isRevoked() || token.getExpiryDate().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }

        User user = token.getUser();
        if (user.getRole() != Role.BUSINESS_OWNER && user.getRole() != Role.STAFF) {
            throw new UnauthorizedException("Not a venue owner account");
        }

        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new UnauthorizedException("Business record not found"));

        String newAccessToken = tokenProvider.generateAccessToken(user, null, business.getId());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(token.getToken())
                .expiresInMs(jwtProperties.getAccessTokenExpiryMs())
                .role(user.getRole())
                .owner(toOwnerResponse(user, business))
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenRepository.findByToken(refreshTokenStr).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerResponse getCurrentOwner(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Owner not found"));
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new UnauthorizedException("Business record not found"));
        return toOwnerResponse(user, business);
    }

    @Override
    @Transactional
    public OwnerResponse updateBusiness(String email, BusinessUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Owner not found"));
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new UnauthorizedException("Business record not found"));
        if (request.getBusinessName() != null && !request.getBusinessName().isBlank()) {
            business.setName(request.getBusinessName());
        }
        if (request.getOwnerName() != null) {
            business.setOwnerName(request.getOwnerName());
        }
        if (request.getContactEmail() != null) {
            business.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            business.setContactPhone(request.getContactPhone());
        }
        if (request.getAddress() != null) {
            business.setAddress(request.getAddress());
        }
        return toOwnerResponse(user, businessRepository.save(business));
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Owner not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public OwnerResponse uploadBusinessImages(String email, MultipartFile logo, List<MultipartFile> images, MultipartFile profileImage) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Owner not found"));
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new UnauthorizedException("Business record not found"));
        String folder = "business/" + business.getId();
        if (profileImage != null && !profileImage.isEmpty()) {
            business.setOwnerProfileImageUrl(fileStorageService.store(profileImage, folder));
        }
        if (logo != null && !logo.isEmpty()) {
            business.setLogoUrl(fileStorageService.store(logo, folder));
        }
        if (images != null) {
            long existing = businessImageRepository.countByBusinessId(business.getId());
            int added = 0;
            for (MultipartFile image : images) {
                if (image == null || image.isEmpty()) {
                    continue;
                }
                if (existing + added >= 3) {
                    throw new BadRequestException("Maximum 3 gallery images plus 1 logo");
                }
                businessImageRepository.save(BusinessImage.builder()
                        .business(business)
                        .url(fileStorageService.store(image, folder))
                        .logo(false)
                        .sortOrder((int) existing + added)
                        .build());
                added++;
            }
        }
        return toOwnerResponse(user, businessRepository.save(business));
    }

    private OwnerResponse toOwnerResponse(User user, Business business) {
        List<String> gallery = businessImageRepository.findByBusinessIdOrderBySortOrderAsc(business.getId()).stream()
                .map(BusinessImage::getUrl)
                .toList();
        return OwnerResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .businessId(business.getId())
                .businessName(business.getName())
                .ownerName(business.getOwnerName())
                .address(business.getAddress())
                .contactEmail(business.getContactEmail())
                .contactPhone(business.getContactPhone())
                .logoUrl(business.getLogoUrl())
                .ownerProfileImageUrl(business.getOwnerProfileImageUrl())
                .imageUrls(gallery)
                .commissionPercent(business.getCommissionPercent())
                .build();
    }

    private String createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiryMs()))
                .isRevoked(false)
                .build();
        return refreshTokenRepository.save(token).getToken();
    }
}
