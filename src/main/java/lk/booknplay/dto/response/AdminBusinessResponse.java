package lk.booknplay.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AdminBusinessResponse {
    private String id;
    private String name;
    private String ownerId;
    private String ownerEmail;
    private String ownerName;
    private String contactEmail;
    private String contactPhone;
    private BigDecimal commissionPercent;
    private boolean enabled;
    private boolean locked;
    private long venueCount;
    private String logoUrl;
    private List<String> imageUrls;
}
