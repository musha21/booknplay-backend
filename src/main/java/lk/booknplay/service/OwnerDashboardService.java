package lk.booknplay.service;

import lk.booknplay.dto.response.OwnerDashboardResponse;

public interface OwnerDashboardService {
    OwnerDashboardResponse getToday(String ownerEmail);
}
