package lk.booknplay.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "booknplay.otp")
@Getter
@Setter
public class OtpProperties {

    private int expirySeconds = 300;
    private int maxAttempts = 5;
    private int resendCooldownSeconds = 60;
    private long registrationTokenExpiryMs = 900_000L;
}
