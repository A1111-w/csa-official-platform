package com.csa.official.modules.resume.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.common.cache.KeyValueStore;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.resume.entity.Resume;
import com.csa.official.modules.resume.enums.ResumeStatusEnum;
import com.csa.official.modules.resume.mapper.ResumeMapper;
import com.csa.official.modules.resume.vo.ResumeReviewListVO;
import com.csa.official.modules.sys.entity.Dept;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.DeptMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ResumeMapper resumeMapper;

    @Mock
    private KeyValueStore keyValueStore;

    @Mock
    private AuditService auditService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private DeptMapper deptMapper;

    @InjectMocks
    private ResumeService resumeService;

    @Test
    void reviewQueueLoadsApplicantsAndDepartmentsInBatches() {
        Resume first = buildResume(10L, 101L, ResumeStatusEnum.PENDING);
        first.setContent("Java and Spring Boot");
        Resume second = buildResume(11L, 102L, ResumeStatusEnum.PENDING);
        second.setContent("Next.js and TypeScript");

        Page<Resume> source = new Page<>(1, 10, 2);
        source.setRecords(List.of(first, second));
        when(resumeMapper.selectPage(any(Page.class), any())).thenReturn(source);

        User firstUser = buildUser(101L, "alice", 201L);
        User secondUser = buildUser(102L, "bob", 202L);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(firstUser, secondUser));

        Dept firstDept = buildDept(201L, "开发部");
        Dept secondDept = buildDept(202L, "算法部");
        when(deptMapper.selectBatchIds(any())).thenReturn(List.of(firstDept, secondDept));

        Page<ResumeReviewListVO> result = resumeService.getReviewPage(1, 10, 1);

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getRecords())
                .extracting(ResumeReviewListVO::getUsername)
                .containsExactly("alice", "bob");
        assertThat(result.getRecords())
                .extracting(ResumeReviewListVO::getDepartmentName)
                .containsExactly("开发部", "算法部");
        verify(userMapper).selectBatchIds(argThat(ids -> Set.copyOf(ids).equals(Set.of(101L, 102L))));
        verify(deptMapper).selectBatchIds(argThat(ids -> Set.copyOf(ids).equals(Set.of(201L, 202L))));
        verify(userMapper, never()).selectById(any());
        verify(deptMapper, never()).selectById(any());
    }

    @Test
    void reviewQueueRejectsDraftStatusBeforeQueryingDatabase() {
        assertThatThrownBy(() -> resumeService.getReviewPage(1, 10, 0))
                .isInstanceOf(CsaException.class)
                .extracting(error -> ((CsaException) error).getCode())
                .isEqualTo(400);

        verify(resumeMapper, never()).selectPage(any(Page.class), any());
    }

    @Test
    void reviewDetailHidesUnsubmittedDraft() {
        when(resumeMapper.selectById(10L))
                .thenReturn(buildResume(10L, 101L, ResumeStatusEnum.DRAFT));

        assertThatThrownBy(() -> resumeService.getReviewDetail(10L))
                .isInstanceOf(CsaException.class)
                .extracting(error -> ((CsaException) error).getCode())
                .isEqualTo(404);

        verify(userMapper, never()).selectById(any());
        verify(deptMapper, never()).selectById(any());
    }

    @Test
    void auditRejectsResumeThatIsNotPending() {
        when(resumeMapper.selectById(10L))
                .thenReturn(buildResume(10L, 101L, ResumeStatusEnum.APPROVED));

        assertThatThrownBy(() -> resumeService.auditResume(10L, true, null, 900L))
                .isInstanceOf(CsaException.class)
                .extracting(error -> ((CsaException) error).getCode())
                .isEqualTo(409);

        verify(resumeMapper, never()).update(any(), any());
    }

    @Test
    void rejectionRequiresReason() {
        assertThatThrownBy(() -> resumeService.auditResume(10L, false, "   ", 900L))
                .isInstanceOf(CsaException.class)
                .extracting(error -> ((CsaException) error).getCode())
                .isEqualTo(400);

        verify(resumeMapper, never()).selectById(any());
    }

    @Test
    void concurrentReviewStateChangeReturnsConflict() {
        when(resumeMapper.selectById(10L))
                .thenReturn(buildResume(10L, 101L, ResumeStatusEnum.PENDING));
        when(resumeMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> resumeService.auditResume(10L, true, null, 900L))
                .isInstanceOf(CsaException.class)
                .extracting(error -> ((CsaException) error).getCode())
                .isEqualTo(409);

        verify(auditService, never()).recordBestEffort(any(), any(), any(), any(), any(), any());
    }

    private Resume buildResume(Long id, Long userId, ResumeStatusEnum status) {
        Resume resume = new Resume();
        resume.setId(id);
        resume.setUserId(userId);
        resume.setStatus(status);
        return resume;
    }

    private User buildUser(Long id, String username, Long departmentId) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(username.toUpperCase());
        user.setDepartmentId(departmentId);
        return user;
    }

    private Dept buildDept(Long id, String name) {
        Dept dept = new Dept();
        dept.setId(id);
        dept.setName(name);
        return dept;
    }
}
