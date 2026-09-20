package com.csa.official.modules.sys.controller;

import com.csa.official.common.annotation.LogContribution;
import com.csa.official.common.annotation.RateLimit;
import com.csa.official.common.result.R;
import com.csa.official.modules.sys.dto.CarouselSaveRequest;
import com.csa.official.modules.sys.enums.ContributionType;
import com.csa.official.modules.sys.service.CarouselService;
import com.csa.official.modules.sys.vo.CarouselAdminVO;
import com.csa.official.modules.sys.vo.CarouselVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CarouselController {
    private final CarouselService service;

    public CarouselController(CarouselService service) {
        this.service = service;
    }

    @GetMapping("/api/public/carousel/list")
    @RateLimit(key = "public_carousel", time = 60, count = 120, publicReadFallback = true)
    public R<List<CarouselVO>> getPublicList() {
        return R.ok(service.getPublicList());
    }

    @PreAuthorize("hasRole('LEVEL_4')")
    @GetMapping("/api/sys/carousel/list")
    public R<List<CarouselAdminVO>> getAdminList() {
        return R.ok(service.getAdminList());
    }

    @PreAuthorize("hasRole('LEVEL_4')")
    @LogContribution(type = ContributionType.OPS, detail = "更新轮播图")
    @PostMapping("/api/sys/carousel/save")
    public R<String> save(@RequestBody @Valid CarouselSaveRequest request) {
        service.save(request);
        return R.ok("保存成功");
    }

    @PreAuthorize("hasRole('LEVEL_4')")
    @PostMapping("/api/sys/carousel/delete")
    public R<String> delete(@RequestParam Long id) {
        service.delete(id);
        return R.ok("删除成功");
    }
}
