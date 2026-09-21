package lk.booknplay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessResponse {
    private String id;
    private String name;
    private String address;
    private String logoUrl;
    private String imageUrl;
    private long venueCount;
    private List<String> cities;
    private List<String> sports;
}
