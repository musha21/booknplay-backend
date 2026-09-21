package lk.booknplay.config;

import lk.booknplay.entity.Sport;
import lk.booknplay.enums.VenueStatus;
import lk.booknplay.repository.SportRepository;
import lk.booknplay.repository.VenueRepository;
import lk.booknplay.util.SportCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CatalogDataSeeder implements ApplicationRunner {

    private final SportRepository sportRepository;
    private final VenueRepository venueRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String name : SportCatalog.CANONICAL) {
            if (sportRepository.findByNameIgnoreCase(name).isEmpty()) {
                sportRepository.save(Sport.builder().name(name).isActive(true).build());
            }
        }
        venueRepository.activateVenuesWithCourts(
                VenueStatus.ACTIVE,
                List.of(VenueStatus.DRAFT, VenueStatus.PENDING_APPROVAL)
        );
    }
}
