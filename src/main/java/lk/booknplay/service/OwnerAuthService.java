package lk.booknplay.service;

import lk.booknplay.dto.request.ChangePasswordRequest;
import lk.booknplay.dto.request.BusinessUpdateRequest;
import lk.booknplay.dto.request.OwnerRegisterRequest;
import lk.booknplay.dto.request.CustomerLoginRequest;
import lk.booknplay.dto.request.RefreshTokenRequest;
import lk.booknplay.dto.response.AuthResponse;
import lk.booknplay.dto.response.OwnerResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OwnerAuthService {
    AuthResponse register(OwnerRegisterRequest request);
    AuthResponse login(CustomerLoginRequest request);
    AuthResponse refresh(RefreshTokenRequest request);
    void logout(String refreshToken);
    OwnerResponse getCurrentOwner(String email);
    OwnerResponse updateBusiness(String email, BusinessUpdateRequest request);
    void changePassword(String email, ChangePasswordRequest request);
    OwnerResponse uploadBusinessImages(String email, MultipartFile logo, List<MultipartFile> images, MultipartFile profileImage);
}
