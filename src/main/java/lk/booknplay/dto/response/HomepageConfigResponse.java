package lk.booknplay.dto.response;

import lk.booknplay.enums.HomepageConfigStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HomepageConfigResponse {
    private String id;
    private int version;
    private HomepageConfigStatus status;
    private String eyebrow;
    private String heading;
    private String description;
    private boolean showSearch;
    private boolean showSports;
    private boolean showBusinesses;
    private boolean showVenues;
    private boolean showCities;
    private boolean showHowItWorks;
    private boolean showOwnerPromotion;
    private boolean showTrust;
    private List<String> sectionOrder;
    private List<String> featuredBusinessIds;
    private List<String> featuredVenueIds;
    private List<String> featuredSportIds;
    private boolean premiumSliderEnabled;
    private boolean premiumSliderAutoplay;
    private int premiumSliderSeconds;
    private boolean premiumSliderArrows;
    private boolean premiumSliderIndicators;
    private boolean premiumSliderPauseOnHover;
    private String animationIntensity;
    private List<String> premiumBusinessIds;
    private List<PremiumSlideResponse> premiumSlides;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
