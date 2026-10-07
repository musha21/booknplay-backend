package lk.booknplay.controller.publicapi;

import lk.booknplay.service.PayHereCheckoutStore;
import lk.booknplay.service.PaymentService;
import lk.booknplay.util.PayHereHash;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/public/payhere")
@RequiredArgsConstructor
public class PayHereCheckoutController {

    private final PaymentService paymentService;

    @GetMapping(value = "/checkout/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public String checkout(@PathVariable String token) {
        PayHereCheckoutStore.Session session = paymentService.requireCheckoutSession(token);
        String amount = PayHereHash.formatAmount(session.amount());
        String merchantId = paymentService.payHereMerchantId();
        String orderId = session.bookingRef();
        String currency = session.currency();
        String hash = session.hash();

        org.slf4j.LoggerFactory.getLogger(PayHereCheckoutController.class).info(
                "PAYHERE HASH INPUT / PAYLOAD: merchantId={}, orderId={}, amount={}, currency={}, hashLength={}, "
                        + "returnUrl={}, cancelUrl={}, notifyUrl={}, checkoutAction={}",
                merchantId,
                orderId,
                amount,
                currency,
                hash == null ? 0 : hash.length(),
                resolveReturnUrl(session),
                resolveCancelUrl(session),
                paymentService.payHereNotifyUrl(),
                paymentService.payHereCheckoutUrl()
        );

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant_id", merchantId);
        fields.put("return_url", resolveReturnUrl(session));
        fields.put("cancel_url", resolveCancelUrl(session));
        fields.put("notify_url", paymentService.payHereNotifyUrl());
        fields.put("order_id", orderId);
        fields.put("items", session.itemTitle());
        fields.put("currency", currency);
        fields.put("amount", amount);
        fields.put("first_name", session.firstName());
        fields.put("last_name", session.lastName());
        fields.put("email", session.email());
        fields.put("phone", session.phone());
        fields.put("address", "Sri Lanka");
        fields.put("city", "Kandy");
        fields.put("country", "Sri Lanka");
        fields.put("hash", hash);

        String inputs = fields.entrySet().stream()
                .map(entry -> "<input type=\"hidden\" name=\"" + escape(entry.getKey())
                        + "\" value=\"" + escape(entry.getValue()) + "\"/>")
                .collect(Collectors.joining());

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="utf-8"/><title>Redirecting to PayHere</title></head>
                <body>
                <p>Redirecting to PayHere sandbox checkout…</p>
                <form id="payhere" method="post" action="%s">%s</form>
                <script>document.getElementById('payhere').submit();</script>
                </body>
                </html>
                """.formatted(escape(paymentService.payHereCheckoutUrl()), inputs);
    }

    private String resolveReturnUrl(PayHereCheckoutStore.Session session) {
        if (session.returnUrl() != null && !session.returnUrl().isBlank()) {
            return session.returnUrl();
        }
        return paymentService.buildReturnUrl(session.bookingId());
    }

    private String resolveCancelUrl(PayHereCheckoutStore.Session session) {
        if (session.cancelUrl() != null && !session.cancelUrl().isBlank()) {
            return session.cancelUrl();
        }
        return paymentService.buildCancelUrl(session.bookingId());
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
