package com.csa.official.modules.sys.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FileUsageMapper {

    @Insert("""
            INSERT INTO sys_file_usage (scope_type, scope_id, used_bytes)
            VALUES (#{scopeType}, #{scopeId}, 0)
            ON DUPLICATE KEY UPDATE scope_id = VALUES(scope_id)
            """)
    int ensureScope(@Param("scopeType") String scopeType, @Param("scopeId") Long scopeId);

    @Update("""
            UPDATE sys_file_usage
            SET used_bytes = used_bytes + #{bytes}, update_time = CURRENT_TIMESTAMP
            WHERE scope_type = #{scopeType}
              AND scope_id = #{scopeId}
              AND used_bytes <= #{quotaBytes} - #{bytes}
            """)
    int reserve(@Param("scopeType") String scopeType,
                @Param("scopeId") Long scopeId,
                @Param("bytes") long bytes,
                @Param("quotaBytes") long quotaBytes);

    @Update("""
            UPDATE sys_file_usage
            SET used_bytes = GREATEST(0, used_bytes - #{bytes}), update_time = CURRENT_TIMESTAMP
            WHERE scope_type = #{scopeType} AND scope_id = #{scopeId}
            """)
    int release(@Param("scopeType") String scopeType,
                @Param("scopeId") Long scopeId,
                @Param("bytes") long bytes);
}
