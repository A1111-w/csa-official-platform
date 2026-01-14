package com.csa.official.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.common.annotation.LogContribution;
import com.csa.official.common.result.R;
import com.csa.official.modules.sys.entity.Carousel;
import com.csa.official.modules.sys.enums.ContributionType;
import com.csa.official.modules.sys.mapper.CarouselMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CarouselController {

    @Autowired
    private CarouselMapper carouselMapper;

    // === 公开接口 (首页加载用) ===
    // 注意路径以 /api/public 开头，方便Security放行
    @GetMapping("/api/public/carousel/list")
    public R<List<Carousel>> getPublicList() {
        List<Carousel> list = carouselMapper.selectList(new LambdaQueryWrapper<Carousel>()
                .eq(Carousel::getStatus, 1) // 只查启用的
                .orderByAsc(Carousel::getSortOrder) // 按顺序排
                .orderByDesc(Carousel::getCreateTime));
        return R.ok(list);
    }

    // === 管理接口 (增删改) ===

    // 保存/修改轮播图 (部长及以上)
    @PreAuthorize("hasRole('LEVEL_3')")
    @LogContribution(type = ContributionType.OPS, detail = "更新轮播图")
    @PostMapping("/api/sys/carousel/save")
    public R<String> save(@RequestBody Carousel carousel) {
        if (carousel.getId() == null) {
            carouselMapper.insert(carousel);
        } else {
            carouselMapper.updateById(carousel);
        }
        return R.ok("保存成功");
    }

    // 删除轮播图
    @PreAuthorize("hasRole('LEVEL_3')")
    @PostMapping("/api/sys/carousel/delete")
    public R<String> delete(@RequestParam Long id) {
        carouselMapper.deleteById(id);
        return R.ok("删除成功");
    }
}