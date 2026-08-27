package com.csa.official.modules.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.common.constant.RoleConsts;
import com.csa.official.common.exception.CsaException;
import com.csa.official.common.util.PageUtils;
import com.csa.official.modules.sys.entity.Resource;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.ResourceMapper;
import com.csa.official.modules.sys.vo.ResourceVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 资源库业务逻辑。
 *
 * <p>这一层是从 {@code ResourceController} 里抽出来的。原先分页查询、归属校验、
 * 下载计数全部堆在 Controller，带来三个问题：Controller 没法加事务、
 * 业务规则不能被其它入口（比如后续的 Agent 工具）复用、单元测试必须起 Web 环境。
 */
@Service
public class ResourceService {

    private final ResourceMapper resourceMapper;
    private final AuditService auditService;

    public ResourceService(ResourceMapper resourceMapper, AuditService auditService) {
        this.resourceMapper = resourceMapper;
        this.auditService = auditService;
    }

    /**
     * 分页查询资源。size 由 {@link PageUtils} 收敛，避免前端传超大 size 拖垮数据库。
     */
    public Page<ResourceVO> listResources(Integer page, Integer size, String category) {
        Page<Resource> pageParam = PageUtils.of(page, size);
        LambdaQueryWrapper<Resource> query = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(category)) {
            query.eq(Resource::getCategory, category.trim());
        }
        query.orderByDesc(Resource::getCreateTime);

        Page<Resource> entityPage = resourceMapper.selectPage(pageParam, query);

        Page<ResourceVO> result = new Page<>(
                entityPage.getCurrent(),
                entityPage.getSize(),
                entityPage.getTotal());
        result.setRecords(entityPage.getRecords().stream().map(ResourceVO::from).toList());
        return result;
    }

    /**
     * 查询全部资源分类。用 {@code SELECT DISTINCT category} 让数据库去重，
     * 而不是把整表拉回 JVM 再 distinct。
     */
    public List<String> listCategories() {
        return resourceMapper.selectList(new QueryWrapper<Resource>()
                        .select("DISTINCT category")
                        .isNotNull("category")
                        .ne("category", "")
                        .orderByAsc("category"))
                .stream()
                .map(Resource::getCategory)
                .filter(StringUtils::hasText)
                .toList();
    }

    /**
     * 新增或更新资源。
     *
     * <p>加 {@code @Transactional} 的原因：更新分支里先 {@code selectById} 做归属校验、
     * 再 {@code updateById}，这是一个「读-判断-写」序列，需要放在同一个事务里，
     * 否则中途资源被删除会出现校验通过但更新落空的情况。
     *
     * @param currentUser 当前登录用户，用于判断是否有权修改这条资源
     * @return 保存后的资源 id
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveResource(Long id, String title, String summary, String fileUrl, String category, User currentUser) {
        Resource resource = new Resource();
        resource.setId(id);
        resource.setTitle(title.trim());
        resource.setSummary(summary);
        resource.setFileUrl(fileUrl.trim());
        resource.setCategory(StringUtils.hasText(category) ? category.trim() : null);

        if (id == null) {
            resource.setDownloadCount(0);
            resource.setUploaderId(currentUser.getId());
            resourceMapper.insert(resource);
            auditService.recordBestEffort("RESOURCE_CREATE", "RESOURCE", String.valueOf(resource.getId()),
                    "SUCCESS", null, Map.of("ownerUserId", currentUser.getId()));
            return resource.getId();
        }

        assertCanManage(id, currentUser);
        int rows = resourceMapper.updateById(resource);
        if (rows <= 0) {
            throw new CsaException(HttpStatus.NOT_FOUND.value(), "资源不存在");
        }
        auditService.recordBestEffort("RESOURCE_UPDATE", "RESOURCE", String.valueOf(id),
                "SUCCESS", null, Map.of("ownerUserId", currentUser.getId()));
        return id;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteResource(Long id, User currentUser) {
        assertCanManage(id, currentUser);
        if (resourceMapper.deleteById(id) <= 0) {
            throw new CsaException(HttpStatus.NOT_FOUND.value(), "资源不存在");
        }
        auditService.recordBestEffort("RESOURCE_DELETE", "RESOURCE", String.valueOf(id),
                "SUCCESS", null, Map.of("operatorUserId", currentUser.getId()));
    }

    /**
     * 下载计数 +1。
     *
     * <p>用 {@code setSql("download_count = COALESCE(download_count, 0) + 1")} 让数据库
     * 原子自增，而不是「查出来 +1 再写回去」——后者在并发下载时会丢计数。
     */
    public void increaseDownloadCount(Long id) {
        int rows = resourceMapper.update(null, new LambdaUpdateWrapper<Resource>()
                .setSql("download_count = COALESCE(download_count, 0) + 1")
                .eq(Resource::getId, id));
        if (rows <= 0) {
            throw new CsaException(HttpStatus.NOT_FOUND.value(), "资源不存在");
        }
    }

    /**
     * 资源归属校验：上传者本人可以改，会长及以上可以改任何人的资源。
     */
    private void assertCanManage(Long resourceId, User currentUser) {
        Resource existing = resourceMapper.selectById(resourceId);
        if (existing == null) {
            throw new CsaException(HttpStatus.NOT_FOUND.value(), "Resource not found");
        }

        boolean elevated = currentUser.getRoleLevel() != null
                && currentUser.getRoleLevel() >= RoleConsts.PRESIDENT;
        boolean owner = currentUser.getId().equals(existing.getUploaderId());
        if (!elevated && !owner) {
            throw new CsaException(HttpStatus.FORBIDDEN.value(), "You cannot modify another user's resource");
        }
    }
}
