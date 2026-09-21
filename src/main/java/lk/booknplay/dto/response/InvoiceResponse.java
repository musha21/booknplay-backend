package lk.booknplay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private String id;
    private String invoiceNumber;
    private String bookingId;
    private String bookingRef;
    private String customerName;
    private String customerEmail;
    private String venueName;
    private String courtName;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime issuedAt;
}
