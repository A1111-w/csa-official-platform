package com.csa.official.modules.sys.vo;

import lombok.Data;

@Data
public class UserDirectoryVO {
    private Long id;
    private String username;
    private String realName;
    private String avatar;
    private String email;
    private Integer roleLevel;
    private Integer positionType;
    private Long departmentId;
    private String departmentName;
}
