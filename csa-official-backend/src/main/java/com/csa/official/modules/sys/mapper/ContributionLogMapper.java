package com.csa.official.modules.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csa.official.modules.sys.entity.ContributionLog;
import com.csa.official.modules.sys.vo.ContributionWallVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContributionLogMapper extends BaseMapper<ContributionLog> {
    @Select("""
            SELECT u.id AS userId,
                   u.real_name AS realName,
                   u.avatar AS avatar,
                   COALESCE(d.name, '') AS deptName,
                   COALESCE(SUM(CASE WHEN l.type = 'DEV' THEN l.score ELSE 0 END), 0) AS devScore,
                   SUM(CASE WHEN l.type = 'RES' THEN 1 ELSE 0 END) AS resCount,
                   SUM(CASE WHEN l.type = 'COMP' THEN 1 ELSE 0 END) AS compCount,
                   SUM(CASE WHEN l.type = 'OPS' THEN 1 ELSE 0 END) AS opsCount,
                   COALESCE(SUM(CASE WHEN l.type = 'DEV' THEN l.score ELSE 0 END), 0)
                       + SUM(CASE WHEN l.type IN ('RES', 'COMP', 'OPS') THEN 1 ELSE 0 END) AS totalSortScore
            FROM sys_contribution_log l
            JOIN sys_user u ON u.id = l.user_id AND (u.deleted = 0 OR u.deleted IS NULL)
            LEFT JOIN sys_dept d ON d.id = u.department_id AND (d.deleted = 0 OR d.deleted IS NULL)
            GROUP BY u.id, u.real_name, u.avatar, d.name
            ORDER BY totalSortScore DESC
            LIMIT #{limit}
            """)
    List<ContributionWallVO> selectWall(@Param("limit") int limit);
}
