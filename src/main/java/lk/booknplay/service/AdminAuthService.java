package lk.booknplay.service;

import lk.booknplay.dto.request.CustomerLoginRequest;
import lk.booknplay.dto.request.RefreshTokenRequest;
import lk.booknplay.dto.response.AdminResponse;
import lk.booknplay.dto.response.AuthResponse;

public interface AdminAuthService {
    AuthResponse login(CustomerLoginRequest request);
    AuthResponse refresh(RefreshTokenRequest request);
    void logout(String refreshToken);
    AdminResponse me(String email);
}
