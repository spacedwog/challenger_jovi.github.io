package io.jovi.upload.model;

import java.time.OffsetDateTime;

public record GalleryRecord(
        long galleryItemId,
        long galleryId,
        long mediaId,
        long userId,
        OffsetDateTime linkedAt,
        String status
) {
}
