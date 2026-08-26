package com.csa.official.modules.sys.vo;

import com.csa.official.modules.sys.entity.Dept;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门对外视图，屏蔽 {@code deleted} 等持久层字段。
 */
@Data
public class DeptVO {

    private Long id;
    private String name;
    private String intro;
    private Long leaderId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static DeptVO from(Dept dept) {
        DeptVO vo = new DeptVO();
        vo.setId(dept.getId());
        vo.setName(dept.getName());
        vo.setIntro(dept.getIntro());
        vo.setLeaderId(dept.getLeaderId());
        vo.setCreateTime(dept.getCreateTime());
        vo.setUpdateTime(dept.getUpdateTime());
        return vo;
    }
}
