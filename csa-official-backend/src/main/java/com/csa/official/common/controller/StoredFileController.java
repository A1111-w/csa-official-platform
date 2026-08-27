package com.csa.official.common.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.common.constant.RoleConsts;
import com.csa.official.common.exception.CsaException;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.entity.Carousel;
import com.csa.official.modules.sys.entity.StoredFile;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.CarouselMapper;
import com.csa.official.modules.sys.mapper.ResourceMapper;
import com.csa.official.modules.sys.service.AuditService;
import com.csa.official.modules.sys.service.FileService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

@RestController
public class StoredFileController {

    private final FileService fileService;
    private final ResourceMapper resourceMapper;
    private final CarouselMapper carouselMapper;
    private final AuditService auditService;

    public StoredFileController(FileService fileService,
                                ResourceMapper resourceMapper,
                                CarouselMapper carouselMapper,
                                AuditService auditService) {
        this.fileService = fileService;
        this.resourceMapper = resourceMapper;
        this.carouselMapper = carouselMapper;
        this.auditService = auditService;
    }

    @GetMapping("/files/{ownerId}/{fileName:.+}")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long ownerId,
                                                       @PathVariable String fileName) throws IOException {
        String fileUrl = "/files/" + ownerId + "/" + fileName;
        StoredFile metadata = fileService.findActiveMetadata(fileUrl);

        if (metadata == null && fileService.hasMetadata(fileUrl)) {
            throw new CsaException(404, "File metadata is not active");
        }
        if (metadata != null && !ownerId.equals(metadata.getOwnerUserId())) {
            throw new CsaException(404, "File metadata does not match the requested path");
        }

        boolean activeCarouselAsset = isCarouselAsset(fileUrl, true);
        boolean carouselAsset = activeCarouselAsset;

        if (!activeCarouselAsset) {
            User currentUser = SecurityUtils.getCurrentUser();
            if (currentUser.getRoleLevel() == null || currentUser.getRoleLevel() < RoleConsts.MEMBER) {
                throw new CsaException(403, "无权访问该文件");
            }

            Long effectiveOwnerId = metadata == null ? ownerId : metadata.getOwnerUserId();
            boolean owner = effectiveOwnerId.equals(currentUser.getId());
            boolean publishedResource = isPublishedResource(fileUrl);
            if (currentUser.getRoleLevel() >= RoleConsts.PRESIDENT) {
                carouselAsset = isCarouselAsset(fileUrl, false);
            }

            if (!owner && !publishedResource && !carouselAsset) {
                throw new CsaException(403, "无权访问该文件");
            }
        }

        Path filePath = fileService.resolveStoredFile(ownerId, fileName);
        if (metadata == null) {
            auditService.recordBestEffort("LEGACY_FILE_ACCESS", "STORED_FILE", null,
                    "SUCCESS", null, Map.of("ownerUserId", ownerId));
        } else if (!carouselAsset) {
            fileService.markAccessed(fileUrl);
        }
        FileSystemResource body = new FileSystemResource(filePath);
        String downloadName = metadata == null ? filePath.getFileName().toString() : metadata.getOriginalName();
        String dispositionType = carouselAsset ? "inline" : "attachment";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileService.detectContentType(filePath)))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.builder(dispositionType)
                        .filename(downloadName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.contentLength()))
                .header("X-Content-Type-Options", "nosniff")
                .body(body);
    }

    private boolean isCarouselAsset(String fileUrl, boolean activeOnly) {
        LambdaQueryWrapper<Carousel> query = new LambdaQueryWrapper<Carousel>()
                .eq(Carousel::getImgUrl, fileUrl);
        if (activeOnly) {
            query.eq(Carousel::getStatus, 1);
        }
        return carouselMapper.exists(query);
    }

    private boolean isPublishedResource(String fileUrl) {
        return resourceMapper.exists(new LambdaQueryWrapper<com.csa.official.modules.sys.entity.Resource>()
                .eq(com.csa.official.modules.sys.entity.Resource::getFileUrl, fileUrl));
    }
}
