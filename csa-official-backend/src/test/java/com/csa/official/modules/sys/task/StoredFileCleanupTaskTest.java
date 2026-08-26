package com.csa.official.modules.sys.task;

import com.csa.official.modules.sys.entity.StoredFile;
import com.csa.official.modules.sys.mapper.StoredFileMapper;
import com.csa.official.modules.sys.service.FileAccountingService;
import com.csa.official.modules.sys.service.ScheduledJobService;
import com.csa.official.modules.sys.storage.FileStorage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoredFileCleanupTaskTest {

    @Test
    void deletesOnlyFilesSelectedAsUnreferencedByMapper() throws Exception {
        StoredFileMapper mapper = mock(StoredFileMapper.class);
        FileStorage storage = mock(FileStorage.class);
        FileAccountingService accountingService = mock(FileAccountingService.class);
        ScheduledJobService scheduledJobService = mock(ScheduledJobService.class);
        StoredFile orphan = new StoredFile();
        orphan.setId(7L);
        orphan.setStorageKey("/files/42/orphan.png");
        when(mapper.selectOrphans(any(), anyInt())).thenReturn(List.of(orphan));
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return true;
        }).when(scheduledJobService).runOnce(anyString(), anyString(), any(Runnable.class));

        StoredFileCleanupTask task = new StoredFileCleanupTask(
                mapper, storage, accountingService, scheduledJobService, 10, 1);

        task.cleanup();

        verify(storage).delete("/files/42/orphan.png");
        verify(accountingService).markDeletedAndRelease(orphan);
    }
}
