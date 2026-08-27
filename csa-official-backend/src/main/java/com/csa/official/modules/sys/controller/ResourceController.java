package com.csa.official.modules.sys.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.common.annotation.LogContribution;
import com.csa.official.common.result.R;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.enums.ContributionType;
import com.csa.official.modules.sys.service.ResourceService;
import com.csa.official.modules.sys.vo.ResourceVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sys/resource")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    // 1. 资源列表 (Level 1 会员及以上可看)
    @PreAuthorize("hasRole('LEVEL_1')")
    @GetMapping("/list")
    public R<Page<ResourceVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String category) { // 支持按分类筛选
        return R.ok(resourceService.listResources(page, size, category));
    }

    @PreAuthorize("hasRole('LEVEL_1')")
    @GetMapping("/categories")
    public R<List<String>> categories() {
        return R.ok(resourceService.listCategories());
    }

    // 2. 发布资源 (Level 3 部长及以上)
    @PreAuthorize("hasRole('LEVEL_3')")
    @LogContribution(type = ContributionType.RES, detail = "上传资源")
    @PostMapping("/save")
    public R<String> save(@RequestBody @Valid SaveResourceDto dto) {
        resourceService.saveResource(
                dto.getId(),
                dto.getTitle(),
                dto.getSummary(),
                dto.getFileUrl(),
                dto.getCategory(),
                SecurityUtils.getCurrentUser());
        return R.ok("发布成功");
    }

    // 3. 删除资源 (Level 3 部长及以上)
    @PreAuthorize("hasRole('LEVEL_3')")
    @PostMapping("/delete")
    public R<String> delete(@RequestParam Long id) {
        resourceService.deleteResource(id, SecurityUtils.getCurrentUser());
        return R.ok("删除成功");
    }

    // 4. 增加下载次数 (可选接口，前端下载时调用一下)
    @PreAuthorize("hasRole('LEVEL_1')")
    @PostMapping("/download")
    public R<String> download(@RequestParam Long id) {
        resourceService.increaseDownloadCount(id);
        return R.ok("下载计数+1");
    }

    @Data
    static class SaveResourceDto {
        private Long id;

        @NotBlank(message = "资源标题不能为空")
        @Size(max = 200, message = "资源标题不能超过 200 个字符")
        private String title;

        @Size(max = 1000, message = "资源摘要不能超过 1000 个字符")
        private String summary;

        @NotBlank(message = "文件地址不能为空")
        @Size(max = 500, message = "文件地址不能超过 500 个字符")
        private String fileUrl;

        @Size(max = 64, message = "资源分类不能超过 64 个字符")
        private String category;
    }
}
