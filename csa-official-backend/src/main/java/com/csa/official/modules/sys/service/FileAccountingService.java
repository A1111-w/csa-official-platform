package com.csa.official.modules.sys.service;

import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.sys.entity.StoredFile;
import com.csa.official.modules.sys.mapper.FileUsageMapper;
import com.csa.official.modules.sys.mapper.StoredFileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileAccountingService {

    static final String USER_SCOPE = "USER";
    static final String SCHOOL_SCOPE = "SCHOOL";
    static final long SCHOOL_SCOPE_ID = 0L;

    private final FileUsageMapper fileUsageMapper;
    private final StoredFileMapper storedFileMapper;

    public FileAccountingService(FileUsageMapper fileUsageMapper, StoredFileMapper storedFileMapper) {
        this.fileUsageMapper = fileUsageMapper;
        this.storedFileMapper = storedFileMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void reserveAndRecord(StoredFile metadata, long userQuotaBytes, long schoolQuotaBytes) {
        if (metadata == null || metadata.getOwnerUserId() == null
                || metadata.getSizeBytes() == null || metadata.getSizeBytes() <= 0) {
            throw new IllegalArgumentException("Valid file metadata is required");
        }

        long bytes = metadata.getSizeBytes();
        reserve(USER_SCOPE, metadata.getOwnerUserId(), bytes, userQuotaBytes, "已超过个人文件配额");
        reserve(SCHOOL_SCOPE, SCHOOL_SCOPE_ID, bytes, schoolQuotaBytes, "协会文件配额已满");

        if (storedFileMapper.insert(metadata) != 1) {
            throw new IllegalStateException("File metadata insert did not affect one row");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markDeletedAndRelease(StoredFile metadata) {
        if (metadata == null || metadata.getId() == null || metadata.getOwnerUserId() == null
                || metadata.getSizeBytes() == null || metadata.getSizeBytes() < 0) {
            throw new IllegalArgumentException("Valid stored file metadata is required");
        }
        if (storedFileMapper.markDeleted(metadata.getId()) != 1) {
            return false;
        }

        release(USER_SCOPE, metadata.getOwnerUserId(), metadata.getSizeBytes());
        release(SCHOOL_SCOPE, SCHOOL_SCOPE_ID, metadata.getSizeBytes());
        return true;
    }

    private void reserve(String scopeType, Long scopeId, long bytes, long quotaBytes, String message) {
        fileUsageMapper.ensureScope(scopeType, scopeId);
        if (fileUsageMapper.reserve(scopeType, scopeId, bytes, quotaBytes) != 1) {
            throw new CsaException(ApiErrorCode.PAYLOAD_TOO_LARGE, message);
        }
    }

    private void release(String scopeType, Long scopeId, long bytes) {
        if (fileUsageMapper.release(scopeType, scopeId, bytes) != 1) {
            throw new IllegalStateException("File usage counter is missing for " + scopeType);
        }
    }
}
