package lk.booknplay.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreateRequest {

    @NotBlank(message = "Court ID is required")
    private String courtId;

    @NotBlank(message = "Sport ID is required")
    private String sportId;

    @NotNull(message = "Booking date is required")
    @FutureOrPresent(message = "Booking date must be today or in the future")
    private LocalDate date;

    /** Legacy continuous range start — used when {@code slots} is empty. */
    private LocalTime startTime;

    /** Legacy continuous range end — used when {@code slots} is empty. */
    private LocalTime endTime;

    /**
     * Discrete 60-minute slots. Gaps are allowed. When non-empty, overrides
     * {@code startTime}/{@code endTime} for pricing and reservation.
     */
    @Valid
    @Builder.Default
    private List<SlotSelectionRequest> slots = new ArrayList<>();

    /** Optional owner promotion code. */
    private String promoCode;
}
