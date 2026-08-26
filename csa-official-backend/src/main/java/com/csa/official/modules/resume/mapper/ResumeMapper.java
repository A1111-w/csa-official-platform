package com.csa.official.modules.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csa.official.modules.resume.entity.Resume;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {

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
