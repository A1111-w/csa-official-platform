package com.csa.official.modules.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csa.official.modules.sys.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

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
            SELECT *
            FROM sys_user
            WHERE account_status = 'DELETION_PENDING'
              AND deletion_requested_at IS NOT NULL
              AND deletion_requested_at <= #{before}
              AND anonymized_at IS NULL
              AND (deleted = 0 OR deleted IS NULL)
            ORDER BY deletion_requested_at, id
            LIMIT #{limit}
            """)
    List<User> selectDeletionCandidates(@Param("before") LocalDateTime before,
                                        @Param("limit") int limit);

    @Update("""
            UPDATE sys_user
            SET username = #{anonymousUsername},
                password = #{passwordHash},
                real_name = NULL,
                email = NULL,
                role_level = 0,
                position_type = 0,
                department_id = NULL,
                balance = 0,
                gitea_username = NULL,
                avatar = NULL,
                phone = NULL,
                wx_open_id = NULL,
                contact = NULL,
                address = NULL,
                student_id = NULL,
                college = NULL,
                class_name = NULL,
                merchant_no = NULL,
                used_invite_code = NULL,
                account_status = 'ANONYMIZED',
                password_changed_at = #{anonymizedAt},
                deactivated_at = COALESCE(deactivated_at, #{anonymizedAt}),
                anonymized_at = #{anonymizedAt},
                privacy_consent_version = NULL,
                privacy_consent_at = NULL,
                session_version = session_version + 1
            WHERE id = #{userId}
              AND account_status = 'DELETION_PENDING'
              AND anonymized_at IS NULL
            """)
    int anonymizeAccount(@Param("userId") Long userId,
                         @Param("anonymousUsername") String anonymousUsername,
                         @Param("passwordHash") String passwordHash,
                         @Param("anonymizedAt") LocalDateTime anonymizedAt);

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
