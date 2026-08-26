package com.csa.official.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.common.annotation.LogContribution;
import com.csa.official.common.exception.CsaException;
import com.csa.official.common.result.R;
import com.csa.official.common.util.PageUtils;
import com.csa.official.modules.sys.entity.Carousel;
import com.csa.official.modules.sys.enums.ContributionType;
import com.csa.official.modules.sys.mapper.CarouselMapper;
import com.csa.official.modules.sys.service.AuditService;
import com.csa.official.modules.sys.vo.CarouselVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class CarouselController {

    @Autowired
    private CarouselMapper carouselMapper;

    @Autowired
    private AuditService auditService;

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

    // === 管理接口 (增删改) ===

    // 保存/修改轮播图 (部长及以上)
    @PreAuthorize("hasRole('LEVEL_4')")
    @LogContribution(type = ContributionType.OPS, detail = "更新轮播图")
    @PostMapping("/api/sys/carousel/save")
    @CacheEvict(value = "public_carousel", allEntries = true)
    public R<String> save(@RequestBody Carousel carousel) {
        validate(carousel);
        carousel.setTitle(carousel.getTitle().trim());
        carousel.setImgUrl(carousel.getImgUrl().trim());
        if (StringUtils.hasText(carousel.getTargetUrl())) {
            carousel.setTargetUrl(carousel.getTargetUrl().trim());
        }

        if (carousel.getId() == null) {
            carouselMapper.insert(carousel);
            auditService.recordBestEffort("CAROUSEL_SAVE", "CAROUSEL", String.valueOf(carousel.getId()),
                    "SUCCESS", null, Map.of("enabled", Integer.valueOf(1).equals(carousel.getStatus())));
        } else {
            int rows = carouselMapper.updateById(carousel);
            if (rows > 0) {
                auditService.recordBestEffort("CAROUSEL_SAVE", "CAROUSEL", String.valueOf(carousel.getId()),
                        "SUCCESS", null, Map.of("enabled", Integer.valueOf(1).equals(carousel.getStatus())));
            }
            if (rows <= 0) {
                throw new CsaException(HttpStatus.NOT_FOUND.value(), "轮播图不存在");
            }
        }
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

    private void validate(Carousel carousel) {
        if (carousel == null || !StringUtils.hasText(carousel.getTitle()) || !StringUtils.hasText(carousel.getImgUrl())) {
            throw new CsaException(HttpStatus.BAD_REQUEST.value(), "轮播图标题和图片地址不能为空");
        }
    }
}
