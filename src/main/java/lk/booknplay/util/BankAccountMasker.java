package lk.booknplay.util;

public final class BankAccountMasker {

    private BankAccountMasker() {
    }

    public static String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return null;
        }
        String digits = accountNumber.trim();
        if (digits.length() <= 4) {
            return "****" + digits;
        }
        return "****" + digits.substring(digits.length() - 4);
    }
}
