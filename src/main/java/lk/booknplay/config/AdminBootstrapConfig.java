package lk.booknplay.config;

import lk.booknplay.entity.User;
import lk.booknplay.enums.Role;
import lk.booknplay.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapConfig implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    @Value("${booknplay.admin.email:}") private String email;
    @Value("${booknplay.admin.password:}") private String password;

    public AdminBootstrapConfig(UserRepository users, PasswordEncoder encoder) { this.users = users; this.encoder = encoder; }

    @Override public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) return;
        users.findByEmail(email.trim().toLowerCase()).orElseGet(() -> users.save(User.builder()
                .email(email.trim().toLowerCase()).password(encoder.encode(password)).role(Role.SUPER_ADMIN)
                .isEnabled(true).isLocked(false).build()));
    }
}
