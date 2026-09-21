package lk.booknplay.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatingHoursUpdateRequest {

    @NotEmpty
    @Valid
    private List<DayHours> days;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayHours {
        private DayOfWeek dayOfWeek;
        private LocalTime openTime;
        private LocalTime closeTime;
        private boolean closed;
    }
}
