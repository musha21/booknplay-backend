package lk.booknplay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class HomepageConfigRequest {
    @Size(max = 80) private String eyebrow;
    @NotBlank @Size(max = 140) private String heading;
    @Size(max = 500) private String description;
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
    private boolean premiumSliderPauseOnHover = true;
    private String animationIntensity;
    private List<String> premiumBusinessIds;
    private List<PremiumSlideRequest> premiumSlides;
}
