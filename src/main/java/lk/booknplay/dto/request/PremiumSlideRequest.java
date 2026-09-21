package lk.booknplay.dto.request;

import lombok.Data;

@Data
public class PremiumSlideRequest {
    private String id;
    private String businessId;
    private boolean enabled = true;
    private int sortOrder;
    private String headline;
    private String description;
    private String badge;
    private String imageUrl;
    private String primaryActionLabel;
    private String startsAt;
    private String expiresAt;
}
