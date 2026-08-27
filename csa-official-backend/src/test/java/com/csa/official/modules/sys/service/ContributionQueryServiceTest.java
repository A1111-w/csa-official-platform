package com.csa.official.modules.sys.service;

import com.csa.official.modules.sys.mapper.ContributionLogMapper;
import com.csa.official.modules.sys.vo.ContributionRankVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContributionQueryServiceTest {

    @Test
    void delegatesBoundedRankQueryToDatabaseMapper() {
        ContributionLogMapper mapper = mock(ContributionLogMapper.class);
        ContributionRankVO row = new ContributionRankVO();
        when(mapper.selectRank(10)).thenReturn(List.of(row));
        ContributionQueryService service = new ContributionQueryService(mapper);

        List<ContributionRankVO> result = service.getRank(10);

        assertThat(result).containsExactly(row);
        verify(mapper).selectRank(10);
    }
}
