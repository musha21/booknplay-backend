package lk.booknplay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin create-business result. {@code temporaryPassword} is returned once for the admin to share
 * with the owner; it is never persisted in plaintext.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateBusinessResponse {
    private String id;
    private String name;
    private String ownerId;
    private String ownerEmail;
    private String ownerName;
    private String contactEmail;
    private String contactPhone;
    private BigDecimal commissionPercent;
    private boolean enabled;
    private boolean locked;
    private long venueCount;
    private String logoUrl;
    private List<String> imageUrls;
    private String planCode;
    private String subscriptionStatus;
    private LocalDateTime trialEndsAt;
    private SubscriptionResponse.PlanLimits limits;
    /** One-time plaintext password for the admin to share with the owner only. */
    private String temporaryPassword;

    public static AdminCreateBusinessResponse from(AdminBusinessResponse business, String temporaryPassword) {
        return AdminCreateBusinessResponse.builder()
                .id(business.getId())
                .name(business.getName())
                .ownerId(business.getOwnerId())
                .ownerEmail(business.getOwnerEmail())
                .ownerName(business.getOwnerName())
                .contactEmail(business.getContactEmail())
                .contactPhone(business.getContactPhone())
                .commissionPercent(business.getCommissionPercent())
                .enabled(business.isEnabled())
                .locked(business.isLocked())
                .venueCount(business.getVenueCount())
                .logoUrl(business.getLogoUrl())
                .imageUrls(business.getImageUrls())
                .planCode(business.getPlanCode())
                .subscriptionStatus(business.getSubscriptionStatus())
                .trialEndsAt(business.getTrialEndsAt())
                .limits(business.getLimits())
                .temporaryPassword(temporaryPassword)
                .build();
    }
}
