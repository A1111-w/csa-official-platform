package com.csa.official.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.common.annotation.LogContribution;
import com.csa.official.common.result.R;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.entity.Resource;
import com.csa.official.modules.sys.enums.ContributionType;
import com.csa.official.modules.sys.mapper.ResourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sys/resource")
public class ResourceController {

    @Autowired
    private ResourceMapper resourceMapper;

    // 1. 资源列表 (Level 1 会员及以上可看)
    @PreAuthorize("hasRole('LEVEL_1')")
    @GetMapping("/list")
    public R<Page<Resource>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category) { // 支持按分类筛选

        Page<Resource> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Resource> query = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(category)) {
            query.eq(Resource::getCategory, category);
        }
        query.orderByDesc(Resource::getCreateTime);

        return R.ok(resourceMapper.selectPage(pageParam, query));
    }

    // 2. 发布资源 (Level 3 部长及以上)
    @PreAuthorize("hasRole('LEVEL_3')")
    @LogContribution(type = ContributionType.RES, detail = "上传资源")
    @PostMapping("/save")
    public R<String> save(@RequestBody Resource resource) {
        // 如果是新增，设置初始下载量和上传者
        if (resource.getId() == null) {
            resource.setDownloadCount(0);
            resource.setUploaderId(SecurityUtils.getUserId());
            resourceMapper.insert(resource);
        } else {
            resourceMapper.updateById(resource);
        }
        return R.ok("发布成功");
    }

    // 3. 删除资源 (Level 3 部长及以上)
    @PreAuthorize("hasRole('LEVEL_3')")
    @PostMapping("/delete")
    public R<String> delete(@RequestParam Long id) {
        resourceMapper.deleteById(id);
        return R.ok("删除成功");
    }

    // 4. 增加下载次数 (可选接口，前端下载时调用一下)
    @PreAuthorize("hasRole('LEVEL_1')")
    @PostMapping("/download")
    public R<String> download(@RequestParam Long id) {
        Resource res = resourceMapper.selectById(id);
        if (res != null) {
            res.setDownloadCount(res.getDownloadCount() + 1);
            resourceMapper.updateById(res);
        }
        return R.ok("下载计数+1");
    }
}
