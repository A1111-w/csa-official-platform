package com.csa.official.common.cache;

import com.csa.official.modules.biz.vo.CompetitionListVO;

import java.util.List;

/**
 * Versioned JSON shape for the public competition page cache.
 */
public record CachedCompetitionPage(
        long current,
        long size,
        long total,
        List<CompetitionListVO> records) {
}
