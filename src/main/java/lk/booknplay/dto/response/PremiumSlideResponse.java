package lk.booknplay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumSlideResponse {
    private String id;
    private String businessId;
    private boolean enabled;
    private int sortOrder;
    private String headline;
    private String description;
    private String badge;
    private String imageUrl;
    private String primaryActionLabel;
    private String startsAt;
    private String expiresAt;
    private String name;
    private String logoUrl;
    private long venueCount;
    private List<String> cities;
    private List<String> sports;
}
