package com.csa.official.modules.sys.task;

import com.csa.official.modules.sys.service.AccountAnonymizationService;
import com.csa.official.modules.sys.service.ScheduledJobService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AccountAnonymizationTaskTest {

    @Test
    void runsThroughDistributedIdempotentScheduler() {
        AccountAnonymizationService service = mock(AccountAnonymizationService.class);
        ScheduledJobService scheduledJobService = mock(ScheduledJobService.class);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return true;
        }).when(scheduledJobService).runOnce(anyString(), anyString(), any(Runnable.class));
        AccountAnonymizationTask task = new AccountAnonymizationTask(
                service, scheduledJobService, 30, 100);

        task.anonymizeExpiredAccounts();

        verify(scheduledJobService).runOnce(
                org.mockito.ArgumentMatchers.eq("anonymize-expired-accounts"),
                anyString(), any(Runnable.class));
        verify(service).anonymizeExpired(any(LocalDateTime.class), anyInt(), anyInt());
    }
}
