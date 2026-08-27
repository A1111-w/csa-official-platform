package com.csa.official.modules.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csa.official.modules.sys.entity.VoteRecord;
import com.csa.official.modules.sys.vo.VoteTallyVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VoteRecordMapper extends BaseMapper<VoteRecord> {
    @Select("""
            SELECT COALESCE(SUM(CASE WHEN result = 1 THEN weight ELSE 0 END), 0) AS agreeWeight,
                   COALESCE(SUM(CASE WHEN result = 0 THEN weight ELSE 0 END), 0) AS rejectWeight
            FROM sys_vote_record
            WHERE proposal_id = #{proposalId}
            """)
    VoteTallyVO selectTally(@Param("proposalId") Long proposalId);
}
