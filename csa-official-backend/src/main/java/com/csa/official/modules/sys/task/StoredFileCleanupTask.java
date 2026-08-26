package com.csa.official.modules.sys.task;

import com.csa.official.modules.sys.entity.StoredFile;
import com.csa.official.modules.sys.mapper.StoredFileMapper;
import com.csa.official.modules.sys.service.ScheduledJobService;
import com.csa.official.modules.sys.storage.FileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class StoredFileCleanupTask {

    private static final String JOB_NAME = "cleanup-orphan-files";

    private final StoredFileMapper storedFileMapper;
    private final FileStorage fileStorage;
    private final ScheduledJobService scheduledJobService;
    private final int batchSize;
    private final long graceHours;

    public StoredFileCleanupTask(StoredFileMapper storedFileMapper, FileStorage fileStorage,
                                 ScheduledJobService scheduledJobService,
                                 @Value("${csa.upload.cleanup-batch-size:100}") int batchSize,
                                 @Value("${csa.upload.orphan-grace-hours:24}") long graceHours) {
        this.storedFileMapper = storedFileMapper;
        this.fileStorage = fileStorage;
        this.scheduledJobService = scheduledJobService;
        this.batchSize = Math.max(1, Math.min(batchSize, 1000));
        this.graceHours = Math.max(1, graceHours);
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanup() {
        String key = LocalDateTime.now().toLocalDate().toString();
        scheduledJobService.runOnce(JOB_NAME, key, () -> {
            LocalDateTime before = LocalDateTime.now().minusHours(graceHours);
            List<StoredFile> orphans = storedFileMapper.selectOrphans(before, batchSize);
            for (StoredFile orphan : orphans) {
                try {
                    fileStorage.delete(orphan.getStorageKey());
                } catch (IOException e) {
                    log.warn("Could not delete orphan file metadataId={}", orphan.getId(), e);
                    continue;
                }
                storedFileMapper.markDeleted(orphan.getId());
            }
            log.info("Orphan file cleanup completed: candidates={}", orphans.size());
        });
    }
}
