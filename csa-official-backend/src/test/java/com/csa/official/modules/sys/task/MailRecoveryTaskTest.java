package com.csa.official.modules.sys.task;

import com.csa.official.modules.sys.service.MailRecoveryService;
import com.csa.official.modules.sys.service.ScheduledJobService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MailRecoveryTaskTest {

    @Test
    void usesSharedScheduledJobGuard() {
        MailRecoveryService recoveryService = mock(MailRecoveryService.class);
        ScheduledJobService scheduledJobService = mock(ScheduledJobService.class);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return true;
        }).when(scheduledJobService).runWithLock(anyString(), any(Runnable.class));

        MailRecoveryTask task = new MailRecoveryTask(recoveryService, scheduledJobService, 100, 90);
        task.recoverStaleMail();

        verify(scheduledJobService).runWithLock(
                org.mockito.ArgumentMatchers.eq("recover-stale-mail"), any(Runnable.class));
        verify(recoveryService).recoverStale(any(), anyInt());
    }
}
