package lk.booknplay.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PayHereHash {

    private PayHereHash() {
    }

    public static String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public static String checkoutHash(String merchantId, String orderId, BigDecimal amount, String currency, String merchantSecret) {
        String amountText = formatAmount(amount);
        return md5Upper(merchantId + orderId + amountText + currency + md5Upper(merchantSecret));
    }

    public static String notifySignature(
            String merchantId,
            String orderId,
            String payhereAmount,
            String payhereCurrency,
            String statusCode,
            String merchantSecret) {
        return md5Upper(merchantId + orderId + payhereAmount + payhereCurrency + statusCode + md5Upper(merchantSecret));
    }

    public static String md5Upper(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02X", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is not available", e);
        }
    }
}
