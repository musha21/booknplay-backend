package lk.booknplay.service.impl;

import lk.booknplay.entity.Business;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.User;
import lk.booknplay.entity.Venue;
import lk.booknplay.exception.ForbiddenException;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.exception.UnauthorizedException;
import lk.booknplay.repository.BusinessRepository;
import lk.booknplay.repository.CourtRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.repository.VenueRepository;
import lk.booknplay.service.OwnerAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnerAccessServiceImpl implements OwnerAccessService {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;

    @Override
    public Business requireBusiness(String ownerEmail) {
        User user = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UnauthorizedException("Owner user not found"));
        return businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found for this owner"));
    }

    @Override
    public Venue requireVenue(String ownerEmail, String venueId) {
        Business business = requireBusiness(ownerEmail);
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + venueId));
        if (!venue.getBusiness().getId().equals(business.getId())) {
            throw new ForbiddenException("Venue does not belong to this business");
        }
        return venue;
    }

    @Override
    public Court requireCourt(String ownerEmail, String courtId) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Court not found with id: " + courtId));
        requireVenue(ownerEmail, court.getVenue().getId());
        return court;
    }
}
