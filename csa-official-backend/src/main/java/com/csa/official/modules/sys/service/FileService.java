package com.csa.official.modules.sys.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileService {

    @Value("${csa.upload-path}")
    private String basePath;

    @Value("#{'${csa.allow-types}'.split(',')}")
    private List<String> allowedExtensions;

    public String upload(MultipartFile file, Long userId) {
        if (file.isEmpty())
            throw new RuntimeException("不能上传空文件");

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

        if (!allowedExtensions.contains(extension)) {
            throw new RuntimeException("文件类型不支持");
        }

        String newFileName = UUID.randomUUID().toString() + "." + extension;

        Path baseDir = Paths.get(basePath);
        Path subDir = baseDir.resolve(String.valueOf(userId));

        if (!subDir.normalize().startsWith(baseDir.normalize())) {
            throw new RuntimeException("非法路径访问");
        }

        File destDir = subDir.toFile();
        if (!destDir.exists())
            destDir.mkdirs();

        try {
            log.info("用户ID [{}] 开始上传文件: {}", userId, originalFilename);
            file.transferTo(new File(destDir, newFileName));
            log.info("用户ID [{}] 上传成功，保存路径: {}", userId, newFileName);
        } catch (IOException e) {
            log.error("用户ID [{}] 上传失败", userId, e);
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }

        return "/files/" + userId + "/" + newFileName;
    }
}