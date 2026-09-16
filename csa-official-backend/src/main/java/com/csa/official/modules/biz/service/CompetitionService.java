package com.csa.official.modules.biz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.common.cache.CacheKeys;
import com.csa.official.common.cache.CacheTtlPolicy;
import com.csa.official.common.cache.CacheValueCodec;
import com.csa.official.common.cache.CachedCompetitionPage;
import com.csa.official.common.cache.KeyValueStore;
import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.util.PageUtils;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.common.constant.RoleConsts;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.biz.entity.CompEditor;
import com.csa.official.modules.biz.entity.Competition;
import com.csa.official.modules.biz.enums.CompetitionStatusEnum;
import com.csa.official.modules.biz.mapper.CompEditorMapper;
import com.csa.official.modules.biz.mapper.CompetitionMapper;
import com.csa.official.modules.biz.vo.CompetitionDetailVO;
import com.csa.official.modules.biz.vo.CompetitionListVO;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.service.AuditService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CompetitionService {

    /** 列表摘要保留的纯文本长度，略大于前端展示的 120 字，留一点余量。 */
    static final int SUMMARY_MAX_LENGTH = 200;
    private static final TypeReference<CachedCompetitionPage> COMPETITION_PAGE_TYPE =
            new TypeReference<>() {
            };
    private static final long DETAIL_LOCK_SECONDS = 4;
    private static final int DETAIL_LOCK_RETRIES = 6;
    private static final long DETAIL_LOCK_WAIT_MILLIS = 50;

    private final CompetitionMapper competitionMapper;
    private final CompEditorMapper editorMapper;
    private final UserMapper userMapper;
    private final AuditService auditService;
    private final KeyValueStore keyValueStore;
    private final CacheValueCodec cacheValueCodec;
    private final CacheTtlPolicy cacheTtlPolicy;

    private volatile long lastCacheWarningAt;

    public CompetitionService(
            CompetitionMapper competitionMapper,
            CompEditorMapper editorMapper,
            UserMapper userMapper,
            AuditService auditService,
            KeyValueStore keyValueStore,
            CacheValueCodec cacheValueCodec,
            CacheTtlPolicy cacheTtlPolicy) {
        this.competitionMapper = competitionMapper;
        this.editorMapper = editorMapper;
        this.userMapper = userMapper;
        this.auditService = auditService;
        this.keyValueStore = keyValueStore;
        this.cacheValueCodec = cacheValueCodec;
        this.cacheTtlPolicy = cacheTtlPolicy;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveCompetition(Competition comp, Long userId) {
        if (comp.getStartTime() != null && comp.getEndTime() != null
                && comp.getEndTime().isBefore(comp.getStartTime())) {
            throw new CsaException(HttpStatus.BAD_REQUEST.value(), "Competition end time cannot be earlier than start time");
        }

        if (comp.getContent() != null) {
            String safeContent = Jsoup.clean(comp.getContent(), Safelist.basicWithImages());
            comp.setContent(safeContent);
        }

        if (comp.getId() == null) {
            comp.setPublisherId(userId);
            competitionMapper.insert(comp);
        } else {
            int rows = competitionMapper.updateById(comp);
            if (rows <= 0) {
                throw new CsaException(HttpStatus.NOT_FOUND.value(), "Competition not found");
            }
        }

        registerCacheInvalidation(comp.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void addEditor(Long compId, Long targetUserId, Long operatorId) {
        Competition competition = competitionMapper.selectById(compId);
        if (competition == null) {
            throw new CsaException(HttpStatus.NOT_FOUND.value(), "Competition not found");
        }

        if (!canGrantEditor(competition, operatorId)) {
            throw new CsaException(HttpStatus.FORBIDDEN.value(), "You are not allowed to grant editors for this competition");
        }

        if (userMapper.selectById(targetUserId) == null) {
            throw new CsaException(HttpStatus.NOT_FOUND.value(), "Target user does not exist");
        }

        if (editorMapper.exists(new LambdaQueryWrapper<CompEditor>()
                .eq(CompEditor::getCompetitionId, compId)
                .eq(CompEditor::getUserId, targetUserId))) {
            auditService.recordBestEffort("COMPETITION_EDITOR_GRANT", "COMPETITION", String.valueOf(compId),
                    "NO_CHANGE", null, Map.of("editorUserId", targetUserId));
            return;
        }

        CompEditor editor = new CompEditor();
        editor.setCompetitionId(compId);
        editor.setUserId(targetUserId);
        editorMapper.insert(editor);
        auditService.recordBestEffort("COMPETITION_EDITOR_GRANT", "COMPETITION", String.valueOf(compId),
                "SUCCESS", null, Map.of("editorUserId", targetUserId));
    }

    public Page<CompetitionListVO> getCompetitionPage(Integer page, Integer size) {
        Page<Competition> pageParam = PageUtils.of(page, size);
        Page<Competition> competitionPage = competitionMapper.selectPage(
                pageParam,
                new LambdaQueryWrapper<Competition>()
                        .orderByDesc(Competition::getUpdateTime)
                        .orderByDesc(Competition::getCreateTime));

        User currentUser = getCurrentUserSafely();
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        Integer currentRoleLevel = currentUser != null ? currentUser.getRoleLevel() : null;
        Set<Long> editableCompetitionIds = getEditableCompetitionIds(
                competitionPage.getRecords(), currentUserId, currentRoleLevel);

        Page<CompetitionListVO> result = new Page<>(
                competitionPage.getCurrent(),
                competitionPage.getSize(),
                competitionPage.getTotal());
        result.setRecords(competitionPage.getRecords().stream()
                .map(competition -> toCompetitionListVO(
                        competition,
                        editableCompetitionIds.contains(competition.getId()),
                        currentUserId,
                        currentRoleLevel))
                .collect(Collectors.toList()));
        return result;
    }

    public Page<CompetitionListVO> getPublicCompetitionPage(Integer page, Integer size) {
        Page<Competition> pageParam = PageUtils.of(page, size);
        long generation = readCompetitionListGeneration();
        String cacheKey = CacheKeys.competitionList(
                Math.toIntExact(pageParam.getCurrent()),
                Math.toIntExact(pageParam.getSize()),
                generation);

        CachedCompetitionPage cached = readCache(cacheKey, COMPETITION_PAGE_TYPE);
        if (cached != null) {
            return toCompetitionPage(cached);
        }

        Page<Competition> competitionPage = competitionMapper.selectPage(
                pageParam,
                new LambdaQueryWrapper<Competition>()
                        .ne(Competition::getStatus, CompetitionStatusEnum.UNPUBLISHED)
                        .orderByDesc(Competition::getUpdateTime)
                        .orderByDesc(Competition::getCreateTime));

        Page<CompetitionListVO> result = new Page<>(
                competitionPage.getCurrent(),
                competitionPage.getSize(),
                competitionPage.getTotal());
        result.setRecords(competitionPage.getRecords().stream()
                .map(competition -> toCompetitionListVO(competition, false, null, null))
                .collect(Collectors.toList()));
        writeCache(cacheKey,
                new CachedCompetitionPage(
                        result.getCurrent(),
                        result.getSize(),
                        result.getTotal(),
                        result.getRecords()),
                cacheTtlPolicy.competitionListSeconds());
        return result;
    }

    private boolean canGrantEditor(Competition competition, Long operatorId) {
        if (competition == null || operatorId == null) {
            return false;
        }

        if (competition.getPublisherId() != null && competition.getPublisherId().equals(operatorId)) {
            return true;
        }

        User operator = userMapper.selectById(operatorId);
        return operator != null
                && operator.getRoleLevel() != null
                && operator.getRoleLevel() >= RoleConsts.PRESIDENT;
    }

    /**
     * 按 id 查竞赛详情（后台入口）。返回完整正文，并带上当前用户的操作权限标记。
     */
    public CompetitionDetailVO getCompetitionDetail(Long id) {
        Competition competition = requireCompetition(id);

        User currentUser = getCurrentUserSafely();
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        Integer currentRoleLevel = currentUser != null ? currentUser.getRoleLevel() : null;
        boolean canEdit = !getEditableCompetitionIds(
                List.of(competition), currentUserId, currentRoleLevel).isEmpty();

        return toCompetitionDetailVO(competition, canEdit, currentUserId, currentRoleLevel);
    }

    /**
     * 按 id 查竞赛详情（公开入口）。未发布的竞赛对外一律当作不存在，
     * 避免通过详情接口探测到还没公开的活动。
     */
    public CompetitionDetailVO getPublicCompetitionDetail(Long id) {
        requireValidPublicId(id);

        CompetitionDetailVO cached = readCache(CacheKeys.competitionDetail(id), CompetitionDetailVO.class);
        if (cached != null) {
            return cached;
        }
        if (hasCacheMarker(CacheKeys.competitionDetailNull(id))) {
            throw notFound();
        }

        String lockKey = CacheKeys.competitionDetailLock(id);
        String lockToken = UUID.randomUUID().toString();
        LockAttempt lockAttempt = tryAcquireLock(lockKey, lockToken);
        if (lockAttempt == LockAttempt.ACQUIRED) {
            try {
                // Another request may have rebuilt the value between the first
                // miss and SETNX; never query MySQL before this second check.
                CompetitionDetailVO secondCheck = readCache(CacheKeys.competitionDetail(id),
                        CompetitionDetailVO.class);
                if (secondCheck != null) {
                    return secondCheck;
                }
                if (hasCacheMarker(CacheKeys.competitionDetailNull(id))) {
                    throw notFound();
                }
                return loadAndCachePublicDetail(id);
            } finally {
                releaseLock(lockKey, lockToken);
            }
        }

        if (lockAttempt == LockAttempt.UNAVAILABLE) {
            return loadAndCachePublicDetail(id);
        }

        for (int attempt = 0; attempt < DETAIL_LOCK_RETRIES; attempt++) {
            sleepBeforeRetry();
            CompetitionDetailVO rebuilt = readCache(CacheKeys.competitionDetail(id),
                    CompetitionDetailVO.class);
            if (rebuilt != null) {
                return rebuilt;
            }
            if (hasCacheMarker(CacheKeys.competitionDetailNull(id))) {
                throw notFound();
            }
        }

        throw new CsaException(ApiErrorCode.SERVICE_UNAVAILABLE,
                "Competition detail is being rebuilt; please retry");
    }

    private Competition requireCompetition(Long id) {
        if (id == null) {
            throw new CsaException(HttpStatus.BAD_REQUEST.value(), "Competition id is required");
        }
        Competition competition = competitionMapper.selectById(id);
        if (competition == null) {
            throw new CsaException(HttpStatus.NOT_FOUND.value(), "Competition not found");
        }
        return competition;
    }

    private CompetitionDetailVO loadAndCachePublicDetail(Long id) {
        Competition competition = competitionMapper.selectById(id);
        if (competition == null || competition.getStatus() == CompetitionStatusEnum.UNPUBLISHED) {
            writeCache(CacheKeys.competitionDetailNull(id),
                    CacheValueCodec.NULL_MARKER,
                    cacheTtlPolicy.competitionNullSeconds());
            throw notFound();
        }

        CompetitionDetailVO detail = toCompetitionDetailVO(competition, false, null, null);
        writeCache(CacheKeys.competitionDetail(id), detail, cacheTtlPolicy.competitionDetailSeconds());
        return detail;
    }

    private void requireValidPublicId(Long id) {
        if (id == null || id <= 0) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "Competition id must be positive");
        }
    }

    private CsaException notFound() {
        return new CsaException(ApiErrorCode.RESOURCE_NOT_FOUND, "Competition not found");
    }

    private LockAttempt tryAcquireLock(String lockKey, String lockToken) {
        try {
            return keyValueStore.setIfAbsent(lockKey, lockToken, DETAIL_LOCK_SECONDS, TimeUnit.SECONDS)
                    ? LockAttempt.ACQUIRED
                    : LockAttempt.BUSY;
        } catch (RuntimeException e) {
            warnCacheUnavailable("lock acquisition", e);
            // Redis failure must not make a public read impossible. The
            // bounded cache path falls back to MySQL without pretending a hit.
            return LockAttempt.UNAVAILABLE;
        }
    }

    private void releaseLock(String lockKey, String lockToken) {
        try {
            keyValueStore.deleteIfValue(lockKey, lockToken);
        } catch (RuntimeException e) {
            warnCacheUnavailable("lock release", e);
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(DETAIL_LOCK_WAIT_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CsaException(ApiErrorCode.SERVICE_UNAVAILABLE,
                    "Competition detail rebuild was interrupted");
        }
    }

    private long readCompetitionListGeneration() {
        try {
            String value = keyValueStore.getString(CacheKeys.COMPETITION_LIST_GENERATION);
            if (value == null) {
                keyValueStore.setIfAbsent(
                        CacheKeys.COMPETITION_LIST_GENERATION,
                        "0",
                        365,
                        TimeUnit.DAYS);
                return 0;
            }
            return Long.parseLong(value);
        } catch (RuntimeException e) {
            warnCacheUnavailable("list generation read", e);
            return 0;
        }
    }

    private <T> T readCache(String key, Class<T> type) {
        try {
            String value = keyValueStore.getString(key);
            if (value == null || CacheValueCodec.NULL_MARKER.equals(value)) {
                return null;
            }
            return cacheValueCodec.read(value, type);
        } catch (RuntimeException e) {
            warnCacheUnavailable("cache read for " + key, e);
            return null;
        }
    }

    private <T> T readCache(String key, TypeReference<T> type) {
        try {
            String value = keyValueStore.getString(key);
            if (value == null || CacheValueCodec.NULL_MARKER.equals(value)) {
                return null;
            }
            return cacheValueCodec.read(value, type);
        } catch (RuntimeException e) {
            warnCacheUnavailable("cache read for " + key, e);
            return null;
        }
    }

    private boolean hasCacheMarker(String key) {
        try {
            return CacheValueCodec.NULL_MARKER.equals(keyValueStore.getString(key));
        } catch (RuntimeException e) {
            warnCacheUnavailable("null cache read for " + key, e);
            return false;
        }
    }

    private void writeCache(String key, Object value, long ttlSeconds) {
        try {
            String encoded = value instanceof String stringValue
                    ? stringValue
                    : cacheValueCodec.write(value);
            keyValueStore.setString(key, encoded, ttlSeconds, TimeUnit.SECONDS);
        } catch (RuntimeException e) {
            warnCacheUnavailable("cache write for " + key, e);
        }
    }

    private void invalidateCompetitionCaches(Long competitionId) {
        try {
            keyValueStore.increment(CacheKeys.COMPETITION_LIST_GENERATION);
            if (competitionId != null) {
                keyValueStore.delete(CacheKeys.competitionDetail(competitionId));
                keyValueStore.delete(CacheKeys.competitionDetailNull(competitionId));
            }
            auditService.recordBestEffort(
                    "CACHE_INVALIDATION",
                    "COMPETITION",
                    competitionId == null ? null : String.valueOf(competitionId),
                    "SUCCESS",
                    null,
                    Map.of("cacheVersion", CacheKeys.VERSION, "scope", "public-competition"));
        } catch (RuntimeException e) {
            warnCacheUnavailable("cache invalidation", e);
            auditService.recordBestEffort(
                    "CACHE_INVALIDATION",
                    "COMPETITION",
                    competitionId == null ? null : String.valueOf(competitionId),
                    "FAILED",
                    null,
                    Map.of("cacheVersion", CacheKeys.VERSION, "scope", "public-competition"));
        }
    }

    private void registerCacheInvalidation(Long competitionId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    invalidateCompetitionCaches(competitionId);
                }
            });
        } else {
            invalidateCompetitionCaches(competitionId);
        }
    }

    private Page<CompetitionListVO> toCompetitionPage(CachedCompetitionPage cached) {
        Page<CompetitionListVO> page = new Page<>(cached.current(), cached.size(), cached.total());
        page.setRecords(cached.records() == null ? List.of() : cached.records());
        return page;
    }

    private synchronized void warnCacheUnavailable(String operation, RuntimeException exception) {
        long now = System.currentTimeMillis();
        if (now - lastCacheWarningAt < 60_000) {
            return;
        }
        lastCacheWarningAt = now;
        log.warn("Competition cache unavailable during {}; falling back to database", operation, exception);
    }

    private enum LockAttempt {
        ACQUIRED,
        BUSY,
        UNAVAILABLE
    }

    /**
     * 把富文本正文压成纯文本摘要。
     *
     * <p>用 Jsoup 把标签剥掉再截断，而不是直接 {@code substring}：
     * 否则会从 HTML 中间切断，前端拿到半个标签。
     */
    static String buildSummary(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String plainText = Jsoup.parse(content).text().trim();
        if (plainText.length() <= SUMMARY_MAX_LENGTH) {
            return plainText;
        }
        return plainText.substring(0, SUMMARY_MAX_LENGTH) + "…";
    }

    private CompetitionDetailVO toCompetitionDetailVO(
            Competition competition,
            boolean canEdit,
            Long currentUserId,
            Integer currentRoleLevel) {
        CompetitionDetailVO vo = new CompetitionDetailVO();
        vo.setId(competition.getId());
        vo.setTitle(competition.getTitle());
        vo.setContent(competition.getContent());
        vo.setCoverImg(competition.getCoverImg());
        vo.setStartTime(competition.getStartTime());
        vo.setEndTime(competition.getEndTime());
        vo.setPublisherId(competition.getPublisherId());
        vo.setStatus(competition.getStatus() == null ? null : competition.getStatus().getCode());
        vo.setCreateTime(competition.getCreateTime());
        vo.setUpdateTime(competition.getUpdateTime());
        vo.setCanEdit(canEdit);
        vo.setCanGrant(canGrantEditor(competition, currentUserId, currentRoleLevel));
        return vo;
    }

    private CompetitionListVO toCompetitionListVO(
            Competition competition,
            boolean canEdit,
            Long currentUserId,
            Integer currentRoleLevel) {
        CompetitionListVO vo = new CompetitionListVO();
        vo.setId(competition.getId());
        vo.setTitle(competition.getTitle());
        vo.setSummary(buildSummary(competition.getContent()));
        vo.setCoverImg(competition.getCoverImg());
        vo.setStartTime(competition.getStartTime());
        vo.setEndTime(competition.getEndTime());
        vo.setPublisherId(competition.getPublisherId());
        vo.setStatus(competition.getStatus() == null ? null : competition.getStatus().getCode());
        vo.setCreateTime(competition.getCreateTime());
        vo.setUpdateTime(competition.getUpdateTime());
        vo.setCanEdit(canEdit);
        vo.setCanGrant(canGrantEditor(competition, currentUserId, currentRoleLevel));
        return vo;
    }

    private Set<Long> getEditableCompetitionIds(
            List<Competition> competitions,
            Long currentUserId,
            Integer currentRoleLevel) {
        if (competitions.isEmpty() || currentUserId == null) {
            return Collections.emptySet();
        }

        if (currentRoleLevel != null && currentRoleLevel >= RoleConsts.PRESIDENT) {
            return competitions.stream().map(Competition::getId).collect(Collectors.toSet());
        }

        Set<Long> editableIds = competitions.stream()
                .filter(competition -> currentUserId.equals(competition.getPublisherId()))
                .map(Competition::getId)
                .collect(Collectors.toCollection(HashSet::new));

        List<Long> competitionIds = competitions.stream().map(Competition::getId).toList();
        editorMapper.selectList(new LambdaQueryWrapper<CompEditor>()
                        .select(CompEditor::getCompetitionId)
                        .in(CompEditor::getCompetitionId, competitionIds)
                        .eq(CompEditor::getUserId, currentUserId))
                .stream()
                .map(CompEditor::getCompetitionId)
                .forEach(editableIds::add);

        return editableIds;
    }

    private boolean canGrantEditor(Competition competition, Long currentUserId, Integer currentRoleLevel) {
        if (competition == null) {
            return false;
        }

        if (currentRoleLevel != null && currentRoleLevel >= RoleConsts.PRESIDENT) {
            return true;
        }

        return currentUserId != null
                && competition.getPublisherId() != null
                && competition.getPublisherId().equals(currentUserId);
    }

    private User getCurrentUserSafely() {
        try {
            return SecurityUtils.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }
}
