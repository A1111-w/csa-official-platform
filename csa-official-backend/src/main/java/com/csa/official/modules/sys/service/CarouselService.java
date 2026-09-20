package com.csa.official.modules.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.common.cache.CacheKeys;
import com.csa.official.common.cache.CacheTtlPolicy;
import com.csa.official.common.cache.PublicReadCache;
import com.csa.official.common.exception.CsaException;
import com.csa.official.common.util.PageUtils;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.dto.CarouselSaveRequest;
import com.csa.official.modules.sys.entity.Carousel;
import com.csa.official.modules.sys.entity.StoredFile;
import com.csa.official.modules.sys.mapper.CarouselMapper;
import com.csa.official.modules.sys.mapper.StoredFileMapper;
import com.csa.official.modules.sys.vo.CarouselAdminVO;
import com.csa.official.modules.sys.vo.CarouselVO;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CarouselService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");

    private final CarouselMapper carouselMapper;
    private final StoredFileMapper storedFileMapper;
    private final AuditService auditService;
    private final PublicReadCache cache;
    private final CacheTtlPolicy ttl;

    public CarouselService(CarouselMapper carouselMapper,
                              StoredFileMapper storedFileMapper,
                              AuditService auditService, PublicReadCache cache, CacheTtlPolicy ttl) {
        this.carouselMapper = carouselMapper;
        this.storedFileMapper = storedFileMapper;
        this.auditService = auditService;
        this.cache = cache;
        this.ttl = ttl;
    }

    // === 公开接口 (首页加载用) ===
    // 注意路径以 /api/public 开头，方便Security放行
    public List<CarouselVO> getPublicList() {
        return cache.get("carousel", CacheKeys.carousel(), null, new TypeReference<List<CarouselVO>>() {},
                ttl.carouselSeconds(), this::loadPublicList);
    }

    private List<CarouselVO> loadPublicList() {
        List<Carousel> list = carouselMapper.selectList(new LambdaQueryWrapper<Carousel>()
                .eq(Carousel::getStatus, 1) // 只查启用的
                .orderByAsc(Carousel::getSortOrder) // 按顺序排
                .orderByDesc(Carousel::getCreateTime)
                .last("LIMIT " + PageUtils.MAX_LIST_LIMIT)); // 兜底，防止后台误配大量轮播图拖垮首页
        return list.stream().map(CarouselVO::from).toList();
    }

    // === 管理接口 (列表、增删改) ===

    public List<CarouselAdminVO> getAdminList() {
        List<Carousel> list = carouselMapper.selectList(new LambdaQueryWrapper<Carousel>()
                .orderByAsc(Carousel::getSortOrder)
                .orderByDesc(Carousel::getCreateTime)
                .last("LIMIT " + PageUtils.MAX_LIST_LIMIT));
        return list.stream().map(CarouselAdminVO::from).toList();
    }

    // 保存/修改轮播图 (会长及以上)
    @Transactional(rollbackFor = Exception.class)
    public void save(CarouselSaveRequest request) {
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
        cache.invalidateAfterCommit("carousel", String.valueOf(carousel.getId()));
    }

    // 删除轮播图
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        int rows = carouselMapper.deleteById(id);
        if (rows > 0) {
            auditService.recordBestEffort("CAROUSEL_DELETE", "CAROUSEL", String.valueOf(id),
                    "SUCCESS", null, Map.of());
        }
        if (rows <= 0) {
            throw new CsaException(HttpStatus.NOT_FOUND.value(), "轮播图不存在");
        }
        cache.invalidateAfterCommit("carousel", String.valueOf(id));
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
