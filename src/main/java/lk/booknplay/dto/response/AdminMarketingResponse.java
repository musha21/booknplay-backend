package lk.booknplay.dto.response;

import lk.booknplay.enums.MarketingChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMarketingResponse {

    private MarketingChannel channel;
    private int targeted;
    private int sent;
    private int skipped;
    private int failed;

    @Builder.Default
    private List<String> failureSamples = new ArrayList<>();
}
