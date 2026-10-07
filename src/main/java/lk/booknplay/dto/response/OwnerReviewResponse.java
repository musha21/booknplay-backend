package lk.booknplay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerReviewResponse {
    private String id;
    private String venueId;
    private String venueName;
    private String customerName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
