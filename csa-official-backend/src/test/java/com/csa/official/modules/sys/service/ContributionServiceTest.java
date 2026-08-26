package com.csa.official.modules.sys.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.common.constant.AccountStatus;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.sys.dto.ContributionAwardRequest;
import com.csa.official.modules.sys.entity.ContributionLog;
import com.csa.official.modules.sys.entity.Dept;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.ContributionLogMapper;
import com.csa.official.modules.sys.mapper.DeptMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.vo.ContributionAwardVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContributionServiceTest {

    private ContributionLogMapper contributionLogMapper;
    private UserMapper userMapper;
    private DeptMapper deptMapper;
    private AuditService auditService;
    private ContributionService contributionService;

    @BeforeEach
    void setUp() {
        contributionLogMapper = mock(ContributionLogMapper.class);
        userMapper = mock(UserMapper.class);
        deptMapper = mock(DeptMapper.class);
        auditService = mock(AuditService.class);
        contributionService = new ContributionService(
                contributionLogMapper, userMapper, deptMapper, auditService);
    }

    @Test
    void manualAwardValidatesTargetAndRecordsOperator() {
        User target = buildUser(7L, "student", "张三", 3L, AccountStatus.ACTIVE);
        when(userMapper.selectById(7L)).thenReturn(target);
        when(contributionLogMapper.insert(any(ContributionLog.class))).thenAnswer(invocation -> {
            invocation.<ContributionLog>getArgument(0).setId(41L);
            return 1;
        });

        ContributionAwardRequest request = request(7L, " dev ", "12.50", "  完成官网无障碍改造  ");
        Long contributionId = contributionService.award(request, 99L);

        ArgumentCaptor<ContributionLog> captor = ArgumentCaptor.forClass(ContributionLog.class);
        verify(contributionLogMapper).insert(captor.capture());
        ContributionLog inserted = captor.getValue();
        assertThat(contributionId).isEqualTo(41L);
        assertThat(inserted.getUserId()).isEqualTo(7L);
        assertThat(inserted.getType()).isEqualTo("DEV");
        assertThat(inserted.getScore()).isEqualByComparingTo("12.50");
        assertThat(inserted.getDetail()).isEqualTo("完成官网无障碍改造");
        assertThat(inserted.getSource()).isEqualTo("MANUAL");
        assertThat(inserted.getAwardedBy()).isEqualTo(99L);
        verify(auditService).recordBestEffort(
                eq("CONTRIBUTION_MANUAL_AWARD"), eq("USER"), eq("7"),
                eq("SUCCESS"), eq(null), anyMap());
    }

    @Test
    void unknownContributionTypeIsRejectedBeforeDatabaseWrite() {
        ContributionAwardRequest request = request(7L, "OTHER", "1", "未知类型");

        assertThatThrownBy(() -> contributionService.award(request, 99L))
                .isInstanceOf(CsaException.class)
                .hasMessageContaining("DEV");

        verify(contributionLogMapper, never()).insert(any());
        verify(userMapper, never()).selectById(any());
    }

    @Test
    void inactiveAccountCannotReceiveManualAward() {
        when(userMapper.selectById(7L))
                .thenReturn(buildUser(7L, "student", "张三", null, AccountStatus.DISABLED));

        assertThatThrownBy(() -> contributionService.award(
                request(7L, "OPS", "1", "线下运维"), 99L))
                .isInstanceOf(CsaException.class)
                .hasMessageContaining("非正常状态");

        verify(contributionLogMapper, never()).insert(any());
    }

    @Test
    void deletedAccountCannotReceiveManualAward() {
        User deleted = buildUser(7L, "student", "张三", null, AccountStatus.ACTIVE);
        deleted.setDeleted(1);
        when(userMapper.selectById(7L)).thenReturn(deleted);

        assertThatThrownBy(() -> contributionService.award(
                request(7L, "OPS", "1", "线下运维"), 99L))
                .isInstanceOf(CsaException.class)
                .hasMessageContaining("成员不存在");

        verify(contributionLogMapper, never()).insert(any());
    }

    @Test
    void anonymizedAccountCannotReceiveManualAward() {
        when(userMapper.selectById(7L))
                .thenReturn(buildUser(7L, "anonymous-7", null, null, AccountStatus.ANONYMIZED));

        assertThatThrownBy(() -> contributionService.award(
                request(7L, "OPS", "1", "线下运维"), 99L))
                .isInstanceOf(CsaException.class)
                .hasMessageContaining("匿名化账号");

        verify(contributionLogMapper, never()).insert(any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void awardHistoryIsPagedAndEnrichedWithoutNPlusOneQueries() {
        ContributionLog log = new ContributionLog();
        log.setId(41L);
        log.setUserId(7L);
        log.setType("DEV");
        log.setScore(new BigDecimal("12.50"));
        log.setDetail("完成官网无障碍改造");
        log.setSource("MANUAL");
        log.setAwardedBy(99L);
        log.setCreateTime(LocalDateTime.of(2026, 8, 26, 20, 0));

        Page<ContributionLog> entityPage = new Page<>(1, 20, 1);
        entityPage.setRecords(List.of(log));
        when(contributionLogMapper.selectPage(any(Page.class), any(Wrapper.class)))
                .thenReturn(entityPage);

        User target = buildUser(7L, "student", "张三", 3L, AccountStatus.ACTIVE);
        User operator = buildUser(99L, "president", "会长", null, AccountStatus.ACTIVE);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(target, operator));
        Dept department = new Dept();
        department.setId(3L);
        department.setName("技术部");
        when(deptMapper.selectBatchIds(anyCollection())).thenReturn(List.of(department));

        Page<ContributionAwardVO> result = contributionService.listAwards(
                1, 20, null, "DEV", "MANUAL");

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(41L);
            assertThat(item.getRealName()).isEqualTo("张三");
            assertThat(item.getDepartmentName()).isEqualTo("技术部");
            assertThat(item.getTypeLabel()).isEqualTo("官网建设");
            assertThat(item.getSourceLabel()).isEqualTo("人工补录");
            assertThat(item.getAwardedByUsername()).isEqualTo("president");
        });
        verify(userMapper).selectBatchIds(anyCollection());
        verify(deptMapper).selectBatchIds(anyCollection());
    }

    private ContributionAwardRequest request(Long userId, String type, String score, String reason) {
        ContributionAwardRequest request = new ContributionAwardRequest();
        request.setUserId(userId);
        request.setType(type);
        request.setScore(new BigDecimal(score));
        request.setReason(reason);
        return request;
    }

    private User buildUser(Long id, String username, String realName,
                           Long departmentId, String accountStatus) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(realName);
        user.setDepartmentId(departmentId);
        user.setAccountStatus(accountStatus);
        user.setDeleted(0);
        return user;
    }
}
