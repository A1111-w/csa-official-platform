package com.csa.official.modules.sys.service;

import com.csa.official.modules.sys.mapper.ContributionLogMapper;
import com.csa.official.modules.sys.vo.ContributionRankVO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContributionQueryService {

    private final ContributionLogMapper contributionLogMapper;

    public ContributionQueryService(ContributionLogMapper contributionLogMapper) {
        this.contributionLogMapper = contributionLogMapper;
    }

    @Cacheable(value = "public_contribution_rank", key = "#limit")
    public List<ContributionRankVO> getRank(int limit) {
        return contributionLogMapper.selectRank(limit);
    }
}
