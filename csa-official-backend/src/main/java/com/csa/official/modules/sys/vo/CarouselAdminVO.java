package com.csa.official.modules.sys.vo;

import com.csa.official.modules.sys.entity.Carousel;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CarouselAdminVO {

    private Long id;
    private String imgUrl;
    private String targetUrl;
    private String title;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static CarouselAdminVO from(Carousel carousel) {
        CarouselAdminVO vo = new CarouselAdminVO();
        vo.setId(carousel.getId());
        vo.setImgUrl(carousel.getImgUrl());
        vo.setTargetUrl(carousel.getTargetUrl());
        vo.setTitle(carousel.getTitle());
        vo.setSortOrder(carousel.getSortOrder());
        vo.setStatus(carousel.getStatus());
        vo.setCreateTime(carousel.getCreateTime());
        vo.setUpdateTime(carousel.getUpdateTime());
        return vo;
    }
}
