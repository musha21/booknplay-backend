package lk.booknplay.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "smslenz")
@Getter
@Setter
public class SmsLenzProperties {

    private static final Logger log = LoggerFactory.getLogger(SmsLenzProperties.class);

    private String baseUrl = "https://smslenz.lk/api";
    private String userId = "";
    private String apiKey = "";
    private String senderId = "SMSlenzDEMO";

    @PostConstruct
    void logResolvedConfig() {
        boolean userSet = userId != null && !userId.isBlank();
        boolean keySet = apiKey != null && !apiKey.isBlank();
        String sender = senderId == null || senderId.isBlank() ? "(blank)" : senderId.trim();
        String base = baseUrl == null || baseUrl.isBlank() ? "(blank)" : baseUrl.trim();
        log.info("SMSlenz config loaded: baseUrl={}, userId={}, apiKeySet={}, senderId={}",
                base,
                userSet ? userId.trim() : "(blank)",
                keySet,
                sender);
    }
}
