package lk.booknplay.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingConfirmRequest {
    @NotBlank
    private String quoteId;
    @Valid @NotNull
    private BookingContactRequest contact;
}
