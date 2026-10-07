package lk.booknplay.service;

import lk.booknplay.dto.request.OwnerStaffInviteRequest;
import lk.booknplay.dto.request.OwnerStaffUpdateRequest;
import lk.booknplay.dto.response.OwnerStaffResponse;

import java.util.List;

public interface OwnerStaffService {
    List<OwnerStaffResponse> list(String ownerEmail);

    OwnerStaffResponse invite(String ownerEmail, OwnerStaffInviteRequest request);

    OwnerStaffResponse update(String ownerEmail, String staffId, OwnerStaffUpdateRequest request);

    void delete(String ownerEmail, String staffId);
}
