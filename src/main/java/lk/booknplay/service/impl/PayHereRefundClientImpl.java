package lk.booknplay.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.service.PayHereRefundClient;
import lk.booknplay.util.PayHereHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class PayHereRefundClientImpl implements PayHereRefundClient {

    private static final Logger log = LoggerFactory.getLogger(PayHereRefundClientImpl.class);
    private static final long TOKEN_SKEW_SECONDS = 30;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final String appId;
    private final String appSecret;
    private final String oauthTokenUrl;
    private final String refundUrl;

    private final Object tokenLock = new Object();
    private String cachedToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public PayHereRefundClientImpl(
            RestClient.Builder restClientBuilder,
            @Value("${payhere.app-id:}") String appId,
            @Value("${payhere.app-secret:}") String appSecret,
            @Value("${payhere.oauth-token-url:https://sandbox.payhere.lk/merchant/v1/oauth/token}") String oauthTokenUrl,
            @Value("${payhere.refund-url:https://sandbox.payhere.lk/merchant/v1/payment/refund}") String refundUrl) {
        this.restClient = restClientBuilder.build();
        this.appId = appId == null ? "" : appId.trim();
        this.appSecret = appSecret == null ? "" : appSecret.trim();
        this.oauthTokenUrl = oauthTokenUrl;
        this.refundUrl = refundUrl;
    }

    @Override
    public String refund(String paymentId, BigDecimal refundAmount, BigDecimal paidAmount, String description) {
        String normalizedPaymentId = requireNumericPaymentId(paymentId);
        if (refundAmount == null || refundAmount.signum() <= 0) {
            throw new BadRequestException("Refund amount must be greater than zero");
        }
        requireCredentials();

        String accessToken = accessToken();
        Map<String, Object> body = new HashMap<>();
        body.put("payment_id", normalizedPaymentId);
        body.put("description", description == null || description.isBlank()
                ? "Booking cancellation refund"
                : description.trim());
        boolean partial = paidAmount != null && refundAmount.compareTo(paidAmount) < 0;
        if (partial) {
            body.put("amount", PayHereHash.formatAmount(refundAmount));
        }

        try {
            ResponseEntity<String> entity = restClient.post()
                    .uri(refundUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            return parseRefundReference(parseJson(entity.getBody()));
        } catch (RestClientResponseException ex) {
            String payHereMsg = extractPayHereMessage(ex.getResponseBodyAsString(), null);
            log.warn(
                    "PayHere refund HTTP {} for payment_id={} msg={}",
                    ex.getStatusCode().value(),
                    normalizedPaymentId,
                    payHereMsg == null ? "(none)" : payHereMsg
            );
            throw new BadRequestException(cancelFailedMessage(
                    payHereMsg != null ? payHereMsg : "PayHere refund failed (HTTP " + ex.getStatusCode().value() + ")"
            ));
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("PayHere refund call failed for payment_id={}: {}", normalizedPaymentId, ex.getMessage());
            throw new BadRequestException(cancelFailedMessage("PayHere refund failed: " + safeExceptionMessage(ex)));
        }
    }

    static String requireNumericPaymentId(String paymentId) {
        if (paymentId == null || paymentId.isBlank()) {
            throw new BadRequestException(
                    "PayHere payment id is missing. The booking cannot be refunded until payment notify stored a payment_id."
            );
        }
        String trimmed = paymentId.trim();
        if (!trimmed.matches("\\d+")) {
            throw new BadRequestException(
                    "Stored PayHere payment id is invalid. Complete a sandbox payment that returns payment_id in notify, then cancel again."
            );
        }
        return trimmed;
    }

    static String extractPayHereMessage(String rawBody, String fallback) {
        if (rawBody == null || rawBody.isBlank()) {
            return fallback;
        }
        try {
            JsonNode node = MAPPER.readTree(rawBody);
            if (node.hasNonNull("msg") && !node.get("msg").asText().isBlank()) {
                return node.get("msg").asText().trim();
            }
            if (node.hasNonNull("message") && !node.get("message").asText().isBlank()) {
                return node.get("message").asText().trim();
            }
            if (node.hasNonNull("error_description") && !node.get("error_description").asText().isBlank()) {
                return node.get("error_description").asText().trim();
            }
            if (node.hasNonNull("error") && !node.get("error").asText().isBlank()) {
                return node.get("error").asText().trim();
            }
        } catch (Exception ignored) {
            // body was not JSON
        }
        String compact = rawBody.replaceAll("\\s+", " ").trim();
        if (compact.length() > 240) {
            compact = compact.substring(0, 240) + "…";
        }
        return compact.isEmpty() ? fallback : compact;
    }

    private String parseRefundReference(JsonNode response) {
        if (response == null || !response.has("status")) {
            throw new BadRequestException(cancelFailedMessage("PayHere refund returned an empty response"));
        }
        int status = response.path("status").asInt(-1);
        if (status != 1) {
            String msg = response.path("msg").asText("Refund was rejected by PayHere");
            log.warn("PayHere refund rejected status={} msg={}", status, msg);
            throw new BadRequestException(cancelFailedMessage(msg));
        }
        JsonNode data = response.get("data");
        if (data == null || data.isNull()) {
            return "PH-REFUND-" + Instant.now().toEpochMilli();
        }
        if (data.isNumber() || data.isTextual()) {
            return data.asText();
        }
        return data.toString();
    }

    private JsonNode parseJson(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(body);
        } catch (Exception ex) {
            throw new BadRequestException(cancelFailedMessage("PayHere refund returned a non-JSON response"));
        }
    }

    private static String cancelFailedMessage(String detail) {
        String text = detail == null || detail.isBlank() ? "PayHere refund failed" : detail.trim();
        if (!text.endsWith(".")) {
            text = text + ".";
        }
        return text + " The booking was not cancelled.";
    }

    private static String safeExceptionMessage(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return msg.length() > 180 ? msg.substring(0, 180) + "…" : msg;
    }

    private void requireCredentials() {
        if (appId.isBlank() || appSecret.isBlank()) {
            throw new BadRequestException(
                    "PayHere refund credentials are not configured. "
                            + "Set payhere.app-id and payhere.app-secret (API Keys in PayHere sandbox), then restart the API."
            );
        }
    }

    private String accessToken() {
        synchronized (tokenLock) {
            if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
                return cachedToken;
            }
            requireCredentials();
            String basic = Base64.getEncoder()
                    .encodeToString((appId + ":" + appSecret).getBytes(StandardCharsets.UTF_8));

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");

            try {
                ResponseEntity<String> entity = restClient.post()
                        .uri(oauthTokenUrl)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Basic " + basic)
                        .body(form)
                        .retrieve()
                        .toEntity(String.class);

                JsonNode tokenResponse = parseJson(entity.getBody());
                if (tokenResponse == null || !tokenResponse.hasNonNull("access_token")) {
                    throw new BadRequestException("PayHere OAuth did not return an access token. Check app-id and app-secret.");
                }
                cachedToken = tokenResponse.get("access_token").asText();
                int expiresIn = tokenResponse.path("expires_in").asInt(500);
                tokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn) - TOKEN_SKEW_SECONDS);
                return cachedToken;
            } catch (BadRequestException ex) {
                throw ex;
            } catch (RestClientResponseException ex) {
                String payHereMsg = extractPayHereMessage(ex.getResponseBodyAsString(), null);
                log.warn("PayHere OAuth HTTP {} msg={}", ex.getStatusCode().value(), payHereMsg == null ? "(none)" : payHereMsg);
                throw new BadRequestException(
                        payHereMsg != null
                                ? ("PayHere OAuth failed: " + payHereMsg)
                                : "PayHere OAuth failed. Check payhere.app-id and payhere.app-secret, then try again."
                );
            } catch (Exception ex) {
                log.warn("PayHere OAuth failed: {}", ex.getMessage());
                throw new BadRequestException(
                        "PayHere OAuth failed. Check payhere.app-id and payhere.app-secret, then try again."
                );
            }
        }
    }
}
