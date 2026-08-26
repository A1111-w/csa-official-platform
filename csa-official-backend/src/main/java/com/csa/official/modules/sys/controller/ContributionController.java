package com.csa.official.modules.sys.controller;

import com.csa.official.common.result.R;
import com.csa.official.common.util.PageUtils;
import com.csa.official.modules.sys.mapper.ContributionLogMapper;
import com.csa.official.modules.sys.service.ContributionQueryService;
import com.csa.official.modules.sys.service.ContributionService;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.dto.ContributionAwardRequest;
import com.csa.official.modules.sys.vo.ContributionWallVO;
import com.csa.official.modules.sys.vo.ContributionRankVO;
import com.csa.official.modules.sys.vo.ContributionAwardVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ContributionController {

    private final ContributionLogMapper logMapper;
    private final ContributionQueryService contributionQueryService;
    private final ContributionService contributionService;

    public ContributionController(ContributionLogMapper logMapper,
                                  ContributionQueryService contributionQueryService,
                                  ContributionService contributionService) {
        this.logMapper = logMapper;
        this.contributionQueryService = contributionQueryService;
        this.contributionService = contributionService;
    }

    @GetMapping("/public/contribution/wall")
    public R<List<ContributionWallVO>> getWall(
            @RequestParam(required = false) Integer limit) {
        int safeLimit = PageUtils.clampLimit(limit, 100);
        return R.ok(logMapper.selectWall(safeLimit));
    }

    @GetMapping("/public/contribution/rank")
    public R<List<ContributionRankVO>> getRank(@RequestParam(required = false) Integer limit) {
        int safeLimit = PageUtils.clampLimit(limit, 10);
        return R.ok(contributionQueryService.getRank(safeLimit));
    }

    @PreAuthorize("hasRole('LEVEL_4')")
    @PostMapping("/sys/contribution/award")
    public R<String> award(@RequestBody @Valid ContributionAwardRequest request) {
        contributionService.award(request, SecurityUtils.getUserId());
        return R.ok("Award granted");
    }

    @PreAuthorize("hasRole('LEVEL_4')")
    @GetMapping("/sys/contribution/awards")
    public R<Page<ContributionAwardVO>> listAwards(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String source) {
        return R.ok(contributionService.listAwards(page, size, keyword, type, source));
    }

}
