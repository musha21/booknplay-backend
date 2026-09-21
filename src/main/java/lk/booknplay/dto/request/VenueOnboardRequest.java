package lk.booknplay.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lk.booknplay.dto.request.OperatingHoursUpdateRequest.DayHours;
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
public class VenueOnboardRequest {

    private String name;

    @NotBlank
    private String venueType;

    private String description;

    @NotBlank
    private String formattedAddress;

    private String city;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    @NotEmpty
    @Valid
    private List<FacilityOnboardRequest> facilities;

    @NotEmpty
    @Valid
    private List<DayHours> hours;

    @Builder.Default
    private List<String> amenities = new ArrayList<>();

    @Builder.Default
    private List<String> rulePresets = new ArrayList<>();

    private String additionalRules;
}
