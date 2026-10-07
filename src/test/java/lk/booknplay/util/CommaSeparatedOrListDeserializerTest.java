package lk.booknplay.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.booknplay.dto.request.AdminSubscriptionPlanUpdateRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommaSeparatedOrListDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void acceptsCommaStringFeatures() throws Exception {
        AdminSubscriptionPlanUpdateRequest request = mapper.readValue(
                "{\"features\":\"1 venue, Reports\",\"reason\":\"test\",\"commissionPercent\":0}",
                AdminSubscriptionPlanUpdateRequest.class);
        assertEquals("1 venue, Reports", request.getFeatures());
        assertEquals(0, request.getCommissionPercent().compareTo(BigDecimal.ZERO));
    }

    @Test
    void acceptsJsonArrayFeatures() throws Exception {
        AdminSubscriptionPlanUpdateRequest request = mapper.readValue(
                "{\"features\":[\"1 venue\",\"Reports\"],\"reason\":\"test\"}",
                AdminSubscriptionPlanUpdateRequest.class);
        assertEquals("1 venue,Reports", request.getFeatures());
    }

    @Test
    void acceptsNullFeatures() throws Exception {
        AdminSubscriptionPlanUpdateRequest request = mapper.readValue(
                "{\"features\":null,\"reason\":\"test\"}",
                AdminSubscriptionPlanUpdateRequest.class);
        assertNull(request.getFeatures());
    }
}
