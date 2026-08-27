package com.csa.official.modules.sys.service;

import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.sys.entity.StoredFile;
import com.csa.official.modules.sys.mapper.StoredFileMapper;
import com.csa.official.modules.sys.storage.FileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.HexFormat;
import java.time.LocalDateTime;

@Slf4j
@Service
public class FileService {

    private final FileStorage fileStorage;
    private final StoredFileMapper storedFileMapper;
    private final FileAccountingService fileAccountingService;

    @Value("#{'${csa.allow-types}'.split(',')}")
    private List<String> allowedExtensions;

    @Value("${csa.upload.max-file-size-bytes:52428800}")
    private long maxFileSizeBytes;

    @Value("${csa.upload.user-quota-bytes:209715200}")
    private long userQuotaBytes;

    @Value("${csa.upload.school-quota-bytes:21474836480}")
    private long schoolQuotaBytes;

    public FileService(FileStorage fileStorage, StoredFileMapper storedFileMapper,
                       FileAccountingService fileAccountingService) {
        this.fileStorage = fileStorage;
        this.storedFileMapper = storedFileMapper;
        this.fileAccountingService = fileAccountingService;
    }

    public String upload(MultipartFile file, Long userId) {
        if (file.isEmpty()) {
            throw new CsaException(400, "不能上传空文件");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new CsaException(400, "文件过大，请压缩后再上传");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new CsaException(400, "文件名不能为空");
        }
        String extension = getExtension(originalFilename);

        if (!isAllowedExtension(extension)) {
            throw new CsaException(400, "文件类型不支持");
        }

        if (!hasExpectedSignature(file, extension)) {
            throw new CsaException(400, "文件内容与扩展名不匹配");
        }

        String newFileName = UUID.randomUUID().toString() + "." + extension;
        String storageKey = "/files/" + userId + "/" + newFileName;
        StoredFile metadata = createMetadata(file, userId, originalFilename, extension, storageKey);

        try {
            try (InputStream input = file.getInputStream()) {
                fileStorage.store(storageKey, input);
            }
        } catch (IOException e) {
            cleanupPhysicalFile(storageKey, e);
            log.error("File upload failed: ownerId={}, extension={}", userId, extension, e);
            throw new CsaException(ApiErrorCode.INTERNAL_ERROR, "文件保存失败", e);
        }

        try {
            fileAccountingService.reserveAndRecord(metadata, userQuotaBytes, schoolQuotaBytes);
        } catch (CsaException e) {
            cleanupPhysicalFile(storageKey, e);
            throw e;
        } catch (RuntimeException e) {
            cleanupPhysicalFile(storageKey, e);
            throw new CsaException(ApiErrorCode.DATABASE_ERROR, "文件元数据保存失败", e);
        }

        log.info("File upload succeeded: ownerId={}, storageProvider={}, extension={}, sizeBytes={}",
                userId, fileStorage.provider(), extension, file.getSize());
        return storageKey;
    }

    public Path resolveStoredFile(Long ownerId, String fileName) {
        if (ownerId == null || fileName == null || fileName.isBlank()
                || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new CsaException(400, "非法文件路径");
        }

        Path storedPath = fileStorage.resolve("/files/" + ownerId + "/" + fileName);
        if (!Files.isRegularFile(storedPath)) {
            throw new CsaException(404, "文件不存在");
        }
        return storedPath;
    }

    public StoredFile findActiveMetadata(String storageKey) {
        return storedFileMapper.findActiveByStorageKey(storageKey);
    }

    public boolean hasMetadata(String storageKey) {
        return storedFileMapper.countByStorageKey(storageKey) > 0;
    }

    public void markAccessed(String storageKey) {
        try {
            storedFileMapper.markAccessed(storageKey);
        } catch (RuntimeException e) {
            log.debug("Stored file access timestamp could not be updated", e);
        }
    }

