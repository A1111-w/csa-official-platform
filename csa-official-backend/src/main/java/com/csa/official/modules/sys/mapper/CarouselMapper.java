package com.csa.official.modules.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csa.official.modules.sys.entity.Carousel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CarouselMapper extends BaseMapper<Carousel> {

    @Update("""
            UPDATE sys_carousel
            SET title = #{title},
                img_url = #{imgUrl},
                target_url = #{targetUrl},
                sort_order = #{sortOrder},
                status = #{status},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND deleted = 0
            """)
    int updateManagedFields(@Param("id") Long id,
                            @Param("title") String title,
                            @Param("imgUrl") String imgUrl,
                            @Param("targetUrl") String targetUrl,
                            @Param("sortOrder") Integer sortOrder,
                            @Param("status") Integer status);
}
