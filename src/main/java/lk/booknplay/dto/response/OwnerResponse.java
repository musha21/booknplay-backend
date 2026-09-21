package lk.booknplay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerResponse {

    private String userId;
    private String email;
    private String businessId;
    private String businessName;
    private String ownerName;
    private String address;
    private String contactEmail;
    private String contactPhone;
    private String logoUrl;
    private String ownerProfileImageUrl;
    private List<String> imageUrls;
    private BigDecimal commissionPercent;
}
