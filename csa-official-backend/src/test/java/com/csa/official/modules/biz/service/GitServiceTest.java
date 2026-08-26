package com.csa.official.modules.biz.service;

import com.csa.official.common.exception.CsaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitServiceTest {

    @TempDir
    Path repositoryRoot;

    private GitService gitService;

    @BeforeEach
    void setUp() {
        gitService = new GitService(repositoryRoot.toString(), "github.com,gitee.com,gitlab.com");
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
}
