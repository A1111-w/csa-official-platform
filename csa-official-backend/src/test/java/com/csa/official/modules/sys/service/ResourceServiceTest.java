package com.csa.official.modules.sys.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.common.constant.RoleConsts;
import com.csa.official.common.exception.CsaException;
import com.csa.official.common.util.PageUtils;
import com.csa.official.modules.sys.entity.Resource;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.ResourceMapper;
import com.csa.official.modules.sys.vo.ResourceVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 资源库 Service 的单元测试。
 *
 * <p>这些规则原先散在 Controller 里，起 Web 环境才能测；抽到 Service 之后
 * 用纯 Mockito 就能覆盖，跑得也快。
 */
@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceMapper resourceMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ResourceService resourceService;

    @Test
    void listResourcesClampsOversizedPageSize() {
        when(resourceMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        resourceService.listResources(1, 999_999, null);

        ArgumentCaptor<IPage<Resource>> captor = ArgumentCaptor.captor();
        verify(resourceMapper).selectPage(captor.capture(), any());
        assertThat(captor.getValue().getSize()).isEqualTo(PageUtils.MAX_PAGE_SIZE);
    }

    @Test
    void listResourcesMapsEntitiesToVoWithoutLeakingDeletedFlag() {
        Page<Resource> stored = new Page<>(1, 10, 1);
        stored.setRecords(List.of(buildResource(7L, 42L)));
        when(resourceMapper.selectPage(any(), any())).thenReturn(stored);

        Page<ResourceVO> result = resourceService.listResources(1, 10, null);

        assertThat(result.getRecords()).hasSize(1);
        ResourceVO vo = result.getRecords().get(0);
        assertThat(vo.getId()).isEqualTo(7L);
        assertThat(vo.getTitle()).isEqualTo("Spring 入门讲义");
        assertThat(vo.getUploaderId()).isEqualTo(42L);
        // ResourceVO 根本没有 deleted 字段，逻辑删除标记不会出现在接口响应里
        assertThat(vo).hasNoNullFieldsOrPropertiesExcept("summary", "category");
    }

    @Test
    void ownerCanUpdateOwnResource() {
        when(resourceMapper.selectById(7L)).thenReturn(buildResource(7L, 42L));
        when(resourceMapper.updateById(any(Resource.class))).thenReturn(1);

        resourceService.saveResource(7L, "新标题", "摘要", "/files/42/a.pdf", "讲义", buildUser(42L, RoleConsts.MINISTER));

        verify(resourceMapper).updateById(any(Resource.class));
    }

    @Test
    void nonOwnerBelowPresidentCannotUpdateOthersResource() {
        when(resourceMapper.selectById(7L)).thenReturn(buildResource(7L, 42L));

        assertThatThrownBy(() -> resourceService.saveResource(
                7L, "新标题", "摘要", "/files/42/a.pdf", "讲义", buildUser(99L, RoleConsts.MINISTER)))
                .isInstanceOf(CsaException.class)
                .hasMessageContaining("another user's resource");

        verify(resourceMapper, never()).updateById(any(Resource.class));
    }

    @Test
    void presidentCanUpdateOthersResource() {
        when(resourceMapper.selectById(7L)).thenReturn(buildResource(7L, 42L));
        when(resourceMapper.updateById(any(Resource.class))).thenReturn(1);

        resourceService.saveResource(
                7L, "新标题", "摘要", "/files/42/a.pdf", "讲义", buildUser(99L, RoleConsts.PRESIDENT));

        verify(resourceMapper).updateById(any(Resource.class));
    }

    @Test
    void savingNewResourceStampsUploaderAndZeroDownloads() {
        resourceService.saveResource(
                null, "  带空格的标题  ", "摘要", "  /files/42/a.pdf ", " 讲义 ", buildUser(42L, RoleConsts.MINISTER));

        ArgumentCaptor<Resource> captor = ArgumentCaptor.captor();
        verify(resourceMapper).insert(captor.capture());
        Resource inserted = captor.getValue();
        assertThat(inserted.getUploaderId()).isEqualTo(42L);
        assertThat(inserted.getDownloadCount()).isZero();
        assertThat(inserted.getTitle()).isEqualTo("带空格的标题");
        assertThat(inserted.getFileUrl()).isEqualTo("/files/42/a.pdf");
        assertThat(inserted.getCategory()).isEqualTo("讲义");
    }

    @Test
    void blankCategoryIsStoredAsNullRatherThanEmptyString() {
        resourceService.saveResource(
                null, "标题", null, "/files/42/a.pdf", "   ", buildUser(42L, RoleConsts.MINISTER));

        ArgumentCaptor<Resource> captor = ArgumentCaptor.captor();
        verify(resourceMapper).insert(captor.capture());
        assertThat(captor.getValue().getCategory()).isNull();
    }

    @Test
    void deletingMissingResourceReports404() {
        when(resourceMapper.selectById(7L)).thenReturn(null);

        assertThatThrownBy(() -> resourceService.deleteResource(7L, buildUser(42L, RoleConsts.PRESIDENT)))
                .isInstanceOf(CsaException.class)
                .extracting(e -> ((CsaException) e).getCode())
                .isEqualTo(404);
    }

    @Test
    void downloadCounterUsesAtomicSqlUpdate() {
        when(resourceMapper.update(isNull(), any())).thenReturn(1);

        resourceService.increaseDownloadCount(7L);

        // 关键：走 UPDATE ... SET download_count = download_count + 1，
        // 而不是「先查出来再写回去」，否则并发下载会丢计数
        ArgumentCaptor<Wrapper<Resource>> captor = ArgumentCaptor.captor();
        verify(resourceMapper).update(isNull(), captor.capture());
        assertThat(captor.getValue().getSqlSet()).contains("download_count");
    }

    @Test
    void downloadOnMissingResourceReports404() {
        when(resourceMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> resourceService.increaseDownloadCount(7L))
                .isInstanceOf(CsaException.class)
                .extracting(e -> ((CsaException) e).getCode())
                .isEqualTo(404);
    }

    private Resource buildResource(Long id, Long uploaderId) {
        Resource resource = new Resource();
        resource.setId(id);
        resource.setTitle("Spring 入门讲义");
        resource.setSummary("摘要");
        resource.setFileUrl("/files/42/a.pdf");
        resource.setCategory("讲义");
        resource.setUploaderId(uploaderId);
        resource.setDownloadCount(3);
        resource.setCreateTime(LocalDateTime.now());
        resource.setDeleted(0);
        return resource;
    }

    private User buildUser(Long id, int roleLevel) {
        User user = new User();
        user.setId(id);
        user.setRoleLevel(roleLevel);
        user.setDeleted(0);
        return user;
    }
}
