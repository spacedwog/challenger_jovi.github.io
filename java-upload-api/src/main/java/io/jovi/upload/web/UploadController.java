package io.jovi.upload.web;

import io.jovi.upload.model.GalleryRecord;
import io.jovi.upload.model.UploadRecord;
import io.jovi.upload.service.UploadStorageService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Validated
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final UploadStorageService uploadStorageService;

    public UploadController(UploadStorageService uploadStorageService) {
        this.uploadStorageService = uploadStorageService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "java-upload-api",
                "timestamp", OffsetDateTime.now()
        );
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "1") long userId,
            @RequestParam(defaultValue = "Selfie") String mode,
            @RequestParam(defaultValue = "Alta") String quality,
            @RequestParam(defaultValue = "smartshot-web") String captureType
    ) throws IOException {
        UploadRecord record = uploadStorageService.store(file, userId, mode, quality, captureType);
        return toResponse(record);
    }

    @PostMapping("/{mediaId}/gallery")
    public GalleryResponse addToGallery(
            @PathVariable long mediaId,
            @RequestParam(defaultValue = "1") long userId
    ) {
        GalleryRecord galleryRecord = uploadStorageService.addToGallery(mediaId, userId);
        UploadRecord uploadRecord = uploadStorageService.getUpload(mediaId);
        return new GalleryResponse(
                galleryRecord.galleryItemId(),
                galleryRecord.galleryId(),
                uploadRecord.mediaId(),
                uploadRecord.captureId(),
                galleryRecord.status(),
                galleryRecord.linkedAt(),
                uploadStorageService.countGalleryItemsByMedia(mediaId)
        );
    }

    @PostMapping("/{mediaId}/share")
    public ShareResponse share(
            @PathVariable long mediaId,
            @RequestParam(defaultValue = "WhatsApp") @NotBlank String platform
    ) {
        UploadRecord uploadRecord = uploadStorageService.getUpload(mediaId);
        String shareUrl = "/api/uploads/" + mediaId + "/file";
        return new ShareResponse(
                uploadRecord.mediaId(),
                uploadRecord.captureId(),
                platform,
                "enviado",
                shareUrl,
                "Compartilhamento preparado com sucesso para " + platform + ".",
                OffsetDateTime.now()
        );
    }

    @GetMapping
    public List<UploadResponse> list() {
        return uploadStorageService.listUploads().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{mediaId}/file")
    public ResponseEntity<Resource> getFile(@PathVariable long mediaId) {
        UploadRecord record = uploadStorageService.getUpload(mediaId);
        Resource resource = uploadStorageService.loadAsResource(mediaId);
        MediaType mediaType = MediaType.parseMediaType(record.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : record.contentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + record.storedFileName() + "\"")
                .body(resource);
    }

    private UploadResponse toResponse(UploadRecord record) {
        return new UploadResponse(
                record.mediaId(),
                record.captureId(),
                record.userId(),
                record.mode(),
                record.quality(),
                record.captureType(),
                record.originalFileName(),
                record.contentType(),
                Math.max(1, record.sizeBytes() / 1024),
                record.uploadedAt(),
                "/api/uploads/" + record.mediaId() + "/file",
                "/api/uploads/" + record.mediaId() + "/share"
        );
    }

    @ExceptionHandler({IllegalArgumentException.class, NoSuchElementException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    public record UploadResponse(
            long mediaId,
            long captureId,
            long userId,
            String mode,
            String quality,
            String captureType,
            String originalFileName,
            String contentType,
            long sizeKb,
            OffsetDateTime uploadedAt,
            String fileUrl,
            String shareUrl
    ) {
    }

    public record GalleryResponse(
            long galleryItemId,
            long galleryId,
            long mediaId,
            long captureId,
            String status,
            OffsetDateTime linkedAt,
            long totalAssociations
    ) {
    }

    public record ShareResponse(
            long mediaId,
            long captureId,
            @NotBlank String platform,
            String status,
            String shareUrl,
            String message,
            @NotNull OffsetDateTime sharedAt
    ) {
    }
}
