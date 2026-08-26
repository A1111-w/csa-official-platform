package com.csa.official.modules.sys.service;

import com.csa.official.common.constant.RoleConsts;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.sys.entity.Dept;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.DeptMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeptServiceTest {

    @Mock
    private DeptMapper deptMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private DeptService deptService;

    @Test
    void appointLeaderRejectsPresidentOrRootAccount() {
        when(deptMapper.selectById(1L)).thenReturn(buildDept(1L, "技术部", 10L));
        when(userMapper.selectById(20L)).thenReturn(buildUser(20L, "president", RoleConsts.PRESIDENT, null, 2));

        assertThatThrownBy(() -> deptService.appointLeader(1L, 20L))
                .isInstanceOf(CsaException.class)
                .hasMessageContaining("不能被任命为部长");

        verify(userMapper, never()).updateById(org.mockito.ArgumentMatchers.any(User.class));
        verify(deptMapper, never()).updateById(org.mockito.ArgumentMatchers.any(Dept.class));
    }

    @Test
    void appointLeaderDemotesPreviousLeaderAndPromotesNewLeader() {
        when(deptMapper.selectById(1L)).thenReturn(buildDept(1L, "技术部", 10L));
        when(userMapper.selectById(10L)).thenReturn(buildUser(10L, "old-leader", RoleConsts.MINISTER, 1L, 3));
        when(userMapper.selectById(20L)).thenReturn(buildUser(20L, "new-leader", RoleConsts.MEMBER, null, 1));

        deptService.appointLeader(1L, 20L);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper, times(2)).updateById(userCaptor.capture());

        List<User> updates = userCaptor.getAllValues();
        User demotedLeader = updates.get(0);
        User promotedLeader = updates.get(1);

        assertThat(demotedLeader.getId()).isEqualTo(10L);
        assertThat(demotedLeader.getRoleLevel()).isEqualTo(RoleConsts.CORE_MEMBER);
        assertThat(demotedLeader.getPositionType()).isEqualTo(1);

        assertThat(promotedLeader.getId()).isEqualTo(20L);
        assertThat(promotedLeader.getRoleLevel()).isEqualTo(RoleConsts.MINISTER);
        assertThat(promotedLeader.getDepartmentId()).isEqualTo(1L);
        assertThat(promotedLeader.getPositionType()).isEqualTo(3);

        ArgumentCaptor<Dept> deptCaptor = ArgumentCaptor.forClass(Dept.class);
        verify(deptMapper).updateById(deptCaptor.capture());
        assertThat(deptCaptor.getValue().getLeaderId()).isEqualTo(20L);
    }

    private Dept buildDept(Long id, String name, Long leaderId) {
        Dept dept = new Dept();
        dept.setId(id);
        dept.setName(name);
        dept.setLeaderId(leaderId);
        return dept;
    }

    private User buildUser(Long id, String username, int roleLevel, Long departmentId, int positionType) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRoleLevel(roleLevel);
        user.setDepartmentId(departmentId);
        user.setPositionType(positionType);
        user.setDeleted(0);
        return user;
    }
}
