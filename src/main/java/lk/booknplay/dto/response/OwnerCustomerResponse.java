package lk.booknplay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerCustomerResponse {
    private String key;
    private String customerId;
    private String name;
    private String phone;
    private String email;
    private long visitCount;
    private BigDecimal totalSpend;
    private LocalDate lastBookingDate;
    private String lastBookingRef;
}
