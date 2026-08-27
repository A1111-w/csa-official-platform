package com.csa.official.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_stored_file")
public class StoredFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ownerUserId;
    private String storageKey;
    private String originalName;
    private String extension;
    private String contentType;
    private Long sizeBytes;
    private String sha256;
    private String storageProvider;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime lastAccessTime;
    private LocalDateTime deletedAt;
}
