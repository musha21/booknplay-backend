package lk.booknplay.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BankAccountMaskerTest {

    @Test
    void masksLeavingLastFour() {
        assertEquals("****7890", BankAccountMasker.mask("1234567890"));
    }

    @Test
    void blanksBecomeNull() {
        assertNull(BankAccountMasker.mask(null));
        assertNull(BankAccountMasker.mask("  "));
    }
}
