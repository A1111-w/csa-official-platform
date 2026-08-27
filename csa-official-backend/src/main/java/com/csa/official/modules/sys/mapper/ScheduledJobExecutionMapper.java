package com.csa.official.modules.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csa.official.modules.sys.entity.ScheduledJobExecution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface ScheduledJobExecutionMapper extends BaseMapper<ScheduledJobExecution> {
    @Select("""
            SELECT * FROM sys_scheduled_job_execution
            WHERE job_name = #{jobName} AND idempotency_key = #{idempotencyKey}
            LIMIT 1
            """)
    ScheduledJobExecution find(@Param("jobName") String jobName,
                               @Param("idempotencyKey") String idempotencyKey);

    @Update("""
            UPDATE sys_scheduled_job_execution
            SET status = 'RUNNING', started_at = #{startedAt}, finished_at = NULL
            WHERE id = #{id}
            """)
    int markRunning(@Param("id") Long id, @Param("startedAt") LocalDateTime startedAt);

    @Update("""
            UPDATE sys_scheduled_job_execution
            SET status = #{status}, finished_at = #{finishedAt}
            WHERE id = #{id}
            """)
    int markFinished(@Param("id") Long id, @Param("status") String status,
                     @Param("finishedAt") LocalDateTime finishedAt);
}
