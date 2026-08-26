package com.csa.official.modules.sys.controller;

import com.csa.official.common.exception.CsaException;
import com.csa.official.common.result.R;
import com.csa.official.modules.sys.service.DeptService;
import com.csa.official.modules.sys.vo.DeptVO;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sys/dept")
public class DeptController {

    @Autowired
    private DeptService deptService;

    // 1. 查看所有部门列表 (公开接口，方便查看 ID)
    @GetMapping("/list")
    public R<List<DeptVO>> list() {
        return R.ok(deptService.getAllDepts().stream().map(DeptVO::from).toList());
    }

    /**
     * 2. 任命部长接口
     * 权限：仅限 会长(Level 4) 或 超级管理员(Root)
     * 逻辑：调用 Service 完成"新王登基、旧王降级"
     */
    @PreAuthorize("hasRole('LEVEL_4') or hasRole('ADMIN')")
    @PostMapping("/appoint")
    public R<String> appoint(@RequestBody AppointDto dto) {
        if (dto.getDeptId() == null || dto.getUserId() == null) {
            throw new CsaException(HttpStatus.BAD_REQUEST.value(), "参数不完整");
        }

        deptService.appointLeader(dto.getDeptId(), dto.getUserId());
        return R.ok("任命成功，人事变动已生效");
    }

    @Data
    static class AppointDto {
        private Long deptId; // 哪个部门
        private Long userId; // 提拔谁
    }
}
