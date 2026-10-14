package lk.booknplay.entity;

import jakarta.persistence.*;
import lk.booknplay.enums.BankVerificationStatus;
import lk.booknplay.enums.PayoutFrequency;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "owner_name")
    private String ownerName;

    private String address;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "owner_profile_image_url")
    private String ownerProfileImageUrl;

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("sortOrder ASC")
    private java.util.List<BusinessImage> images = new java.util.ArrayList<>();

    @Builder.Default
    @Column(name = "commission_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionPercent = new BigDecimal("10.00");

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_account_name")
    private String bankAccountName;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_branch")
    private String bankBranch;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payout_frequency", nullable = false, length = 20)
    private PayoutFrequency payoutFrequency = PayoutFrequency.WEEKLY;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "bank_verification_status", nullable = false, length = 20)
    private BankVerificationStatus bankVerificationStatus = BankVerificationStatus.UNVERIFIED;

    @Column(name = "bank_verified_at")
    private LocalDateTime bankVerifiedAt;

    @Column(name = "bank_verified_by")
    private String bankVerifiedBy;

    @Builder.Default
    @Column(name = "notify_booking_email", nullable = false)
    private boolean notifyBookingEmail = true;

    @Builder.Default
    @Column(name = "notify_booking_sms", nullable = false)
    private boolean notifyBookingSms = false;

    @Builder.Default
    @Column(name = "notify_payment_email", nullable = false)
    private boolean notifyPaymentEmail = true;

    @Builder.Default
    @Column(name = "notify_trial_email", nullable = false)
    private boolean notifyTrialEmail = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (payoutFrequency == null) {
            payoutFrequency = PayoutFrequency.WEEKLY;
        }
        if (bankVerificationStatus == null) {
            bankVerificationStatus = BankVerificationStatus.UNVERIFIED;
        }
    }
}
