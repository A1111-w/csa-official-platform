package com.csa.official.modules.biz.service;

import com.csa.official.common.exception.CsaException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitServiceTest {

    @TempDir
    Path repositoryRoot;

    private GitService gitService;

    @BeforeEach
    void setUp() {
        gitService = new GitService(
                repositoryRoot.toString(), "github.com,gitee.com,gitlab.com", 30, 1024 * 1024);
    }

    @Test
    void acceptsHttpsRepositoryOnApprovedHost() {
        URI uri = ReflectionTestUtils.invokeMethod(
                gitService,
                "validateRepositoryUrl",
                "https://github.com/csa/example.git");

        assertThat(uri).isNotNull();
        assertThat(uri.getScheme()).isEqualTo("https");
        assertThat(uri.getHost()).isEqualTo("github.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://github.com.attacker.invalid/csa/example.git",
            "https://github.com@127.0.0.1/internal.git",
            "https://127.0.0.1/internal.git",
            "https://[::1]/internal.git",
            "https://user:password@github.com/csa/example.git",
            "https://github.com:8443/csa/example.git",
            "http://github.com/csa/example.git",
            "https://github.com/csa/example.git?access_token=hidden",
            "https://github.com/csa/example.git#fragment",
            "ssh://git@github.com/csa/example.git",
            "file:///etc/passwd",
            "not-a-url"
    })
    void rejectsRepositoryUrlsOutsideOutboundPolicy(String repoUrl) {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                gitService,
                "validateRepositoryUrl",
                repoUrl))
                .isInstanceOf(CsaException.class);
    }

    @Test
    void rejectsMissingRepositoryUrl() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                gitService,
                "validateRepositoryUrl",
                (String) null))
                .isInstanceOf(CsaException.class);
    }

    @Test
    void clonesRepositoryAndReturnsHeadMetadata() throws Exception {
        Path remote = createRepository("first", "hello");
        Path local = repositoryRoot.resolve("git-repos").resolve("101");

        GitService.GitSyncResult result = ReflectionTestUtils.invokeMethod(
                gitService, "syncLocked", 101L, remote.toUri(), local);

        assertThat(result).isNotNull();
        assertThat(result.branch()).isNotBlank();
        assertThat(result.commit()).hasSize(40);
        assertThat(result.sizeBytes()).isPositive();
        assertThat(local.resolve("README.md")).exists();
    }

    @Test
    void repositoryUrlChangeReplacesExistingClone() throws Exception {
        Path firstRemote = createRepository("first", "first repository");
        Path secondRemote = createRepository("second", "second repository");
        Path local = repositoryRoot.resolve("git-repos").resolve("102");

        GitService.GitSyncResult first = ReflectionTestUtils.invokeMethod(
                gitService, "syncLocked", 102L, firstRemote.toUri(), local);
        GitService.GitSyncResult second = ReflectionTestUtils.invokeMethod(
                gitService, "syncLocked", 102L, secondRemote.toUri(), local);

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(second.commit()).isNotEqualTo(first.commit());
        assertThat(Files.readString(local.resolve("README.md")))
                .isEqualTo("second repository");
        try (Git cloned = Git.open(local.toFile())) {
            assertThat(cloned.getRepository().getConfig()
                    .getString("remote", "origin", "url"))
                    .isEqualTo(secondRemote.toUri().toASCIIString());
        }
    }

    @Test
    void oversizedRepositoryIsRemoved() throws Exception {
        Path remote = createRepository("large", "content larger than one byte");
        Path local = repositoryRoot.resolve("git-repos").resolve("103");
        GitService limitedService = new GitService(
                repositoryRoot.toString(), "github.com", 30, 1);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                limitedService, "syncLocked", 103L, remote.toUri(), local))
                .isInstanceOf(CsaException.class)
                .extracting(error -> ((CsaException) error).getCode())
                .isEqualTo(413);

        assertThat(local).doesNotExist();
    }

    private Path createRepository(String directoryName, String content) throws Exception {
        Path path = repositoryRoot.resolve("remotes").resolve(directoryName);
        Files.createDirectories(path);
        try (Git git = Git.init().setDirectory(path.toFile()).call()) {
            Files.writeString(path.resolve("README.md"), content);
            git.add().addFilepattern("README.md").call();
            RevCommit commit = git.commit()
                    .setMessage("initial commit")
                    .setAuthor("CSA Test", "csa@example.invalid")
                    .setCommitter("CSA Test", "csa@example.invalid")
                    .call();
            assertThat(commit.getName()).hasSize(40);
        }
        return path;
    }
}
