package com.csa.official.modules.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csa.official.modules.sys.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Update("UPDATE sys_user SET last_login_at = #{loginAt} WHERE id = #{userId}")
    int updateLastLoginAt(@Param("userId") Long userId, @Param("loginAt") LocalDateTime loginAt);

    @Update("""
            UPDATE sys_user
            SET password = #{passwordHash},
                password_changed_at = #{changedAt},
                session_version = session_version + 1
            WHERE id = #{userId} AND account_status = 'ACTIVE'
            """)
    int updatePasswordAndRevokeSessions(@Param("userId") Long userId,
                                        @Param("passwordHash") String passwordHash,
                                        @Param("changedAt") LocalDateTime changedAt);

    @Update("UPDATE sys_user SET session_version = session_version + 1 WHERE id = #{userId}")
    int incrementSessionVersion(@Param("userId") Long userId);

    @Update("""
            UPDATE sys_user
            SET account_status = 'DISABLED', deactivated_at = #{changedAt},
                session_version = session_version + 1
            WHERE id = #{userId} AND account_status = 'ACTIVE'
            """)
    int deactivateAccount(@Param("userId") Long userId, @Param("changedAt") LocalDateTime changedAt);

    @Update("""
            UPDATE sys_user
            SET account_status = 'DELETION_PENDING', deletion_requested_at = #{changedAt},
                session_version = session_version + 1
            WHERE id = #{userId} AND account_status = 'ACTIVE'
            """)
    int requestAccountDeletion(@Param("userId") Long userId, @Param("changedAt") LocalDateTime changedAt);

    @Select("""
            SELECT COALESCE(SUM(CASE
                       WHEN role_level = #{presidentRole} THEN 2
                       WHEN role_level = #{ministerRole} THEN 1
                       ELSE 0
                   END), 0)
            FROM sys_user
            WHERE role_level IN (#{ministerRole}, #{presidentRole})
              AND (deleted = 0 OR deleted IS NULL)
              AND id != #{excludedUserId}
            """)
    Integer selectEligibleVoteWeight(
            @Param("excludedUserId") Long excludedUserId,
            @Param("ministerRole") int ministerRole,
            @Param("presidentRole") int presidentRole);
}
