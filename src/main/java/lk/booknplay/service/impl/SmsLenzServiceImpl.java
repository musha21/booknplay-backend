package lk.booknplay.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.booknplay.config.SmsLenzProperties;
import lk.booknplay.exception.SmsDeliveryException;
import lk.booknplay.service.SmsLenzService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;

@Service
public class SmsLenzServiceImpl implements SmsLenzService {

    private static final Logger log = LoggerFactory.getLogger(SmsLenzServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final SmsLenzProperties properties;

    public SmsLenzServiceImpl(RestClient.Builder restClientBuilder, SmsLenzProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public void sendSms(String phone, String message) {
        requireCredentials();

        String baseUrl = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String url = baseUrl + "/send-sms";
        String senderId = properties.getSenderId().trim();
        String userId = properties.getUserId().trim();

        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("api_key", properties.getApiKey().trim());
        body.put("sender_id", senderId);
        body.put("contact", phone);
        body.put("message", message);

        try {
            ResponseEntity<String> entity = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            if (!entity.getStatusCode().is2xxSuccessful()) {
                log.error("SMSlenz HTTP {} for contact ending {} (senderId={}, userId={})",
                        entity.getStatusCode().value(), maskPhone(phone), senderId, userId);
                throw new SmsDeliveryException("Unable to send SMS. Please try again");
            }

            String responseBody = entity.getBody();
            if (responseBody != null && !responseBody.isBlank()) {
                JsonNode root = MAPPER.readTree(responseBody);
                if (root.has("success") && !root.get("success").asBoolean(false)) {
                    String providerMessage = root.path("message").asText("SMS send failed");
                    log.error("SMSlenz rejected SMS for contact ending {} (senderId={}, userId={}): {}",
                            maskPhone(phone), senderId, userId, providerMessage);
                    throw new SmsDeliveryException("Unable to send SMS. Please try again");
                }
            }

            log.info("SMSlenz SMS accepted for contact ending {} (senderId={}, userId={})",
                    maskPhone(phone), senderId, userId);
        } catch (SmsDeliveryException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.error("SMSlenz HTTP error {} for contact ending {} (senderId={}, userId={})",
                    ex.getStatusCode().value(), maskPhone(phone), senderId, userId);
            throw new SmsDeliveryException("Unable to send SMS. Please try again", ex);
        } catch (Exception ex) {
            log.error("SMSlenz send failed for contact ending {} (senderId={}, userId={}): {}",
                    maskPhone(phone), senderId, userId, ex.getMessage());
            throw new SmsDeliveryException("Unable to send SMS. Please try again", ex);
        }
    }

    private void requireCredentials() {
        if (isBlank(properties.getUserId()) || isBlank(properties.getApiKey()) || isBlank(properties.getSenderId())) {
            log.error("SMSlenz credentials are not configured (userIdSet={}, apiKeySet={}, senderId={})",
                    !isBlank(properties.getUserId()),
                    !isBlank(properties.getApiKey()),
                    isBlank(properties.getSenderId()) ? "(blank)" : properties.getSenderId().trim());
            throw new SmsDeliveryException("Unable to send SMS. Please try again");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(phone.length() - 4);
    }
}
