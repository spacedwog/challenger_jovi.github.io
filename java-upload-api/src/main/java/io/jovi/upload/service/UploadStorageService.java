package io.jovi.upload.service;

import io.jovi.upload.model.GalleryRecord;
import io.jovi.upload.model.UploadRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UploadStorageService {

    private final Path storageRoot;
    private final AtomicLong mediaSequence = new AtomicLong(1000);
    private final AtomicLong captureSequence = new AtomicLong(5000);
    private final AtomicLong gallerySequence = new AtomicLong(9000);
    private final Map<Long, UploadRecord> uploads = new ConcurrentHashMap<>();
    private final Map<Long, GalleryRecord> galleryItems = new ConcurrentHashMap<>();

    public UploadStorageService(@Value("${app.upload.storage-root:${java.io.tmpdir}/jovi-smartshot-uploads}") String storageRoot) throws IOException {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        Files.createDirectories(this.storageRoot);
    }

    public UploadRecord store(MultipartFile file, long userId, String mode, String quality, String captureType) throws IOException {
        validateImage(file);
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "captura" : file.getOriginalFilename());
        String extension = extensionOf(originalName, file.getContentType());
        String storedFileName = UUID.randomUUID() + extension;
        Path target = storageRoot.resolve(storedFileName).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Caminho de upload inválido.");
        }
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        long mediaId = mediaSequence.incrementAndGet();
        long captureId = captureSequence.incrementAndGet();
        UploadRecord record = new UploadRecord(
                mediaId,
                captureId,
                userId,
                normalize(mode, "Selfie"),
                normalize(quality, "Alta"),
                normalize(captureType, "smartshot-web"),
                originalName,
                storedFileName,
                file.getContentType(),
                file.getSize(),
                target,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        uploads.put(mediaId, record);
        return record;
    }

    public GalleryRecord addToGallery(long mediaId, long userId) {
        UploadRecord record = getUpload(mediaId);
        long galleryItemId = gallerySequence.incrementAndGet();
        long galleryId = userId * 10000 + LocalDate.now(ZoneOffset.UTC).getDayOfYear();
        GalleryRecord galleryRecord = new GalleryRecord(
                galleryItemId,
                galleryId,
                record.mediaId(),
                userId,
                OffsetDateTime.now(ZoneOffset.UTC),
                "selecionada"
        );
        galleryItems.put(galleryItemId, galleryRecord);
        return galleryRecord;
    }

    public UploadRecord getUpload(long mediaId) {
        UploadRecord record = uploads.get(mediaId);
        if (record == null) {
            throw new NoSuchElementException("Mídia não encontrada.");
        }
        return record;
    }

    public Resource loadAsResource(long mediaId) {
        UploadRecord record = getUpload(mediaId);
        return new FileSystemResource(record.filePath());
    }

    public List<UploadRecord> listUploads() {
        return uploads.values().stream()
                .sorted(Comparator.comparing(UploadRecord::uploadedAt).reversed())
                .toList();
    }

    public long countGalleryItemsByMedia(long mediaId) {
        return galleryItems.values().stream().filter(item -> item.mediaId() == mediaId).count();
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Selecione uma imagem para upload.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("A API aceita apenas arquivos de imagem.");
        }
    }

    private String extensionOf(String fileName, String contentType) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > -1 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex);
        }
        if (contentType == null) {
            return ".bin";
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    private String normalize(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
