package lk.booknplay.util;

import lk.booknplay.exception.BadRequestException;

import java.util.regex.Pattern;

/**
 * Normalizes Sri Lankan mobile numbers to E.164 {@code +947XXXXXXXX}.
 */
public final class PhoneNumberUtil {

    private static final Pattern NORMALIZED = Pattern.compile("^\\+947[0-9]{8}$");

    private PhoneNumberUtil() {
    }

    /**
     * Normalize common Sri Lankan mobile inputs to {@code +947XXXXXXXX}.
     *
     * @throws BadRequestException if the number cannot be normalized to a valid SL mobile
     */
    public static String normalizeSriLanka(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Invalid phone number");
        }

        String digits = raw.trim()
                .replaceAll("[\\s\\-()]", "");

        if (digits.startsWith("00")) {
            digits = "+" + digits.substring(2);
        }

        String candidate;
        if (digits.startsWith("+")) {
            candidate = digits;
        } else if (digits.startsWith("94") && digits.length() == 11) {
            candidate = "+" + digits;
        } else if (digits.startsWith("0") && digits.length() == 10) {
            candidate = "+94" + digits.substring(1);
        } else if (digits.length() == 9 && digits.startsWith("7")) {
            candidate = "+94" + digits;
        } else {
            throw new BadRequestException("Invalid phone number");
        }

        if (!NORMALIZED.matcher(candidate).matches()) {
            throw new BadRequestException("Invalid phone number");
        }
        return candidate;
    }

    public static boolean isValidNormalized(String phone) {
        return phone != null && NORMALIZED.matcher(phone).matches();
    }
}
