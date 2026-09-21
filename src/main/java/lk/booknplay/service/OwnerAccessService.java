package lk.booknplay.service;

import lk.booknplay.entity.Business;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.Venue;

public interface OwnerAccessService {
    Business requireBusiness(String ownerEmail);
    Venue requireVenue(String ownerEmail, String venueId);
    Court requireCourt(String ownerEmail, String courtId);
}
