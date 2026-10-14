package lk.booknplay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lk.booknplay.enums.FacilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerCourtRequest {

    @NotBlank
    private String name;

    private String sportId;

    private String sportName;

    @NotNull
    private FacilityType facilityType;

    @NotNull
    private BigDecimal hourlyRate;
}
