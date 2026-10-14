package lk.booknplay.repository;

import lk.booknplay.entity.Business;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.Sport;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.CourtStatus;
import lk.booknplay.enums.FacilityType;
import lk.booknplay.enums.VenueStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the court-level sport + facility filter matches a SINGLE active court
 * (Feature 5), so a venue is only returned when one court satisfies BOTH criteria.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class VenueRepositoryFacilityTest {

    @Autowired private VenueRepository venueRepository;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private SportRepository sportRepository;
    @Autowired private CourtRepository courtRepository;

    private Sport cricket;
    private Sport carrom;
    private final List<VenueStatus> statuses = List.of(VenueStatus.ACTIVE, VenueStatus.APPROVED);
    private final Pageable pageable = PageRequest.of(0, 50);

    @BeforeEach
    void setUp() {
        Business business = businessRepository.save(Business.builder().name("Arena Co").ownerId("owner-1").build());
        cricket = sportRepository.save(Sport.builder().name("Cricket").isActive(true).build());
        carrom = sportRepository.save(Sport.builder().name("Carrom").isActive(true).build());

        // Kandy Sports Hub: Cricket indoor AND cricket outdoor.
        Venue hub = venueRepository.save(activeVenue(business, "Kandy Sports Hub"));
        courtRepository.save(court(hub, cricket, FacilityType.INDOOR));
        courtRepository.save(court(hub, cricket, FacilityType.OUTDOOR));

        // Carrom House: only an indoor Carrom court.
        Venue carromHouse = venueRepository.save(activeVenue(business, "Carrom House"));
        courtRepository.save(court(carromHouse, carrom, FacilityType.INDOOR));

        // Mixed Club: Cricket outdoor + Carrom indoor (never cricket AND indoor together).
        Venue mixed = venueRepository.save(activeVenue(business, "Mixed Club"));
        courtRepository.save(court(mixed, cricket, FacilityType.OUTDOOR));
        courtRepository.save(court(mixed, carrom, FacilityType.INDOOR));
    }

    @Test
    void cricketIndoorMatchesOnlyVenuesWithACricketIndoorCourt() {
        List<String> names = search(cricket.getId(), FacilityType.INDOOR);
        assertTrue(names.contains("Kandy Sports Hub"));
        assertFalse(names.contains("Carrom House"), "Carrom-only venue must be excluded");
        assertFalse(names.contains("Mixed Club"), "Cross-court match must be rejected");
    }

    @Test
    void cricketOutdoorMatchesHubAndMixed() {
        List<String> names = search(cricket.getId(), FacilityType.OUTDOOR);
        assertTrue(names.contains("Kandy Sports Hub"));
        assertTrue(names.contains("Mixed Club"));
        assertFalse(names.contains("Carrom House"));
    }

    @Test
    void carromOutdoorMatchesNothing() {
        List<String> names = search(carrom.getId(), FacilityType.OUTDOOR);
        assertTrue(names.isEmpty());
    }

    @Test
    void facilityOnlyFilterMatchesAnyVenueWithThatFacility() {
        List<String> indoor = search(null, FacilityType.INDOOR);
        assertTrue(indoor.contains("Kandy Sports Hub"));
        assertTrue(indoor.contains("Carrom House"));
        assertTrue(indoor.contains("Mixed Club"));
    }

    @Test
    void noFiltersReturnsEveryActiveVenue() {
        List<String> all = search(null, null);
        assertTrue(all.contains("Kandy Sports Hub"));
        assertTrue(all.contains("Carrom House"));
        assertTrue(all.contains("Mixed Club"));
    }

    private List<String> search(String sportId, FacilityType facilityType) {
        return venueRepository.searchVenues(statuses, null, null, sportId, facilityType, null, pageable)
                .getContent().stream()
                .map(Venue::getName)
                .toList();
    }

    private Venue activeVenue(Business business, String name) {
        return Venue.builder()
                .business(business)
                .name(name)
                .address("Kandy")
                .city("Kandy")
                .status(VenueStatus.ACTIVE)
                .build();
    }

    private Court court(Venue venue, Sport sport, FacilityType facilityType) {
        return Court.builder()
                .venue(venue)
                .sport(sport)
                .name(sport.getName() + " " + facilityType)
                .hourlyRate(new BigDecimal("2500"))
                .facilityType(facilityType)
                .status(CourtStatus.ACTIVE)
                .build();
    }
}
