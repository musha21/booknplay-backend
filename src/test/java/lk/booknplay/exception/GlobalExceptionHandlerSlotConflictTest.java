package lk.booknplay.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerSlotConflictTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsLegacyUkCourtSlotToConflict() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement",
                new SQLIntegrityConstraintViolationException(
                        "Duplicate entry 'court-1-2026-10-01-06:00:00-07:00:0' for key 'bookings.uk_court_slot'"
                )
        );

        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrity(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("COURT_ALREADY_BOOKED", response.getBody().getCode());
        assertTrue(response.getBody().getMessage().toLowerCase().contains("already booked"));
    }

    @Test
    void mapsActiveCourtSlotUniqueToConflict() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "constraint [bookings.uk_active_court_slot]",
                new SQLIntegrityConstraintViolationException(
                        "Duplicate entry 'court|2026-10-01|06:00:00|07:00:00' for key 'uk_active_court_slot'"
                )
        );

        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrity(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("COURT_ALREADY_BOOKED", response.getBody().getCode());
    }

    @Test
    void leavesOtherIntegrityErrorsAsBadRequest() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "other",
                new SQLIntegrityConstraintViolationException("Duplicate entry for key 'uk_customer_idempotency'")
        );

        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrity(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("DATA_INTEGRITY", response.getBody().getCode());
    }
}
