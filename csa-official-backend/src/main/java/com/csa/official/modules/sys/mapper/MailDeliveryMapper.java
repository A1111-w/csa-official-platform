package com.csa.official.modules.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csa.official.modules.sys.entity.MailDelivery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MailDeliveryMapper extends BaseMapper<MailDelivery> {

    @Select("""
            SELECT *
            FROM sys_mail_delivery
            WHERE status IN ('PENDING', 'SENDING')
              AND update_time <= #{before}
            ORDER BY update_time, id
            LIMIT #{limit}
            """)
    List<MailDelivery> selectRecoverable(@Param("before") LocalDateTime before,
                                         @Param("limit") int limit);

    @Update("""
            UPDATE sys_mail_delivery
            SET status = 'SENDING', update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND status IN ('PENDING', 'SENDING')
              AND update_time <= #{before}
            """)
    int claimRecovery(@Param("id") Long id, @Param("before") LocalDateTime before);

    @Update("""
            UPDATE sys_mail_delivery
            SET status = 'FAILED',
                last_error_code = #{errorCode},
                last_error_message = #{errorMessage},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND status IN ('PENDING', 'SENDING')
            """)
    int markRecoveryFailed(@Param("id") Long id,
                           @Param("errorCode") String errorCode,
                           @Param("errorMessage") String errorMessage);
}
