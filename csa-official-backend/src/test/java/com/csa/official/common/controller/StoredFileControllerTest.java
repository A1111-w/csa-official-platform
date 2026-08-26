package com.csa.official.common.controller;

import com.csa.official.common.constant.RoleConsts;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.entity.StoredFile;
import com.csa.official.modules.sys.mapper.ResourceMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.mapper.StoredFileMapper;
import com.csa.official.modules.sys.service.AuditService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "csa.cache.type=memory",
        "csa.jwt.secret=01234567890123456789012345678901",
        "csa.jwt.expiration=604800000"
})
@AutoConfigureMockMvc
class StoredFileControllerTest {

    private static final Path uploadDir = Path.of(System.getProperty("java.io.tmpdir"),
            "csa-stored-file-test-" + UUID.randomUUID());

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private ResourceMapper resourceMapper;

    @MockBean
    private StoredFileMapper storedFileMapper;

    @MockBean
    private AuditService auditService;

    @DynamicPropertySource
    static void uploadProperties(DynamicPropertyRegistry registry) {
        registry.add("csa.upload-path", () -> uploadDir.toString());
    }

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(uploadDir.resolve("1"));
        Files.writeString(uploadDir.resolve("1").resolve("note.txt"), "hello", StandardCharsets.UTF_8);
    }

    @AfterAll
    static void cleanUp() throws IOException {
        if (!Files.exists(uploadDir)) {
            return;
        }

        try (var paths = Files.walk(uploadDir)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best-effort cleanup for temporary test files.
                        }
                    });
        }
    }

    @Test
    @WithMockUser(username = "owner", authorities = { "ROLE_LEVEL_1" })
    void ownerCanDownloadOwnStoredFile() throws Exception {
        when(userMapper.selectOne(any())).thenReturn(buildUser(1L, "owner"));

        mockMvc.perform(get("/files/1/note.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(content().bytes("hello".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @WithMockUser(username = "other", authorities = { "ROLE_LEVEL_1" })
    void otherMemberCannotDownloadUnpublishedFile() throws Exception {
        when(userMapper.selectOne(any())).thenReturn(buildUser(2L, "other"));
        when(resourceMapper.exists(any())).thenReturn(false);

        mockMvc.perform(get("/files/1/note.txt"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser(username = "other", authorities = { "ROLE_LEVEL_1" })
    void memberCanDownloadPublishedResourceFile() throws Exception {
        when(userMapper.selectOne(any())).thenReturn(buildUser(2L, "other"));
        when(resourceMapper.exists(any())).thenReturn(true);

        mockMvc.perform(get("/files/1/note.txt"))
                .andExpect(status().isOk())
                .andExpect(content().bytes("hello".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @WithMockUser(username = "owner", authorities = { "ROLE_LEVEL_1" })
    void rejectsPathWhoseActiveMetadataNamesAnotherOwner() throws Exception {
        when(userMapper.selectOne(any())).thenReturn(buildUser(1L, "owner"));
        StoredFile metadata = new StoredFile();
        metadata.setOwnerUserId(99L);
        metadata.setStorageKey("/files/1/note.txt");
        metadata.setStatus("ACTIVE");
        when(storedFileMapper.findActiveByStorageKey("/files/1/note.txt")).thenReturn(metadata);

        mockMvc.perform(get("/files/1/note.txt"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @WithMockUser(username = "owner", authorities = { "ROLE_LEVEL_1" })
    void rejectsPhysicalFileWhenMetadataExistsButIsNotActive() throws Exception {
        when(userMapper.selectOne(any())).thenReturn(buildUser(1L, "owner"));
        when(storedFileMapper.findActiveByStorageKey("/files/1/note.txt")).thenReturn(null);
        when(storedFileMapper.countByStorageKey("/files/1/note.txt")).thenReturn(1);

        mockMvc.perform(get("/files/1/note.txt"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    private User buildUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRoleLevel(RoleConsts.MEMBER);
        user.setDeleted(0);
        return user;
    }
}
