package com.csa.official.modules.sys.service;

import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.sys.entity.StoredFile;
import com.csa.official.modules.sys.mapper.FileUsageMapper;
import com.csa.official.modules.sys.mapper.StoredFileMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileAccountingServiceTest {

    @Test
    void reservesUserAndSchoolBeforeRecordingMetadata() {
        FileUsageMapper usageMapper = mock(FileUsageMapper.class);
        StoredFileMapper storedFileMapper = mock(StoredFileMapper.class);
        StoredFile metadata = metadata(7L, 42L, 128L);
        when(usageMapper.reserve(FileAccountingService.USER_SCOPE, 42L, 128L, 1024L)).thenReturn(1);
        when(usageMapper.reserve(FileAccountingService.SCHOOL_SCOPE,
                FileAccountingService.SCHOOL_SCOPE_ID, 128L, 4096L)).thenReturn(1);
        when(storedFileMapper.insert(metadata)).thenReturn(1);

        FileAccountingService service = new FileAccountingService(usageMapper, storedFileMapper);
        service.reserveAndRecord(metadata, 1024L, 4096L);

        var order = inOrder(usageMapper, storedFileMapper);
        order.verify(usageMapper).ensureScope(FileAccountingService.USER_SCOPE, 42L);
        order.verify(usageMapper).reserve(FileAccountingService.USER_SCOPE, 42L, 128L, 1024L);
        order.verify(usageMapper).ensureScope(
                FileAccountingService.SCHOOL_SCOPE, FileAccountingService.SCHOOL_SCOPE_ID);
        order.verify(usageMapper).reserve(FileAccountingService.SCHOOL_SCOPE,
                FileAccountingService.SCHOOL_SCOPE_ID, 128L, 4096L);
        order.verify(storedFileMapper).insert(metadata);
    }

    @Test
    void rejectsWhenAtomicUserReservationFails() {
        FileUsageMapper usageMapper = mock(FileUsageMapper.class);
        StoredFileMapper storedFileMapper = mock(StoredFileMapper.class);
        StoredFile metadata = metadata(7L, 42L, 128L);
        when(usageMapper.reserve(FileAccountingService.USER_SCOPE, 42L, 128L, 100L)).thenReturn(0);

        FileAccountingService service = new FileAccountingService(usageMapper, storedFileMapper);

        assertThatThrownBy(() -> service.reserveAndRecord(metadata, 100L, 4096L))
                .isInstanceOf(CsaException.class)
                .satisfies(error -> assertThat(((CsaException) error).getCode()).isEqualTo(413));
        verify(storedFileMapper, never()).insert(metadata);
    }

    @Test
    void releasesBothCountersOnlyAfterActiveMetadataIsDeleted() {
        FileUsageMapper usageMapper = mock(FileUsageMapper.class);
        StoredFileMapper storedFileMapper = mock(StoredFileMapper.class);
        StoredFile metadata = metadata(7L, 42L, 128L);
        when(storedFileMapper.markDeleted(7L)).thenReturn(1);
        when(usageMapper.release(FileAccountingService.USER_SCOPE, 42L, 128L)).thenReturn(1);
        when(usageMapper.release(FileAccountingService.SCHOOL_SCOPE,
                FileAccountingService.SCHOOL_SCOPE_ID, 128L)).thenReturn(1);

        FileAccountingService service = new FileAccountingService(usageMapper, storedFileMapper);

        assertThat(service.markDeletedAndRelease(metadata)).isTrue();
        verify(usageMapper).release(FileAccountingService.USER_SCOPE, 42L, 128L);
        verify(usageMapper).release(
                FileAccountingService.SCHOOL_SCOPE, FileAccountingService.SCHOOL_SCOPE_ID, 128L);
    }

    private StoredFile metadata(Long id, Long ownerId, Long sizeBytes) {
        StoredFile metadata = new StoredFile();
        metadata.setId(id);
        metadata.setOwnerUserId(ownerId);
        metadata.setSizeBytes(sizeBytes);
        return metadata;
    }
}
