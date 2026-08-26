package com.csa.official.modules.biz.service;

import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.exception.CsaException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GitService {

    private final String basePath;
    private final Set<String> allowedHosts;

    public GitService(
            @Value("${csa.upload-path}") String basePath,
            @Value("${csa.git.allowed-hosts:github.com,gitee.com,gitlab.com}") String allowedHostsConfig) {
        this.basePath = basePath;
        this.allowedHosts = Arrays.stream(allowedHostsConfig.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> IDN.toASCII(value).toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public void syncRepository(Long userId, String repoUrl) {
        URI repositoryUri = validateRepositoryUrl(repoUrl);
        File localPath = new File(basePath + File.separator + "git-repos" + File.separator + userId);

        try {
            if (localPath.exists() && new File(localPath, ".git").exists()) {
                try (Git git = Git.open(localPath)) {
                    git.pull().call();
                }
                return;
            }

            if (localPath.exists()) {
                deleteDir(localPath);
            }
            if (!localPath.mkdirs() && !localPath.isDirectory()) {
                throw new IOException("Unable to create repository directory");
            }

            log.info("Cloning repository from approved host {}", repositoryUri.getHost());
            Git.cloneRepository()
                    .setURI(repositoryUri.toASCIIString())
                    .setDirectory(localPath)
                    .setDepth(1)
                    .call()
                    .close();
        } catch (GitAPIException | IOException e) {
            log.error("Git synchronization failed for userId={}", userId, e);
            throw new CsaException(ApiErrorCode.UPSTREAM_ERROR, "Repository synchronization failed", e);
        }
    }

    private URI validateRepositoryUrl(String repoUrl) {
        if (repoUrl == null || repoUrl.isBlank()) {
            throw new CsaException(400, "Repository URL is required");
        }

        final URI uri;
        try {
            uri = new URI(repoUrl.trim());
        } catch (URISyntaxException e) {
            throw new CsaException(400, "Invalid repository URL");
        }

        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new CsaException(400, "Only HTTP/HTTPS repository URLs are supported");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null) {
            throw new CsaException(400, "Invalid repository URL");
        }

        int port = uri.getPort();
        if (port != -1 && port != 80 && port != 443) {
            throw new CsaException(400, "Repository URL port is not allowed");
        }

        String host = IDN.toASCII(uri.getHost()).toLowerCase(Locale.ROOT);
        boolean allowed = allowedHosts.stream()
                .anyMatch(value -> host.equals(value) || host.endsWith("." + value));
        if (!allowed) {
            throw new CsaException(400, "Repository host is not allowed");
        }
        return uri;
    }

    private void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File child : contents) {
                deleteDir(child);
            }
        }
        if (!file.delete() && file.exists()) {
            log.warn("Unable to delete {}", file.getAbsolutePath());
        }
    }
}
