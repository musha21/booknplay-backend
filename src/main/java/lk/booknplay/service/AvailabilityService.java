package lk.booknplay.service;

import lk.booknplay.dto.response.AvailabilityResponse;

import java.time.LocalDate;

public interface AvailabilityService {
    AvailabilityResponse getAvailability(String courtId, LocalDate date);
}
