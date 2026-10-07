package lk.booknplay.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class PayHereConfigGuard {

    private static final Logger log =
            LoggerFactory.getLogger(PayHereConfigGuard.class);

    private final String paymentsMode;
    private final String merchantId;
    private final String merchantSecret;
    private final String checkoutUrl;
    private final String appBaseUrl;
    private final String notifyUrl;

    public PayHereConfigGuard(
            @Value("${booknplay.payments.mode:PAYHERE}") String paymentsMode,
            @Value("${payhere.merchant-id:}") String merchantId,
            @Value("${payhere.merchant-secret:}") String merchantSecret,
            @Value("${payhere.checkout-url:https://sandbox.payhere.lk/pay/checkout}") String checkoutUrl,
            @Value("${app.base-url:http://localhost:8080}") String appBaseUrl,
            @Value("${payhere.notify-url:}") String notifyUrl) {

        this.paymentsMode = paymentsMode;
        this.merchantId = merchantId == null ? "" : merchantId.trim();
        this.merchantSecret =
                merchantSecret == null ? "" : merchantSecret.trim();
        this.checkoutUrl = checkoutUrl;
        this.appBaseUrl =
                appBaseUrl == null ? "" : appBaseUrl.trim();
        this.notifyUrl =
                notifyUrl == null ? "" : notifyUrl.trim();
    }

    @PostConstruct
    void logPayHereConfig() {

        boolean payHere =
                "PAYHERE".equalsIgnoreCase(
                        String.valueOf(paymentsMode).trim()
                );

        if (!payHere) {
            return;
        }

        boolean secretConfigured = !merchantSecret.isBlank();

        String baseHost = hostOf(appBaseUrl);
        String notifyHost = hostOf(notifyUrl);

        log.info(
                "PayHere config: mode={}, merchantId={}, appBaseUrl={}, " +
                        "notifyUrl={}, checkoutUrl={}, secretConfigured={}",
                paymentsMode,
                merchantId.isBlank() ? "(missing)" : merchantId,
                appBaseUrl,
                notifyUrl.isBlank() ? "(missing)" : notifyUrl,
                checkoutUrl,
                secretConfigured
        );

        /*
         * Check required PayHere credentials.
         */
        if (merchantId.isBlank() || !secretConfigured) {

            log.error(
                    "PayHere credentials are incomplete. " +
                            "merchantIdConfigured={}, merchantSecretConfigured={}",
                    !merchantId.isBlank(),
                    secretConfigured
            );

            return;
        }

        /*
         * Validate notify URL.
         */
        if (notifyUrl.isBlank()) {

            log.error(
                    "PayHere notify URL is missing. " +
                            "PayHere will not be able to notify BooknPlay " +
                            "when a payment succeeds."
            );

            return;
        }

        /*
         * Local development:
         *
         * app.base-url can be localhost while notify-url uses ngrok.
         * PayHere cannot call localhost from its servers,
         * therefore ngrok/public HTTPS is required for the webhook.
         */
        if (isLocalHost(baseHost)) {

            if (isLocalHost(notifyHost)) {

                log.error(
                        "PayHere local configuration problem: " +
                                "payhere.notify-url points to '{}'. " +
                                "PayHere cannot call localhost. " +
                                "Use a public HTTPS URL such as ngrok.",
                        notifyHost
                );

            } else {

                log.info(
                        "PayHere local development configuration detected. " +
                                "app.base-url host='{}', public notify host='{}'. " +
                                "This is expected when using ngrok.",
                        baseHost,
                        notifyHost
                );
            }

            log.warn(
                    "PayHere merchant secret must match the PayHere " +
                            "Integration configuration used for the checkout domain."
            );

            return;
        }

        /*
         * Production:
         *
         * Normally app.base-url and notify-url should use
         * the same production domain.
         */
        if (!baseHost.isBlank()
                && !notifyHost.isBlank()
                && !baseHost.equalsIgnoreCase(notifyHost)) {

            log.warn(
                    "PayHere production host mismatch: " +
                            "app.base-url host='{}', notify host='{}'. " +
                            "Verify that this is intentional.",
                    baseHost,
                    notifyHost
            );
        }

        log.info(
                "PayHere configuration check completed. baseHost={}, notifyHost={}",
                baseHost,
                notifyHost
        );
    }

    private static boolean isLocalHost(String host) {

        if (host == null) {
            return false;
        }

        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host);
    }

    private static String hostOf(String url) {

        if (url == null || url.isBlank()) {
            return "";
        }

        try {

            URI uri = URI.create(url);

            return uri.getHost() == null
                    ? ""
                    : uri.getHost();

        } catch (IllegalArgumentException ex) {

            return "";
        }
    }
}