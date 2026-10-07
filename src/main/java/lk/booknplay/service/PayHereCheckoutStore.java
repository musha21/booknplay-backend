package lk.booknplay.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PayHereCheckoutStore {

    /**
     * @param bookingId   booking id for customer payments, or subscription payment id for SUB- orders
     * @param bookingRef  PayHere order_id (booking ref or SUB-{id})
     * @param returnUrl   optional override; null uses PaymentService booking return URL
     * @param cancelUrl   optional override; null uses PaymentService booking cancel URL
     */
    public record Session(
            String token,
            String bookingId,
            String bookingRef,
            BigDecimal amount,
            String currency,
            String firstName,
            String lastName,
            String email,
            String phone,
            String itemTitle,
            String hash,
            String returnUrl,
            String cancelUrl
    ) {
        /** Compatibility constructor for booking checkouts without custom return URLs. */
        public Session(
                String token,
                String bookingId,
                String bookingRef,
                BigDecimal amount,
                String currency,
                String firstName,
                String lastName,
                String email,
                String phone,
                String itemTitle,
                String hash) {
            this(token, bookingId, bookingRef, amount, currency, firstName, lastName, email, phone, itemTitle, hash, null, null);
        }
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public Session put(Session session) {
        sessions.put(session.token(), session);
        return session;
    }

    public Optional<Session> get(String token) {
        return Optional.ofNullable(sessions.get(token));
    }

    public void remove(String token) {
        sessions.remove(token);
    }

    public static String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
