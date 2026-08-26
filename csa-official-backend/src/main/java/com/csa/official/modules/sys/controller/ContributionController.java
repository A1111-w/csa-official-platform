package com.csa.official.modules.sys.controller;

import com.csa.official.common.result.R;
import com.csa.official.common.util.PageUtils;
import com.csa.official.modules.sys.entity.ContributionLog;
import com.csa.official.modules.sys.mapper.ContributionLogMapper;
import com.csa.official.modules.sys.service.ContributionQueryService;
import com.csa.official.modules.sys.vo.ContributionWallVO;
import com.csa.official.modules.sys.vo.ContributionRankVO;
import org.springframework.cache.annotation.CacheEvict;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ContributionController {

    private final ContributionLogMapper logMapper;
    private final ContributionQueryService contributionQueryService;

    public ContributionController(ContributionLogMapper logMapper,
                                  ContributionQueryService contributionQueryService) {
        this.logMapper = logMapper;
        this.contributionQueryService = contributionQueryService;
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
    @CacheEvict(value = "public_contribution_rank", allEntries = true)
    @PostMapping("/sys/contribution/award")
    public R<String> award(@RequestBody AwardDto dto) {
        ContributionLog log = new ContributionLog();
        log.setUserId(dto.getUserId());
        log.setType(dto.getType());
        log.setScore(dto.getScore());
        log.setDetail(dto.getReason());
        logMapper.insert(log);
        return R.ok("Award granted");
    }

    @Data
    static class AwardDto {
        private Long userId;
        private String type;
        private BigDecimal score;
        private String reason;
    }

}
