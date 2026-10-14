package lk.booknplay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "platform_payout_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformPayoutConfig {

    public static final String SINGLETON_ID = "default";

    @Id
    @Column(length = 32)
    @Builder.Default
    private String id = SINGLETON_ID;

    @Builder.Default
    @Column(name = "gateway_fee_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal gatewayFeePercent = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
        if (id == null || id.isBlank()) {
            id = SINGLETON_ID;
        }
        if (gatewayFeePercent == null) {
            gatewayFeePercent = BigDecimal.ZERO;
        }
    }
}
