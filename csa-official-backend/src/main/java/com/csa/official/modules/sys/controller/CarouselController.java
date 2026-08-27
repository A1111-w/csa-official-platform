package com.csa.official.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.common.annotation.LogContribution;
import com.csa.official.common.exception.CsaException;
import com.csa.official.common.result.R;
import com.csa.official.common.util.PageUtils;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.dto.CarouselSaveRequest;
import com.csa.official.modules.sys.entity.Carousel;
import com.csa.official.modules.sys.entity.StoredFile;
import com.csa.official.modules.sys.enums.ContributionType;
import com.csa.official.modules.sys.mapper.CarouselMapper;
import com.csa.official.modules.sys.mapper.StoredFileMapper;
import com.csa.official.modules.sys.service.AuditService;
import com.csa.official.modules.sys.vo.CarouselAdminVO;
import com.csa.official.modules.sys.vo.CarouselVO;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class CarouselController {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");

    private final CarouselMapper carouselMapper;
    private final StoredFileMapper storedFileMapper;
    private final AuditService auditService;

    public CarouselController(CarouselMapper carouselMapper,
                              StoredFileMapper storedFileMapper,
                              AuditService auditService) {
        this.carouselMapper = carouselMapper;
        this.storedFileMapper = storedFileMapper;
        this.auditService = auditService;
    }

    // === 公开接口 (首页加载用) ===
    // 注意路径以 /api/public 开头，方便Security放行
    @GetMapping("/api/public/carousel/list")
    @Cacheable(value = "public_carousel", key = "'active'")
    public R<List<CarouselVO>> getPublicList() {
        List<Carousel> list = carouselMapper.selectList(new LambdaQueryWrapper<Carousel>()
                .eq(Carousel::getStatus, 1) // 只查启用的
                .orderByAsc(Carousel::getSortOrder) // 按顺序排
                .orderByDesc(Carousel::getCreateTime)
                .last("LIMIT " + PageUtils.MAX_LIST_LIMIT)); // 兜底，防止后台误配大量轮播图拖垮首页
        return R.ok(list.stream().map(CarouselVO::from).toList());
    }

    // === 管理接口 (列表、增删改) ===

    @PreAuthorize("hasRole('LEVEL_4')")
    @GetMapping("/api/sys/carousel/list")
    public R<List<CarouselAdminVO>> getAdminList() {
        List<Carousel> list = carouselMapper.selectList(new LambdaQueryWrapper<Carousel>()
                .orderByAsc(Carousel::getSortOrder)
                .orderByDesc(Carousel::getCreateTime)
                .last("LIMIT " + PageUtils.MAX_LIST_LIMIT));
        return R.ok(list.stream().map(CarouselAdminVO::from).toList());
    }

    // 保存/修改轮播图 (会长及以上)
    @PreAuthorize("hasRole('LEVEL_4')")
    @LogContribution(type = ContributionType.OPS, detail = "更新轮播图")
    @PostMapping("/api/sys/carousel/save")
    @CacheEvict(value = "public_carousel", allEntries = true)
    public R<String> save(@RequestBody @Valid CarouselSaveRequest request) {
        Carousel existing = request.getId() == null ? null : carouselMapper.selectById(request.getId());
        if (request.getId() != null && existing == null) {
            throw new CsaException(HttpStatus.NOT_FOUND.value(), "轮播图不存在");
        }

        String imageUrl = normalizeImageUrl(request.getImgUrl(), existing);
        String targetUrl = normalizeTargetUrl(request.getTargetUrl());

        Carousel carousel = new Carousel();
        carousel.setId(request.getId());
        carousel.setTitle(request.getTitle().trim());
        carousel.setImgUrl(imageUrl);
        carousel.setTargetUrl(targetUrl);
        carousel.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        carousel.setStatus(request.getStatus() == null ? 1 : request.getStatus());

        if (request.getId() == null) {
            carouselMapper.insert(carousel);
        } else {
            int rows = carouselMapper.updateManagedFields(
                    carousel.getId(),
                    carousel.getTitle(),
                    carousel.getImgUrl(),
                    carousel.getTargetUrl(),
                    carousel.getSortOrder(),
                    carousel.getStatus());
            if (rows <= 0) {
                throw new CsaException(HttpStatus.NOT_FOUND.value(), "轮播图不存在");
            }
        }

        auditService.recordBestEffort("CAROUSEL_SAVE", "CAROUSEL", String.valueOf(carousel.getId()),
                "SUCCESS", null, Map.of(
                        "title", carousel.getTitle(),
                        "enabled", Integer.valueOf(1).equals(carousel.getStatus()),
                        "sortOrder", carousel.getSortOrder()));
        return R.ok("保存成功");
    }

    // 删除轮播图
    @PreAuthorize("hasRole('LEVEL_4')")
    @PostMapping("/api/sys/carousel/delete")
    @CacheEvict(value = "public_carousel", allEntries = true)
    public R<String> delete(@RequestParam Long id) {
        int rows = carouselMapper.deleteById(id);
        if (rows > 0) {
            auditService.recordBestEffort("CAROUSEL_DELETE", "CAROUSEL", String.valueOf(id),
                    "SUCCESS", null, Map.of());
        }
        if (rows <= 0) {
            throw new CsaException(HttpStatus.NOT_FOUND.value(), "轮播图不存在");
        }
        return R.ok("删除成功");
    }

    private String normalizeImageUrl(String value, Carousel existing) {
        String normalized = value.trim();
        if (normalized.startsWith("/files/")) {
            if (normalized.contains("..") || normalized.contains("?") || normalized.contains("#")) {
                throw new CsaException(HttpStatus.BAD_REQUEST.value(), "轮播图图片路径不合法");
            }

            if (existing != null && normalized.equals(existing.getImgUrl())) {
                return normalized;
            }

            StoredFile metadata = storedFileMapper.findActiveByStorageKey(normalized);
            if (metadata == null) {
                throw new CsaException(HttpStatus.BAD_REQUEST.value(), "上传图片不存在或已失效");
            }
            if (!SecurityUtils.getUserId().equals(metadata.getOwnerUserId())) {
                throw new CsaException(HttpStatus.FORBIDDEN.value(), "不能发布其他成员的私人文件");
            }
            String extension = metadata.getExtension();
            if (!StringUtils.hasText(extension) || !IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
                throw new CsaException(HttpStatus.BAD_REQUEST.value(), "轮播图只支持 JPG、PNG 或 GIF 图片");
            }
            return normalized;
        }

        URI uri = parseHttpUrl(normalized, "轮播图图片地址不合法");
        if (uri.getUserInfo() != null) {
            throw new CsaException(HttpStatus.BAD_REQUEST.value(), "轮播图图片地址不能包含凭据");
        }
        return normalized;
    }

    private String normalizeTargetUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.startsWith("/") && !normalized.startsWith("//")) {
            if (normalized.contains("\\")) {
                throw new CsaException(HttpStatus.BAD_REQUEST.value(),
                        "轮播图跳转地址只支持站内路径或 HTTP(S) 链接");
            }
            try {
                URI localPath = new URI(normalized);
                if (localPath.isAbsolute() || localPath.getRawAuthority() != null) {
                    throw new CsaException(HttpStatus.BAD_REQUEST.value(),
                            "轮播图跳转地址只支持站内路径或 HTTP(S) 链接");
                }
            } catch (URISyntaxException e) {
                throw new CsaException(HttpStatus.BAD_REQUEST.value(),
                        "轮播图跳转地址只支持站内路径或 HTTP(S) 链接");
            }
            return normalized;
        }

        URI uri = parseHttpUrl(normalized, "轮播图跳转地址只支持站内路径或 HTTP(S) 链接");
        if (uri.getUserInfo() != null) {
            throw new CsaException(HttpStatus.BAD_REQUEST.value(), "轮播图跳转地址不能包含凭据");
        }
        return normalized;
    }

    private URI parseHttpUrl(String value, String errorMessage) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || !StringUtils.hasText(uri.getHost())) {
                throw new CsaException(HttpStatus.BAD_REQUEST.value(), errorMessage);
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new CsaException(HttpStatus.BAD_REQUEST.value(), errorMessage);
        }
    }
}
