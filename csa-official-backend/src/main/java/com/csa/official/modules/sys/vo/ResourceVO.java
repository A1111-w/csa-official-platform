package com.csa.official.modules.sys.vo;

import com.csa.official.modules.sys.entity.Resource;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源库列表对外视图。
 *
 * <p>不直接返回 {@link Resource} 实体，是为了让接口字段和数据库表解耦：
 * 实体上的 {@code deleted} 逻辑删除标记属于持久层细节，没有必要暴露给前端，
 * 以后表里加字段也不会自动泄露到接口上。
 */
@Data
public class ResourceVO {

    private Long id;
    private String title;
    private String summary;
    private String fileUrl;
    private String category;
    private Long uploaderId;
    private Integer downloadCount;
    private LocalDateTime createTime;

    public static ResourceVO from(Resource resource) {
        ResourceVO vo = new ResourceVO();
        vo.setId(resource.getId());
        vo.setTitle(resource.getTitle());
        vo.setSummary(resource.getSummary());
        vo.setFileUrl(resource.getFileUrl());
        vo.setCategory(resource.getCategory());
        vo.setUploaderId(resource.getUploaderId());
        vo.setDownloadCount(resource.getDownloadCount());
        vo.setCreateTime(resource.getCreateTime());
        return vo;
    }
}
