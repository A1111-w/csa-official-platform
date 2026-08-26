package com.csa.official.modules.sys.vo;

import com.csa.official.modules.sys.entity.Carousel;
import lombok.Data;

/**
 * 首页轮播图对外视图。
 *
 * <p>公开接口只需要渲染用的四个字段；{@code status}、{@code sortOrder}、
 * {@code deleted} 属于后台维护信息，没必要出现在未登录用户能拿到的响应里。
 */
@Data
public class CarouselVO {

    private Long id;
    private String imgUrl;
    private String targetUrl;
    private String title;

    public static CarouselVO from(Carousel carousel) {
        CarouselVO vo = new CarouselVO();
        vo.setId(carousel.getId());
        vo.setImgUrl(carousel.getImgUrl());
        vo.setTargetUrl(carousel.getTargetUrl());
        vo.setTitle(carousel.getTitle());
        return vo;
    }
}
