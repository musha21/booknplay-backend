package lk.booknplay.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayHereHashTest {

    @Test
    void checkoutHash_isUppercaseMd5AndStable() {
        String hash = PayHereHash.checkoutHash(
                "121XXXX",
                "ItemNo12345",
                new BigDecimal("1000.00"),
                "LKR",
                "XXXXXX"
        );
        assertEquals(32, hash.length());
        assertEquals(hash, hash.toUpperCase());
        assertTrue(hash.matches("[0-9A-F]{32}"));
        assertEquals(
                PayHereHash.checkoutHash("121XXXX", "ItemNo12345", new BigDecimal("1000"), "LKR", "XXXXXX"),
                hash
        );
    }

    @Test
    void formatAmount_alwaysTwoDecimals() {
        assertEquals("1500.00", PayHereHash.formatAmount(new BigDecimal("1500")));
        assertEquals("1500.50", PayHereHash.formatAmount(new BigDecimal("1500.5")));
    }

    @Test
    void checkoutHash_changesWhenSecretChanges() {
        String a = PayHereHash.checkoutHash("1238375", "BNP-TEST", new BigDecimal("1500.00"), "LKR", "secret-a");
        String b = PayHereHash.checkoutHash("1238375", "BNP-TEST", new BigDecimal("1500.00"), "LKR", "secret-b");
        assertFalse(a.equalsIgnoreCase(b));
    }
}