    public String detectContentType(Path filePath) {
        try {
            String contentType = Files.probeContentType(filePath);
            return contentType != null ? contentType : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private String getExtension(String originalFilename) {
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            throw new CsaException(400, "文件类型不支持");
        }
        return originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String safeFilename(String originalFilename) {
        String filename = Paths.get(originalFilename).getFileName().toString();
        return filename.length() <= 255 ? filename : filename.substring(filename.length() - 255);
    }

    private String sha256(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new CsaException(ApiErrorCode.INTERNAL_ERROR, "文件校验失败", e);
        }
    }

    private StoredFile createMetadata(MultipartFile file, Long userId, String originalFilename,
                                      String extension, String storageKey) {
        StoredFile metadata = new StoredFile();
        metadata.setOwnerUserId(userId);
        metadata.setStorageKey(storageKey);
        metadata.setOriginalName(safeFilename(originalFilename));
        metadata.setExtension(extension);
        metadata.setContentType(file.getContentType() == null
                ? "application/octet-stream" : file.getContentType());
        metadata.setSizeBytes(file.getSize());
        metadata.setSha256(sha256(file));
        metadata.setStorageProvider(fileStorage.provider());
        metadata.setStatus("ACTIVE");
        metadata.setCreateTime(LocalDateTime.now());
        return metadata;
    }

    private void cleanupPhysicalFile(String storageKey, Throwable failure) {
        try {
            fileStorage.delete(storageKey);
        } catch (IOException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
            log.error("Could not clean up failed upload: storageKey={}", storageKey, cleanupFailure);
        }
    }

    private boolean isAllowedExtension(String extension) {
        return allowedExtensions.stream()
                .map(type -> type.trim().toLowerCase(Locale.ROOT))
                .anyMatch(type -> type.equals(extension));
    }

    private boolean hasExpectedSignature(MultipartFile file, String extension) {
        byte[] header = new byte[512];
        int bytesRead;

        try (InputStream inputStream = file.getInputStream()) {
            bytesRead = inputStream.read(header);
        } catch (IOException e) {
            throw new CsaException(ApiErrorCode.INTERNAL_ERROR, "文件读取失败", e);
        }

        if (bytesRead <= 0) {
            return false;
        }

        return switch (extension) {
            case "jpg", "jpeg" -> startsWith(header, bytesRead, 0xFF, 0xD8, 0xFF);
            case "png" -> startsWith(header, bytesRead, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "gif" -> startsWithAscii(header, bytesRead, "GIF87a") || startsWithAscii(header, bytesRead, "GIF89a");
            case "pdf" -> startsWithAscii(header, bytesRead, "%PDF-");
            case "zip", "docx", "pptx" -> startsWith(header, bytesRead, 0x50, 0x4B, 0x03, 0x04)
                    || startsWith(header, bytesRead, 0x50, 0x4B, 0x05, 0x06)
                    || startsWith(header, bytesRead, 0x50, 0x4B, 0x07, 0x08);
            case "rar" -> startsWith(header, bytesRead, 0x52, 0x61, 0x72, 0x21, 0x1A, 0x07);
            case "7z" -> startsWith(header, bytesRead, 0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C);
            case "gz" -> startsWith(header, bytesRead, 0x1F, 0x8B);
            case "tar" -> hasTarSignature(header, bytesRead);
            case "doc", "ppt" -> startsWith(header, bytesRead, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            default -> false;
        };
    }

    private boolean startsWithAscii(byte[] header, int bytesRead, String signature) {
        byte[] signatureBytes = signature.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        return startsWith(header, bytesRead, toUnsigned(signatureBytes));
    }

    private int[] toUnsigned(byte[] bytes) {
        int[] result = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[i] & 0xFF;
        }
        return result;
    }

    private boolean startsWith(byte[] header, int bytesRead, int... signature) {
        if (bytesRead < signature.length) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            if ((header[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean hasTarSignature(byte[] header, int bytesRead) {
        byte[] signature = "ustar".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (bytesRead < 262) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            if (header[257 + i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
