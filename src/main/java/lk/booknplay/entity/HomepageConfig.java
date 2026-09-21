package lk.booknplay.entity;

import jakarta.persistence.*;
import lk.booknplay.enums.HomepageConfigStatus;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "homepage_configs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HomepageConfig {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false) private int version;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private HomepageConfigStatus status;
    private String eyebrow;
    @Column(nullable = false) private String heading;
    @Column(columnDefinition = "TEXT") private String description;
    @Builder.Default @Column(nullable = false) private boolean showSearch = true;
    @Builder.Default @Column(nullable = false) private boolean showSports = true;
    @Builder.Default @Column(nullable = false) private boolean showBusinesses = true;
    @Builder.Default @Column(nullable = false) private boolean showVenues = true;
    @Builder.Default @Column(nullable = false) private boolean showCities = true;
    @Builder.Default @Column(nullable = false) private boolean showHowItWorks = true;
    @Builder.Default @Column(nullable = false) private boolean showOwnerPromotion = true;
    @Builder.Default @Column(nullable = false) private boolean showTrust = true;
    @Column(name = "section_order", columnDefinition = "TEXT") private String sectionOrder;
    @Column(name = "featured_business_ids", columnDefinition = "TEXT") private String featuredBusinessIds;
    @Column(name = "featured_venue_ids", columnDefinition = "TEXT") private String featuredVenueIds;
    @Column(name = "featured_sport_ids", columnDefinition = "TEXT") private String featuredSportIds;
    @Builder.Default @Column(name = "premium_slider_enabled", nullable = false) private boolean premiumSliderEnabled = true;
    @Builder.Default @Column(name = "premium_slider_autoplay", nullable = false) private boolean premiumSliderAutoplay = true;
    @Builder.Default @Column(name = "premium_slider_seconds", nullable = false) private int premiumSliderSeconds = 6;
    @Builder.Default @Column(name = "premium_slider_arrows", nullable = false) private boolean premiumSliderArrows = true;
    @Builder.Default @Column(name = "premium_slider_indicators", nullable = false) private boolean premiumSliderIndicators = true;
    @Builder.Default @Column(name = "premium_slider_pause_on_hover", nullable = false) private boolean premiumSliderPauseOnHover = true;
    @Builder.Default @Column(name = "animation_intensity", nullable = false, length = 20) private String animationIntensity = "SUBTLE";
    @Column(name = "premium_business_ids", columnDefinition = "TEXT") private String premiumBusinessIds;
    @Column(name = "premium_slides", columnDefinition = "TEXT") private String premiumSlides;
    @Column(name = "updated_by") private String updatedBy;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @Column(name = "published_at") private LocalDateTime publishedAt;
    @PrePersist void create() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
}
