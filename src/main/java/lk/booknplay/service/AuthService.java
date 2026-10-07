package lk.booknplay.service;

import lk.booknplay.dto.request.CustomerLoginRequest;
import lk.booknplay.dto.request.CustomerRegisterRequest;
import lk.booknplay.dto.request.RefreshTokenRequest;
import lk.booknplay.dto.response.AuthResponse;
import lk.booknplay.entity.User;

public interface AuthService {
    AuthResponse register(CustomerRegisterRequest request);
    AuthResponse login(CustomerLoginRequest request);
    AuthResponse refresh(RefreshTokenRequest request);
    void logout(String refreshToken);

    /** Issue access + refresh tokens for an already-authenticated customer user. */
    AuthResponse issueTokensForCustomer(User user);
}
