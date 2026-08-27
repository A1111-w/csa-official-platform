package com.csa.official.modules.sys.service;

import com.csa.official.modules.resume.entity.Resume;
import com.csa.official.modules.resume.enums.ResumeStatusEnum;
import com.csa.official.modules.resume.mapper.ResumeMapper;
import com.csa.official.modules.sys.entity.AuditLog;
import com.csa.official.modules.sys.entity.StoredFile;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.AuditLogMapper;
import com.csa.official.modules.sys.mapper.StoredFileMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.vo.PersonalDataExportVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonalDataExportServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private ResumeMapper resumeMapper;
    @Mock
    private StoredFileMapper storedFileMapper;
    @Mock
    private AuditLogMapper auditLogMapper;
    @Mock
    private AuditService auditService;

    private PersonalDataExportService service;

    @BeforeEach
    void setUp() {
        service = new PersonalDataExportService(userMapper, resumeMapper, storedFileMapper,
                auditLogMapper, auditService);
    }

    @Test
    void exportsOwnedDataThroughExplicitAllowListAndAuditsTheAction() throws Exception {
        User user = new User();
        user.setId(42L);
        user.setUsername("member");
        user.setPassword("stored-password-hash-must-never-leak");
        user.setEmail("member@example.test");
        user.setSessionVersion(7L);

        Resume resume = new Resume();
        resume.setId(8L);
        resume.setUserId(42L);
        resume.setContent("owned resume");
        resume.setStatus(ResumeStatusEnum.APPROVED);

        StoredFile file = new StoredFile();
        file.setId(9L);
        file.setOwnerUserId(42L);
        file.setStorageKey("private/internal/path");
        file.setOriginalName("portfolio.pdf");
        file.setStatus("ACTIVE");

        AuditLog event = new AuditLog();
        event.setActorUserId(42L);
        event.setAction("LOGIN_SUCCESS");
        event.setTargetId("another-user-id");
        event.setDetailsJson("{\"internal\":\"not-for-export\"}");

        when(userMapper.selectById(42L)).thenReturn(user);
        when(resumeMapper.selectOne(any())).thenReturn(resume);
        when(storedFileMapper.selectList(any())).thenReturn(List.of(file));
        when(auditLogMapper.selectList(any())).thenReturn(List.of(event));

        PersonalDataExportVO result = service.exportFor(42L);
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(result);

        assertThat(result.getAccount().getEmail()).isEqualTo("member@example.test");
        assertThat(result.getResume().getStatus()).isEqualTo(ResumeStatusEnum.APPROVED.getCode());
        assertThat(result.getUploadedFiles()).extracting(PersonalDataExportVO.StoredFileData::getOriginalName)
                .containsExactly("portfolio.pdf");
        assertThat(json).doesNotContain(
                "stored-password-hash-must-never-leak",
                "private/internal/path",
                "another-user-id",
                "not-for-export");
        verify(auditService).recordBestEffort("DATA_EXPORT", "USER", "42", "SUCCESS", null,
                Map.of("scope", "SELF", "fileCount", 1, "eventCount", 1));
    }
}
