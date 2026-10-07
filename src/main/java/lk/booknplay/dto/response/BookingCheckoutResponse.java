package lk.booknplay.dto.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingCheckoutResponse {
    private BookingResponse booking;
    private PaymentResponse payment;
    private boolean invoiceAvailable;
    private String paymentMode;
}
