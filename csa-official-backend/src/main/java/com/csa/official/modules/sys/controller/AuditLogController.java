package com.csa.official.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.common.result.R;
import com.csa.official.common.util.PageUtils;
import com.csa.official.modules.sys.entity.AuditLog;
import com.csa.official.modules.sys.mapper.AuditLogMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sys/audit")
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;

    public AuditLogController(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @PreAuthorize("hasRole('LEVEL_4')")
    @GetMapping("/list")
    public R<Page<AuditLog>> list(@RequestParam(required = false) Integer page,
                                  @RequestParam(required = false) Integer size,
                                  @RequestParam(required = false) String action,
                                  @RequestParam(required = false) String result,
                                  @RequestParam(required = false) String requestId) {
        LambdaQueryWrapper<AuditLog> query = new LambdaQueryWrapper<AuditLog>()
                .eq(StringUtils.hasText(action), AuditLog::getAction, action)
                .eq(StringUtils.hasText(result), AuditLog::getResult, result)
                .eq(StringUtils.hasText(requestId), AuditLog::getRequestId, requestId)
                .orderByDesc(AuditLog::getCreateTime)
                .orderByDesc(AuditLog::getId);
        return R.ok(auditLogMapper.selectPage(PageUtils.of(page, size), query));
    }
}
