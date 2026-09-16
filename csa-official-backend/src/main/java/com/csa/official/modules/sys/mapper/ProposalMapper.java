package com.csa.official.modules.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csa.official.modules.sys.entity.Proposal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProposalMapper extends BaseMapper<Proposal> {

    @Select("""
            SELECT *
            FROM sys_proposal
            WHERE id = #{proposalId}
              AND deleted = 0
            FOR UPDATE
            """)
    Proposal selectForUpdate(@Param("proposalId") Long proposalId);
}
