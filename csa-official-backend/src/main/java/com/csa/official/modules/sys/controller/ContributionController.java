package com.csa.official.modules.sys.controller;

import com.csa.official.common.result.R;
import com.csa.official.common.util.PageUtils;
import com.csa.official.modules.sys.entity.ContributionLog;
import com.csa.official.modules.sys.mapper.ContributionLogMapper;
import com.csa.official.modules.sys.vo.ContributionWallVO;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ContributionController {

    private final ContributionLogMapper logMapper;

    public ContributionController(ContributionLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @GetMapping("/public/contribution/wall")
    public R<List<ContributionWallVO>> getWall(
            @RequestParam(required = false) Integer limit) {
        int safeLimit = PageUtils.clampLimit(limit, 100);
        return R.ok(logMapper.selectWall(safeLimit));
    }

    @GetMapping("/public/contribution/rank")
    public R<List<RankVo>> getRank() {
        return R.ok(new ArrayList<>());
    }

    @PreAuthorize("hasRole('LEVEL_4')")
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

    @Data
    static class RankVo {
        private String username;
        private BigDecimal score;
    }
}
