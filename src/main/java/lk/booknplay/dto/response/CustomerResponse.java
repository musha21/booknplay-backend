package lk.booknplay.dto.response;

import lk.booknplay.enums.CustomerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String profileImage;
    private CustomerStatus status;
    private LocalDateTime createdAt;
}
