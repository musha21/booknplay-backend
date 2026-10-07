package lk.booknplay.service.impl;

import lk.booknplay.config.SmsLenzProperties;
import lk.booknplay.exception.SmsDeliveryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsLenzServiceImplTest {

    @Mock private RestClient.Builder restClientBuilder;
    @Mock private RestClient restClient;
    @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private RestClient.RequestBodySpec requestBodySpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    private SmsLenzProperties properties;
    private SmsLenzServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new SmsLenzProperties();
        properties.setBaseUrl("https://smslenz.lk/api");
        properties.setUserId("2153");
        properties.setApiKey("test-key");
        properties.setSenderId("AVENQUE");

        when(restClientBuilder.build()).thenReturn(restClient);
        service = new SmsLenzServiceImpl(restClientBuilder, properties);
    }

    @Test
    void sendSms_providerFailure_throwsSmsDeliveryException() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(
                ResponseEntity.ok("{\"success\":false,\"message\":\"Insufficient credits\"}")
        );

        assertThrows(SmsDeliveryException.class,
                () -> service.sendSms("+94771234567", "Your BooknPlay verification code is 123456"));
    }

    @Test
    void sendSms_missingCredentials_throws() {
        properties.setApiKey("");
        assertThrows(SmsDeliveryException.class,
                () -> service.sendSms("+94771234567", "test"));
        verify(restClient, never()).post();
    }
}
