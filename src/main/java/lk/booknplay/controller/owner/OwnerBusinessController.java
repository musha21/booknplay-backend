package lk.booknplay.controller.owner;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lk.booknplay.dto.request.BusinessUpdateRequest;
import lk.booknplay.dto.response.OwnerResponse;
import lk.booknplay.service.OwnerAuthService;
import lk.booknplay.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/owner/business")
@RequiredArgsConstructor
@Tag(name = "Owner Business")
public class OwnerBusinessController {

    private final OwnerAuthService ownerAuthService;

    @PutMapping
    public ResponseEntity<ApiResponse<OwnerResponse>> update(
            Authentication authentication,
            @Valid @RequestBody BusinessUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Business updated",
                ownerAuthService.updateBusiness(authentication.getName(), request)));
    }

    @PostMapping("/images")
    public ResponseEntity<ApiResponse<OwnerResponse>> uploadImages(
            Authentication authentication,
            @RequestParam(value = "logo", required = false) MultipartFile logo,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity.ok(ApiResponse.success(
                "Images uploaded",
                ownerAuthService.uploadBusinessImages(authentication.getName(), logo, images, profileImage)));
    }
}
