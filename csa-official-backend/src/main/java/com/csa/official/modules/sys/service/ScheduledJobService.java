package com.csa.official.modules.sys.service;

import com.csa.official.common.cache.KeyValueStore;
import com.csa.official.modules.sys.entity.ScheduledJobExecution;
import com.csa.official.modules.sys.mapper.ScheduledJobExecutionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ScheduledJobService {

    private static final String LOCK_PREFIX = "lock:scheduled-job:";

    private final KeyValueStore keyValueStore;
    private final ScheduledJobExecutionMapper executionMapper;
    private final TransactionTemplate transactionTemplate;
    private final long lockSeconds;

    public ScheduledJobService(KeyValueStore keyValueStore,
                               ScheduledJobExecutionMapper executionMapper,
                               TransactionTemplate transactionTemplate,
                               @Value("${csa.scheduling.lock-seconds:900}") long lockSeconds) {
        this.keyValueStore = keyValueStore;
        this.executionMapper = executionMapper;
        this.transactionTemplate = transactionTemplate;
        this.lockSeconds = Math.max(30, lockSeconds);
    }

    public boolean runOnce(String jobName, String idempotencyKey, Runnable work) {
        String lockKey = LOCK_PREFIX + jobName;
        String lockToken = UUID.randomUUID().toString();
        if (!keyValueStore.setIfAbsent(lockKey, lockToken, lockSeconds, TimeUnit.SECONDS)) {
            log.info("Scheduled job is already running: job={}", jobName);
            return false;
        }

        try {
            ScheduledJobExecution execution = transactionTemplate.execute(status -> claim(jobName, idempotencyKey));
            if (execution == null) {
                return false;
            }

            try {
                transactionTemplate.executeWithoutResult(status -> {
                    work.run();
                    executionMapper.markFinished(execution.getId(), "SUCCESS", LocalDateTime.now());
                });
                return true;
            } catch (RuntimeException e) {
                transactionTemplate.executeWithoutResult(status ->
                        executionMapper.markFinished(execution.getId(), "FAILED", LocalDateTime.now()));
                throw e;
            }
        } finally {
            keyValueStore.deleteIfValue(lockKey, lockToken);
        }
    }

    private ScheduledJobExecution claim(String jobName, String idempotencyKey) {
        ScheduledJobExecution existing = executionMapper.find(jobName, idempotencyKey);
        if (existing != null) {
            if ("SUCCESS".equals(existing.getStatus())) {
                log.info("Scheduled job version already completed: job={}, key={}", jobName, idempotencyKey);
                return null;
            }
            if ("RUNNING".equals(existing.getStatus()) && existing.getStartedAt() != null
                    && Duration.between(existing.getStartedAt(), LocalDateTime.now()).getSeconds() < lockSeconds) {
                return null;
            }
            executionMapper.markRunning(existing.getId(), LocalDateTime.now());
            existing.setStatus("RUNNING");
            existing.setStartedAt(LocalDateTime.now());
            return existing;
        }

        ScheduledJobExecution created = new ScheduledJobExecution();
        created.setJobName(jobName);
        created.setIdempotencyKey(idempotencyKey);
        created.setStatus("RUNNING");
        created.setStartedAt(LocalDateTime.now());
        try {
            executionMapper.insert(created);
            return created;
        } catch (DuplicateKeyException e) {
            return null;
        }
    }
}
