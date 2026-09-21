package lk.booknplay.service.impl;

import lk.booknplay.exception.BadRequestException;
import lk.booknplay.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!ALLOWED.contains(contentType)) {
            throw new BadRequestException("Only JPEG, PNG, WEBP, or GIF images are allowed");
        }
        try {
            Path dir = Path.of(uploadDir, folder).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String ext = contentType.contains("png") ? ".png" : contentType.contains("webp") ? ".webp" : contentType.contains("gif") ? ".gif" : ".jpg";
            String filename = UUID.randomUUID() + ext;
            Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + folder + "/" + filename;
        } catch (IOException e) {
            throw new BadRequestException("Could not store image");
        }
    }
}
