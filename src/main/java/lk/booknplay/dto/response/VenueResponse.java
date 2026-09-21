package lk.booknplay.dto.response;

import lk.booknplay.enums.VenueStatus;
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
public class VenueResponse {

    private String id;
    private String businessId;
    private String businessName;
    private String name;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private String description;
    private VenueStatus status;
    private String venueType;
    private String sportName;
    private String formattedAddress;
    private String coverImageUrl;
    private String businessImageUrl;
    private BigDecimal startingPrice;
    private String currency;
    private Double rating;
    private List<String> amenities;
    private List<String> rules;
    private String additionalRules;
    private List<String> images;
    private List<CourtResponse> courts;
    private int setupPercent;
}
