package com.csa.official.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Profile({"dev", "test"})
@ConditionalOnProperty(name = "csa.seed.enabled", havingValue = "true")
public class DevSeedDataInitializer implements ApplicationRunner {

    private final DataSource dataSource;
    private final Resource seedResource;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DevSeedDataInitializer(
            DataSource dataSource,
            PasswordEncoder passwordEncoder,
            @Value("${csa.seed.location:file:../db/seed.sql}") Resource seedResource,
            @Value("${csa.seed.password:}") String demoPassword) {
        this.dataSource = dataSource;
        this.passwordEncoder = passwordEncoder;
        this.seedResource = seedResource;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!seedResource.exists()) {
            throw new IllegalStateException("Configured demo seed file does not exist: " + seedResource);
        }
        if (!StringUtils.hasText(demoPassword) || demoPassword.length() < 12) {
            throw new IllegalStateException(
                    "DEMO_SEED_PASSWORD must be set to at least 12 characters when demo seed is enabled");
        }

        String seedSql = seedResource.getContentAsString(StandardCharsets.UTF_8);
        if (!seedSql.contains("__DEMO_PASSWORD_HASH__")) {
            throw new IllegalStateException("Demo seed password placeholder is missing");
        }
        String materializedSql = seedSql.replace("__DEMO_PASSWORD_HASH__", passwordEncoder.encode(demoPassword));
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ByteArrayResource(materializedSql.getBytes(StandardCharsets.UTF_8)));
        populator.setContinueOnError(false);
        populator.execute(dataSource);
    }
}
