package lk.booknplay.service;

import lk.booknplay.dto.request.HomepageConfigRequest;
import lk.booknplay.dto.response.HomepageConfigResponse;
import java.util.List;

public interface HomepageConfigService {
    HomepageConfigResponse publicConfig();
    HomepageConfigResponse draft();
    HomepageConfigResponse saveDraft(HomepageConfigRequest request, String adminEmail);
    HomepageConfigResponse publish(String reason, String adminEmail);
    List<HomepageConfigResponse> versions();
    HomepageConfigResponse restore(String id, String reason, String adminEmail);
}
