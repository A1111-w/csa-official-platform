package com.csa.official.modules.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csa.official.modules.resume.entity.Resume;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {

    @Update("""
            UPDATE biz_resume
            SET git_sync_status = 'SYNCING',
                git_sync_run_id = #{runId},
                git_sync_started_at = #{startedAt},
                git_sync_completed_at = NULL,
                git_sync_error_code = NULL,
                git_sync_branch = NULL,
                git_sync_commit = NULL,
                git_sync_size_bytes = NULL
            WHERE user_id = #{userId}
              AND deleted = 0
              AND git_repo_url IS NOT NULL
              AND git_repo_url <> ''
              AND git_repo_url = #{repoUrl}
              AND (git_sync_status <> 'SYNCING'
                   OR git_sync_started_at IS NULL
                   OR git_sync_started_at < #{staleBefore})
            """)
    int claimGitSync(@Param("userId") Long userId,
                     @Param("repoUrl") String repoUrl,
                     @Param("runId") String runId,
                     @Param("startedAt") LocalDateTime startedAt,
                     @Param("staleBefore") LocalDateTime staleBefore);

    @Update("""
            UPDATE biz_resume
            SET git_sync_status = 'SUCCEEDED',
                git_sync_completed_at = #{completedAt},
                git_sync_error_code = NULL,
                git_sync_branch = #{branch},
                git_sync_commit = #{commit},
                git_sync_size_bytes = #{sizeBytes}
            WHERE user_id = #{userId}
              AND deleted = 0
              AND git_sync_status = 'SYNCING'
              AND git_sync_run_id = #{runId}
            """)
    int completeGitSync(@Param("userId") Long userId,
                        @Param("runId") String runId,
                        @Param("completedAt") LocalDateTime completedAt,
                        @Param("branch") String branch,
                        @Param("commit") String commit,
                        @Param("sizeBytes") long sizeBytes);

    @Update("""
            UPDATE biz_resume
            SET git_sync_status = 'FAILED',
                git_sync_completed_at = #{completedAt},
                git_sync_error_code = #{errorCode},
                git_sync_branch = NULL,
                git_sync_commit = NULL,
                git_sync_size_bytes = NULL
            WHERE user_id = #{userId}
              AND deleted = 0
              AND git_sync_status = 'SYNCING'
              AND git_sync_run_id = #{runId}
            """)
    int failGitSync(@Param("userId") Long userId,
                    @Param("runId") String runId,
                    @Param("completedAt") LocalDateTime completedAt,
                    @Param("errorCode") String errorCode);

    @Update("""
            UPDATE biz_resume
            SET content = NULL,
                git_repo_url = NULL,
                status = 0,
                reject_reason = NULL,
                audit_by = NULL,
                audit_time = NULL,
                git_sync_status = 'NOT_SYNCED',
                git_sync_run_id = NULL,
                git_sync_started_at = NULL,
                git_sync_completed_at = NULL,
                git_sync_error_code = NULL,
                git_sync_branch = NULL,
                git_sync_commit = NULL,
                git_sync_size_bytes = NULL
            WHERE user_id = #{userId}
            """)
    int anonymizeByUserId(@Param("userId") Long userId);
}
