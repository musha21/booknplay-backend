package lk.booknplay.dto.response;

import lk.booknplay.enums.BookingSource;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private String id;
    private String bookingRef;
    private String customerId;
    private String customerName;
    private String venueId;
    private String venueName;
    private String courtId;
    private String courtName;
    private String sportId;
    private String sportName;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal totalAmount;
    private String currency;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private BookingSource source;
    private String guestName;
    private String guestPhone;
    private LocalDateTime createdAt;
}
