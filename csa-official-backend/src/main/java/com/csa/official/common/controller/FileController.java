package com.csa.official.common.controller;

import com.csa.official.common.result.R;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/common/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    public R<String> upload(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getUserId();
        String path = fileService.upload(file, userId);
        return R.ok(path);
    }
}