package lk.booknplay.service;

import lk.booknplay.entity.Business;
import lk.booknplay.entity.Court;
import lk.booknplay.entity.Venue;
import lk.booknplay.enums.StaffPermission;

public interface OwnerAccessService {
    Business requireBusiness(String ownerEmail);
    /** Business owner only. Staff receive 403. */
    Business requireOwner(String ownerEmail);
    Venue requireVenue(String ownerEmail, String venueId);
    Court requireCourt(String ownerEmail, String courtId);

    /** Same as requireBusiness, then fails if subscription is expired. */
    Business requireMutableBusiness(String ownerEmail);

    /** Same as requireOwner, then fails if subscription is expired. */
    Business requireMutableOwner(String ownerEmail);

    /** Same as requireVenue, then fails if subscription is expired. */
    Venue requireMutableVenue(String ownerEmail, String venueId);

    /** Same as requireCourt, then fails if subscription is expired. */
    Court requireMutableCourt(String ownerEmail, String courtId);

    void requireMutableSubscription(Business business);

    /** Owners always pass. Active staff must have the given permission flag. */
    void requireStaffPermission(String ownerEmail, StaffPermission permission);
}
