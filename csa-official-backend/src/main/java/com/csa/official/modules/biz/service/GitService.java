package com.csa.official.modules.biz.service;

import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.exception.CsaException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class GitService {

    private final Path repositoryRoot;
    private final Set<String> allowedHosts;
    private final int timeoutSeconds;
    private final long maxRepositorySizeBytes;
    private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    public GitService(
            @Value("${csa.upload-path}") String basePath,
            @Value("${csa.git.allowed-hosts:github.com,gitee.com,gitlab.com}") String allowedHostsConfig,
            @Value("${csa.git.timeout-seconds:30}") int configuredTimeoutSeconds,
            @Value("${csa.git.max-repository-size-bytes:209715200}") long configuredMaxRepositorySizeBytes) {
        this.repositoryRoot = Path.of(basePath).toAbsolutePath().normalize().resolve("git-repos");
        this.allowedHosts = Arrays.stream(allowedHostsConfig.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> IDN.toASCII(value).toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.timeoutSeconds = Math.max(5, Math.min(configuredTimeoutSeconds, 300));
        this.maxRepositorySizeBytes = Math.max(1L, configuredMaxRepositorySizeBytes);
    }

    public GitSyncResult syncRepository(Long userId, String repoUrl) {
        if (userId == null || userId <= 0) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "User id is required");
        }

        URI repositoryUri = validateRepositoryUrl(repoUrl);
        Path localPath = repositoryPath(userId);
        ReentrantLock lock = userLocks.computeIfAbsent(userId, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return syncLocked(userId, repositoryUri, localPath);
        } finally {
            lock.unlock();
        }
    }

    public URI validateRepositoryUrl(String repoUrl) {
        if (repoUrl == null || repoUrl.isBlank()) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "Repository URL is required");
        }

        final URI uri;
        try {
            uri = new URI(repoUrl.trim()).normalize();
        } catch (URISyntaxException e) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "Invalid repository URL");
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "Only HTTPS repository URLs are supported");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "Invalid repository URL");
        }
        if (uri.getPort() != -1 && uri.getPort() != 443) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "Repository URL port is not allowed");
        }

        String host = IDN.toASCII(uri.getHost()).toLowerCase(Locale.ROOT);
        boolean allowed = allowedHosts.stream()
                .anyMatch(value -> host.equals(value) || host.endsWith("." + value));
        if (!allowed) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "Repository host is not allowed");
        }
        return uri;
    }

    private GitSyncResult syncLocked(Long userId, URI repositoryUri, Path localPath) {
        try {
            if (isGitRepository(localPath)) {
                boolean sameRepository;
                Git git = Git.open(localPath.toFile());
                try {
                    String configuredUrl = git.getRepository().getConfig()
                            .getString("remote", "origin", "url");
                    sameRepository = repositoryUri.toASCIIString().equals(configuredUrl);
                    if (sameRepository) {
                        PullResult pull = git.pull()
                                .setTimeout(timeoutSeconds)
                                .call();
                        if (!pull.isSuccessful()) {
                            throw new CsaException(ApiErrorCode.UPSTREAM_ERROR,
                                    "Repository update could not be applied cleanly");
                        }
                    }
                } finally {
                    closeObjectDatabase(git);
                    git.close();
                }
                if (sameRepository) {
                    return inspectRepository(localPath);
                }
            }

            deleteDirectory(localPath);
            Files.createDirectories(repositoryRoot);
            log.info("Cloning repository from approved host {} for userId={}",
                    repositoryUri.getHost(), userId);
            Git cloned = Git.cloneRepository()
                    .setURI(repositoryUri.toASCIIString())
                    .setDirectory(localPath.toFile())
                    .setDepth(1)
                    .setTimeout(timeoutSeconds)
                    .call();
            try {
                // Clone command has completed; release all pack handles before
                // size enforcement or a future repository replacement.
            } finally {
                closeObjectDatabase(cloned);
                cloned.close();
            }
            try {
                return inspectRepository(localPath);
            } catch (IOException | RuntimeException e) {
                deleteAfterFailedClone(localPath, e);
                throw e;
            }
        } catch (CsaException e) {
            throw e;
        } catch (GitAPIException | IOException e) {
            log.error("Git synchronization failed for userId={}", userId, e);
            throw new CsaException(ApiErrorCode.UPSTREAM_ERROR,
                    "Repository synchronization failed", e);
        }
    }

    private GitSyncResult inspectRepository(Path localPath) throws IOException {
        long sizeBytes = directorySize(localPath);
        if (sizeBytes > maxRepositorySizeBytes) {
            deleteDirectory(localPath);
            throw new CsaException(413, "GIT_REPOSITORY_TOO_LARGE",
                    "Repository exceeds the configured size limit");
        }

        Git git = Git.open(localPath.toFile());
        try {
            String branch = git.getRepository().getBranch();
            ObjectId head = git.getRepository().resolve(Constants.HEAD);
            if (head == null) {
                throw new CsaException(ApiErrorCode.UPSTREAM_ERROR,
                        "Repository does not contain a commit");
            }
            return new GitSyncResult(branch, head.name(), sizeBytes);
        } finally {
            closeObjectDatabase(git);
            git.close();
        }
    }

    private void closeObjectDatabase(Git git) {
        git.getRepository().getObjectDatabase().close();
    }

    private Path repositoryPath(Long userId) {
        Path localPath = repositoryRoot.resolve(String.valueOf(userId)).normalize();
        if (!localPath.startsWith(repositoryRoot)) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "Invalid repository path");
        }
        return localPath;
    }

    private boolean isGitRepository(Path path) {
        return Files.isDirectory(path) && Files.isDirectory(path.resolve(Constants.DOT_GIT));
    }

    private long directorySize(Path path) throws IOException {
        if (!Files.exists(path)) {
            return 0L;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            return paths.filter(Files::isRegularFile)
                    .mapToLong(file -> {
                        try {
                            return Files.size(file);
                        } catch (IOException e) {
                            throw new DirectoryReadException(e);
                        }
                    })
                    .sum();
        } catch (DirectoryReadException e) {
            throw e.getCause();
        }
    }

    private void deleteAfterFailedClone(Path localPath, Throwable failure) {
        try {
            deleteDirectory(localPath);
        } catch (IOException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
            log.warn("Failed to clean incomplete Git repository at {}", localPath, cleanupFailure);
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(repositoryRoot) || normalized.equals(repositoryRoot)) {
            throw new IOException("Refusing to delete outside the Git repository root");
        }
        // JGit keeps pack windows globally cached; flush them before deletion so
        // Windows can release pack-file handles used by a completed clone/pull.
        new WindowCacheConfig().install();
        IOException finalFailure = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            try (Stream<Path> paths = Files.walk(normalized)) {
                for (Path candidate : paths.sorted(Comparator.reverseOrder()).toList()) {
                    candidate.toFile().setWritable(true);
                    Files.deleteIfExists(candidate);
                }
                return;
            } catch (IOException e) {
                finalFailure = e;
                if (attempt == 9) {
                    break;
                }
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    e.addSuppressed(interrupted);
                    throw e;
                }
            }
        }
        throw finalFailure;
    }

    public record GitSyncResult(String branch, String commit, long sizeBytes) {
    }

    private static final class DirectoryReadException extends RuntimeException {
        private DirectoryReadException(IOException cause) {
            super(cause);
        }

        @Override
        public synchronized IOException getCause() {
            return (IOException) super.getCause();
        }
    }
}
