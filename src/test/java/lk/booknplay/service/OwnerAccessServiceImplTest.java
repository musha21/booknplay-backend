package lk.booknplay.service;

import lk.booknplay.entity.Business;
import lk.booknplay.entity.User;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.Role;
import lk.booknplay.exception.ForbiddenException;
import lk.booknplay.repository.BusinessRepository;
import lk.booknplay.repository.CourtRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.repository.VenueRepository;
import lk.booknplay.service.impl.OwnerAccessServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerAccessServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BusinessRepository businessRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private CourtRepository courtRepository;

    @InjectMocks
    private OwnerAccessServiceImpl ownerAccessService;

    private User owner;
    private Business business;

    @BeforeEach
    void setUp() {
        owner = User.builder().id("u-owner").email("owner@example.com").role(Role.BUSINESS_OWNER).build();
        business = Business.builder().id("biz-1").ownerId("u-owner").name("Arena Co").build();
    }

    @Test
    void requireVenue_OtherBusiness_ThrowsForbidden() {
        Venue otherVenue = Venue.builder()
                .id("v-other")
                .name("Other Venue")
                .business(Business.builder().id("biz-other").ownerId("u-other").build())
                .build();

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(businessRepository.findByOwnerId("u-owner")).thenReturn(Optional.of(business));
        when(venueRepository.findById("v-other")).thenReturn(Optional.of(otherVenue));

        assertThrows(ForbiddenException.class,
                () -> ownerAccessService.requireVenue("owner@example.com", "v-other"));
    }
}
