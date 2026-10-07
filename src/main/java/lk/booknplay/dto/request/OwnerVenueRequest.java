package lk.booknplay.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class OwnerVenueRequest {

    private String name;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    private String formattedAddress;
    private String venueType;

    private Double latitude;
    private Double longitude;
    private String description;
    private String coverImageUrl;

    @Builder.Default
    private List<String> rules = new ArrayList<>();

    private String additionalRules;

    @Builder.Default
    private List<String> amenities = new ArrayList<>();

    /** When null, existing venue photos are left unchanged. */
    private List<String> imageUrls;
}
