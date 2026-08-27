package com.csa.official;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** 守护 dev/test seed 不把共享口令或可直接使用的固定密码哈希提交进仓库。 */
class SeedDataPasswordTest {

    @Test
    void seedUsesRuntimePasswordPlaceholderInsteadOfCommittedCredential() throws IOException {
        Path seedFile = Path.of("..", "db", "seed.sql").normalize();
        assertThat(seedFile)
                .as("db/seed.sql 应该存在；它只允许由 dev/test initializer 执行")
                .exists();

        String sql = Files.readString(seedFile, StandardCharsets.UTF_8);
        assertThat(sql).contains("__DEMO_PASSWORD_HASH__");
        assertThat(sql).doesNotContainPattern("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");
        assertThat(sql).doesNotContain("Csa@12345");
    }
}
