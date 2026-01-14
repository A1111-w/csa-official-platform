package com.csa.official.modules.sys.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExportDto {
    // 1. 想要导出的列 (有序)
    private List<String> columns;

    // 2. 时间范围
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    // 3.  新增：全能筛选条件
    private String college;    // 筛选学院
    private String className;  // 筛选班级
    private String realName;   // 筛选姓名 (模糊查询)
    private String studentId;  // 筛选学号
    private Integer roleLevel; // 筛选等级 (比如只导部长)
    private String inviteCode; // 筛选特定邀请码
}