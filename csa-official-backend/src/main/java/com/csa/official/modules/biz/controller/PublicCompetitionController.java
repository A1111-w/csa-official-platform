package com.csa.official.modules.biz.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.common.result.R;
import com.csa.official.modules.biz.service.CompetitionService;
import com.csa.official.modules.biz.vo.CompetitionDetailVO;
import com.csa.official.modules.biz.vo.CompetitionListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/competitions")
public class PublicCompetitionController {

    @Autowired
    private CompetitionService competitionService;

    @GetMapping
    public R<Page<CompetitionListVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(competitionService.getPublicCompetitionPage(page, size));
    }

    /**
     * 公开竞赛详情。未发布的竞赛在这里会返回 404，不会泄露给未登录用户。
     */
    @GetMapping("/{id}")
    public R<CompetitionDetailVO> detail(@PathVariable Long id) {
        return R.ok(competitionService.getPublicCompetitionDetail(id));
    }
}
