package lk.booknplay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lk.booknplay.enums.MarketingChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMarketingRequest {

    @NotNull(message = "channel is required")
    private MarketingChannel channel;

    /** Required when channel is EMAIL or BOTH. */
    private String subject;

    @NotBlank(message = "message is required")
    private String message;

    /** Empty/null = all ACTIVE customers. */
    private List<String> customerIds;
}
