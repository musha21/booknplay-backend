package lk.booknplay.util;

import lk.booknplay.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNumberUtilTest {

    @Test
    void normalize_localLeadingZero() {
        assertEquals("+94771234567", PhoneNumberUtil.normalizeSriLanka("0771234567"));
    }

    @Test
    void normalize_countryCodeWithoutPlus() {
        assertEquals("+94771234567", PhoneNumberUtil.normalizeSriLanka("94771234567"));
    }

    @Test
    void normalize_alreadyE164() {
        assertEquals("+94771234567", PhoneNumberUtil.normalizeSriLanka("+94771234567"));
    }

    @Test
    void normalize_withSpacesAndDashes() {
        assertEquals("+94771234567", PhoneNumberUtil.normalizeSriLanka("077-123 4567"));
    }

    @Test
    void normalize_rejectsInvalid() {
        assertThrows(BadRequestException.class, () -> PhoneNumberUtil.normalizeSriLanka("12345"));
        assertThrows(BadRequestException.class, () -> PhoneNumberUtil.normalizeSriLanka("+94112345678"));
        assertThrows(BadRequestException.class, () -> PhoneNumberUtil.normalizeSriLanka(""));
        assertThrows(BadRequestException.class, () -> PhoneNumberUtil.normalizeSriLanka(null));
    }
}
