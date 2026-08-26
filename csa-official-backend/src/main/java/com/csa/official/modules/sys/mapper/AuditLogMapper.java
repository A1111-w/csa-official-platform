package com.csa.official.modules.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csa.official.modules.sys.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    @Update("""
            UPDATE sys_audit_log
            SET actor_username = #{anonymousUsername}
            WHERE actor_user_id = #{userId}
            """)
    int anonymizeActorUsername(@Param("userId") Long userId,
                               @Param("anonymousUsername") String anonymousUsername);
}
