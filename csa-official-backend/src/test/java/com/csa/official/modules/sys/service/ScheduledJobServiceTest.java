package com.csa.official.modules.sys.service;

import com.csa.official.common.cache.KeyValueStore;
import com.csa.official.modules.sys.entity.ScheduledJobExecution;
import com.csa.official.modules.sys.mapper.ScheduledJobExecutionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledJobServiceTest {

    @Test
    void redisLockStopsConcurrentExecutionBeforeDatabaseClaim() {
        KeyValueStore keyValueStore = mock(KeyValueStore.class);
        ScheduledJobExecutionMapper mapper = mock(ScheduledJobExecutionMapper.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(keyValueStore.setIfAbsent(any(), any(), any(Long.class), eq(TimeUnit.SECONDS))).thenReturn(false);
        ScheduledJobService service = new ScheduledJobService(keyValueStore, mapper, transactionTemplate, 60);
        AtomicInteger executions = new AtomicInteger();

        boolean ran = service.runOnce("cleanup", "day-1", executions::incrementAndGet);

        assertThat(ran).isFalse();
        assertThat(executions).hasValue(0);
        verify(mapper, never()).find(any(), any());
    }

    @Test
    void lockOnlyExecutionDoesNotCreateDatabaseHistory() {
        KeyValueStore keyValueStore = mock(KeyValueStore.class);
        ScheduledJobExecutionMapper mapper = mock(ScheduledJobExecutionMapper.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(keyValueStore.setIfAbsent(any(), any(), any(Long.class), eq(TimeUnit.SECONDS))).thenReturn(true);
        ScheduledJobService service = new ScheduledJobService(keyValueStore, mapper, transactionTemplate, 60);
        AtomicInteger executions = new AtomicInteger();

        boolean ran = service.runWithLock("mail-recovery", executions::incrementAndGet);

        assertThat(ran).isTrue();
        assertThat(executions).hasValue(1);
        verify(mapper, never()).insert(any());
        verify(keyValueStore).deleteIfValue(any(), any());
    }

    @Test
    void successfulIdempotencyKeyPreventsDuplicateWork() {
        KeyValueStore keyValueStore = mock(KeyValueStore.class);
        ScheduledJobExecutionMapper mapper = mock(ScheduledJobExecutionMapper.class);
        TransactionTemplate transactionTemplate = transactionTemplateThatExecutesCallbacks();
        when(keyValueStore.setIfAbsent(any(), any(), any(Long.class), eq(TimeUnit.SECONDS))).thenReturn(true);
        ScheduledJobExecution completed = new ScheduledJobExecution();
        completed.setStatus("SUCCESS");
        when(mapper.find("cleanup", "day-1")).thenReturn(completed);
        ScheduledJobService service = new ScheduledJobService(keyValueStore, mapper, transactionTemplate, 60);
        AtomicInteger executions = new AtomicInteger();

        boolean ran = service.runOnce("cleanup", "day-1", executions::incrementAndGet);

        assertThat(ran).isFalse();
        assertThat(executions).hasValue(0);
        verify(keyValueStore).deleteIfValue(any(), any());
    }

    @Test
    void failedExecutionIsMarkedFailedAndCanBeRetriedLater() {
        KeyValueStore keyValueStore = mock(KeyValueStore.class);
        ScheduledJobExecutionMapper mapper = mock(ScheduledJobExecutionMapper.class);
        TransactionTemplate transactionTemplate = transactionTemplateThatExecutesCallbacks();
        when(keyValueStore.setIfAbsent(any(), any(), any(Long.class), eq(TimeUnit.SECONDS))).thenReturn(true);
        ScheduledJobExecution failed = new ScheduledJobExecution();
        failed.setId(11L);
        failed.setStatus("FAILED");
        when(mapper.find("cleanup", "day-1")).thenReturn(failed);
        ScheduledJobService service = new ScheduledJobService(keyValueStore, mapper, transactionTemplate, 60);

        assertThatThrownBy(() -> service.runOnce("cleanup", "day-1",
                () -> { throw new IllegalStateException("work failed"); }))
                .isInstanceOf(IllegalStateException.class);

        verify(mapper).markRunning(eq(11L), any());
        verify(mapper).markFinished(eq(11L), eq("FAILED"), any());
    }

    @SuppressWarnings("unchecked")
    private TransactionTemplate transactionTemplateThatExecutesCallbacks() {
        TransactionTemplate template = mock(TransactionTemplate.class);
        when(template.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback<Object>) invocation.getArgument(0))
                        .doInTransaction(mock(TransactionStatus.class)));
        doAnswer(invocation -> {
            ((java.util.function.Consumer<TransactionStatus>) invocation.getArgument(0))
                    .accept(mock(TransactionStatus.class));
            return null;
        }).when(template).executeWithoutResult(any());
        return template;
    }
}
