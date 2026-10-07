package lk.booknplay.dto.request;

import jakarta.validation.constraints.NotEmpty;
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
public class MediaOrderRequest {
    @NotEmpty(message = "At least one media id is required")
    @Builder.Default
    private List<String> mediaIds = new ArrayList<>();
}
