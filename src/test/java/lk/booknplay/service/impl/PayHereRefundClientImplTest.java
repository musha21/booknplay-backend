package lk.booknplay.service.impl;

import lk.booknplay.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PayHereRefundClientImplTest {

    @Test
    void extractPayHereMessageReadsMsgField() {
        assertEquals(
                "Error processing refund",
                PayHereRefundClientImpl.extractPayHereMessage(
                        "{\"status\":-1,\"msg\":\"Error processing refund\",\"data\":null}",
                        "fallback"
                )
        );
    }

    @Test
    void requireNumericPaymentIdRejectsBookingRefs() {
        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> PayHereRefundClientImpl.requireNumericPaymentId("BNP-20261001-ABC123")
        );
        assertTrue(error.getMessage().toLowerCase().contains("invalid"));
    }

    @Test
    void requireNumericPaymentIdAcceptsDigits() {
        assertEquals("320027150501", PayHereRefundClientImpl.requireNumericPaymentId("320027150501"));
    }

    @Test
    void refundSurfacesPayHereErrorBodyOnHttpFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PayHereRefundClientImpl client = new PayHereRefundClientImpl(
                builder,
                "app-id",
                "app-secret",
                "https://sandbox.payhere.lk/merchant/v1/oauth/token",
                "https://sandbox.payhere.lk/merchant/v1/payment/refund"
        );

        server.expect(requestTo("https://sandbox.payhere.lk/merchant/v1/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"access_token\":\"tok-1\",\"token_type\":\"bearer\",\"expires_in\":600}",
                        MediaType.APPLICATION_JSON
                ));
        server.expect(requestTo("https://sandbox.payhere.lk/merchant/v1/payment/refund"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer tok-1"))
                .andRespond(withBadRequest().body(
                        "{\"status\":-1,\"msg\":\"Error processing refund\",\"data\":null}"
                ).contentType(MediaType.APPLICATION_JSON));

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> client.refund(
                        "320027150501",
                        new BigDecimal("3000.00"),
                        new BigDecimal("3000.00"),
                        "Booking cancel"
                )
        );

        assertTrue(error.getMessage().contains("Error processing refund"));
        assertTrue(error.getMessage().contains("not cancelled"));
        server.verify();
    }

    @Test
    void refundRejectsNonNumericPaymentIdWithoutCallingPayHere() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PayHereRefundClientImpl client = new PayHereRefundClientImpl(
                builder,
                "app-id",
                "app-secret",
                "https://sandbox.payhere.lk/merchant/v1/oauth/token",
                "https://sandbox.payhere.lk/merchant/v1/payment/refund"
        );

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> client.refund(
                        "BNP-ORDER-1",
                        new BigDecimal("1000.00"),
                        new BigDecimal("1000.00"),
                        "cancel"
                )
        );

        assertTrue(error.getMessage().toLowerCase().contains("invalid"));
        server.verify();
    }
}
