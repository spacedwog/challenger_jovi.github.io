package io.jovi.upload.model;

import java.nio.file.Path;
import java.time.OffsetDateTime;

public record UploadRecord(
        long mediaId,
        long captureId,
        long userId,
        String mode,
        String quality,
        String captureType,
        String originalFileName,
        String storedFileName,
        String contentType,
        long sizeBytes,
        Path filePath,
        OffsetDateTime uploadedAt
) {
}
