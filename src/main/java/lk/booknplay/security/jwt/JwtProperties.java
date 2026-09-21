package lk.booknplay.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private long accessTokenExpiryMs = 900000; // 15 mins
    private long refreshTokenExpiryMs = 604800000; // 7 days
}
