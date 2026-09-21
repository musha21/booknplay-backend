package lk.booknplay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.booknplay.dto.request.HomepageConfigRequest;
import lk.booknplay.dto.request.PremiumSlideRequest;
import lk.booknplay.entity.Business;
import lk.booknplay.entity.HomepageConfig;
import lk.booknplay.entity.User;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.HomepageConfigStatus;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.repository.*;
import lk.booknplay.service.impl.HomepageConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomepageConfigServiceImplTest {

    @Mock private HomepageConfigRepository configs;
    @Mock private BusinessRepository businesses;
    @Mock private VenueRepository venues;
    @Mock private SportRepository sports;
    @Mock private UserRepository users;
    @Mock private CourtRepository courts;
    @Mock private AdminAuditLogRepository audit;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private HomepageConfigServiceImpl service;

    private HomepageConfigRequest request;
    private Business business;
    private User owner;

    @BeforeEach
    void setUp() {
        owner = User.builder().id("owner-1").email("owner@example.com").isEnabled(true).isLocked(false).build();
        business = Business.builder().id("biz-1").name("Arena Co").ownerId("owner-1").logoUrl("/logo.png").build();
        request = new HomepageConfigRequest();
        request.setHeading("Find a court");
        request.setSectionOrder(List.of("sports", "businesses", "venues", "cities", "howItWorks", "ownerPromotion", "trust"));
        request.setPremiumSliderSeconds(6);
        request.setPremiumSliderEnabled(true);
        PremiumSlideRequest slide = new PremiumSlideRequest();
        slide.setBusinessId("biz-1");
        slide.setEnabled(true);
        request.setPremiumSlides(List.of(slide));
    }

    @Test
    void saveDraft_rejectsBusinessWithoutLiveVenue() {
        when(businesses.findById("biz-1")).thenReturn(Optional.of(business));
        when(users.findById("owner-1")).thenReturn(Optional.of(owner));
        when(venues.findByBusinessId("biz-1")).thenReturn(List.of());
        assertThrows(BadRequestException.class, () -> service.saveDraft(request, "admin@booknplay.lk"));
    }

    @Test
    void publicConfig_omitsExpiredSlides() {
        when(configs.findFirstByStatusOrderByVersionDesc(HomepageConfigStatus.PUBLISHED)).thenReturn(Optional.of(
                HomepageConfig.builder().id("pub").version(1).status(HomepageConfigStatus.PUBLISHED)
                        .heading("Find a court").premiumSliderEnabled(true).premiumSliderSeconds(6)
                        .premiumSlides("[{\"id\":\"s1\",\"businessId\":\"biz-1\",\"enabled\":true,\"expiresAt\":\"2020-01-01\"}]")
                        .build()));
        when(businesses.findById("biz-1")).thenReturn(Optional.of(business));
        when(users.findById("owner-1")).thenReturn(Optional.of(owner));
        when(venues.findByBusinessId("biz-1")).thenReturn(List.of(Venue.builder().id("v1").status(VenueStatus.ACTIVE).city("Kandy").build()));
        assertTrue(service.publicConfig().getPremiumSlides().isEmpty());
    }
}
