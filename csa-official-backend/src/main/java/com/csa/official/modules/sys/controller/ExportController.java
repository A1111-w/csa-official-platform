package com.csa.official.modules.sys.controller;

import com.csa.official.modules.sys.dto.ExportDto;
import com.csa.official.modules.sys.service.UserExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/sys/export")
public class ExportController {

    @Autowired
    private UserExportService userExportService;

    @PreAuthorize("hasRole('LEVEL_4') or hasRole('ADMIN')")
    @PostMapping("/members")
    public void exportMembers(@RequestBody ExportDto dto, HttpServletResponse response) throws IOException {

        userExportService.exportMembers(dto, response);
    }
}