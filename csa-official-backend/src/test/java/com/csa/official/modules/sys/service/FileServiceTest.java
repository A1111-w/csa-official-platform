package com.csa.official.modules.sys.service;

import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.sys.entity.StoredFile;
import com.csa.official.modules.sys.mapper.StoredFileMapper;
import com.csa.official.modules.sys.storage.LocalFileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FileServiceTest {

    @TempDir
    Path uploadRoot;

    private FileService fileService;
    private StoredFileMapper storedFileMapper;
    private FileAccountingService fileAccountingService;

    @BeforeEach
    void setUp() {
        storedFileMapper = mock(StoredFileMapper.class);
        fileAccountingService = mock(FileAccountingService.class);
        fileService = new FileService(
                new LocalFileStorage(uploadRoot.toString()), storedFileMapper, fileAccountingService);
        ReflectionTestUtils.setField(fileService, "allowedExtensions", List.of("jpg", "jpeg", "png", "pdf", "zip"));
        ReflectionTestUtils.setField(fileService, "maxFileSizeBytes", 1024L);
        ReflectionTestUtils.setField(fileService, "userQuotaBytes", 1024L * 1024L);
        ReflectionTestUtils.setField(fileService, "schoolQuotaBytes", 10L * 1024L * 1024L);
    }

    @Test
    void savesValidPngWithSafeGeneratedName() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] {
                        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                        0x00, 0x00, 0x00, 0x0D
                });

        String url = fileService.upload(file, 42L);

        assertTrue(url.matches("/files/42/[0-9a-f\\-]{36}\\.png"));
        assertTrue(Files.exists(uploadRoot.resolve("42").resolve(url.substring(url.lastIndexOf("/") + 1))));
    }

    @Test
    void rejectsFileWhenExtensionAndContentDoNotMatch() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                "not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThrows(CsaException.class, () -> fileService.upload(file, 42L));
    }

    @Test
    void rejectsOversizedFileBeforeSaving() {
        ReflectionTestUtils.setField(fileService, "maxFileSizeBytes", 4L);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] {
                        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
                });

        assertThrows(CsaException.class, () -> fileService.upload(file, 42L));
        assertTrue(Files.notExists(uploadRoot.resolve("42")));
    }

    @Test
    void rejectsUploadWhenUserQuotaWouldBeExceeded() throws Exception {
        ReflectionTestUtils.setField(fileService, "userQuotaBytes", 8L);
        doThrow(new CsaException(413, "已超过个人文件配额"))
                .when(fileAccountingService).reserveAndRecord(any(StoredFile.class), any(Long.class), any(Long.class));

        MockMultipartFile file = validPng("avatar.png");

        CsaException exception = assertThrows(CsaException.class, () -> fileService.upload(file, 42L));

        assertThat(exception.getCode()).isEqualTo(413);
        try (var files = Files.list(uploadRoot.resolve("42"))) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void removesPhysicalFileWhenMetadataInsertFails() throws Exception {
        doThrow(new IllegalStateException("metadata failure"))
                .when(fileAccountingService).reserveAndRecord(any(StoredFile.class), any(Long.class), any(Long.class));

        assertThrows(CsaException.class, () -> fileService.upload(validPng("avatar.png"), 42L));

        if (Files.exists(uploadRoot.resolve("42"))) {
            try (var files = Files.list(uploadRoot.resolve("42"))) {
                assertThat(files).isEmpty();
            }
        }
    }

    @Test
    void persistsSha256AndStorageMetadata() throws Exception {
        MockMultipartFile file = validPng("avatar.png");

        fileService.upload(file, 42L);

        ArgumentCaptor<StoredFile> captor = ArgumentCaptor.forClass(StoredFile.class);
        verify(fileAccountingService).reserveAndRecord(captor.capture(), any(Long.class), any(Long.class));
        StoredFile metadata = captor.getValue();
        String expected = java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(file.getBytes()));
        assertThat(metadata.getOwnerUserId()).isEqualTo(42L);
        assertThat(metadata.getSha256()).isEqualTo(expected);
        assertThat(metadata.getStatus()).isEqualTo("ACTIVE");
        assertThat(metadata.getStorageKey()).startsWith("/files/42/");
    }

    private MockMultipartFile validPng(String filename) {
        return new MockMultipartFile(
                "file", filename, "image/png",
                new byte[] {
                        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                        0x00, 0x00, 0x00, 0x0D
                });
    }
}
