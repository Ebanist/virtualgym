package com.gymplanner.storage;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publiczny odczyt zdjęć (ładowane przez {@code <img>}); pliki są niezmienne, więc cache na rok. */
@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "Files")
@SecurityRequirements
public class FileController {

    private static final CacheControl IMMUTABLE = CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

    private final ImageStorageService images;

    public FileController(ImageStorageService images) {
        this.images = images;
    }

    @GetMapping("/{id}")
    public ResponseEntity<InputStreamResource> original(@PathVariable UUID id) {
        StoredFile file = images.get(id);
        return ResponseEntity.ok()
                .cacheControl(IMMUTABLE)
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .contentLength(file.getSizeBytes())
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(images.open(file.getStorageKey())));
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<InputStreamResource> thumbnail(@PathVariable UUID id) {
        StoredFile file = images.get(id);
        String key = file.getThumbnailKey() != null ? file.getThumbnailKey() : file.getStorageKey();
        String type = file.getThumbnailKey() != null ? ImageStorageService.THUMBNAIL_CONTENT_TYPE : file.getContentType();
        return ResponseEntity.ok()
                .cacheControl(IMMUTABLE)
                .contentType(MediaType.parseMediaType(type))
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(images.open(key)));
    }

    public static String url(UUID fileId) {
        return fileId == null ? null : "/api/v1/files/" + fileId;
    }

    public static String thumbnailUrl(UUID fileId) {
        return fileId == null ? null : "/api/v1/files/" + fileId + "/thumbnail";
    }
}
