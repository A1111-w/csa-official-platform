package com.csa.official.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Run with `mvn -Dit.containers=true test` when Docker is available.
 * The ordinary unit-test command skips this test so local work does not require Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
@EnabledIfSystemProperty(named = "it.containers", matches = "true")
class FlywayMySqlRedisIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("csa_integration")
            .withUsername("csa_integration")
            .withPassword(UUID.randomUUID().toString());

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    @Test
    void initializesEmptyMySqlUpgradesV1DataAndRoundTripsRedis() throws Exception {
        Flyway v1 = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("1"))
                .cleanDisabled(true)
                .load();

        v1.migrate();

        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            try (ResultSet users = statement.executeQuery("SELECT COUNT(*) FROM sys_user")) {
                assertThat(users.next()).isTrue();
                assertThat(users.getInt(1)).isZero();
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO sys_user
                        (username, password, email, student_id, role_level, position_type, balance, deleted)
                    VALUES (?, ?, ?, ?, 0, 0, 0.00, 0)
                    """)) {
                insert.setString(1, "legacy_student");
                insert.setString(2, "test-only-password-hash");
                insert.setString(3, " Legacy.Student@Example.Invalid ");
                insert.setString(4, " 20260001 ");
                assertThat(insert.executeUpdate()).isEqualTo(1);
            }
        }

        Flyway latest = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load();
        latest.migrate();

        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            try (ResultSet tables = statement.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables "
                            + "WHERE table_schema = DATABASE() AND table_name = 'sys_user'")) {
                assertThat(tables.next()).isTrue();
                assertThat(tables.getInt(1)).isEqualTo(1);
            }
            try (ResultSet history = statement.executeQuery(
                    "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1")) {
                assertThat(history.next()).isTrue();
                assertThat(history.getString(1)).isEqualTo("5");
            }
            try (ResultSet reviewIndex = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = 'biz_resume'
                      AND index_name = 'idx_resume_status_update'
                    """)) {
                assertThat(reviewIndex.next()).isTrue();
                assertThat(reviewIndex.getInt(1)).isEqualTo(2);
            }
            try (ResultSet fileUsageTable = statement.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables "
                            + "WHERE table_schema = DATABASE() AND table_name = 'sys_file_usage'")) {
                assertThat(fileUsageTable.next()).isTrue();
                assertThat(fileUsageTable.getInt(1)).isEqualTo(1);
            }
            try (ResultSet contributionColumns = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                      AND table_name = 'sys_contribution_log'
                      AND column_name IN ('source', 'awarded_by')
                    """)) {
                assertThat(contributionColumns.next()).isTrue();
                assertThat(contributionColumns.getInt(1)).isEqualTo(2);
            }
            try (ResultSet migratedUser = statement.executeQuery("""
                    SELECT email, student_id, account_status, session_version
                    FROM sys_user
                    WHERE username = 'legacy_student'
                    """)) {
                assertThat(migratedUser.next()).isTrue();
                assertThat(migratedUser.getString("email"))
                        .isEqualTo("legacy.student@example.invalid");
                assertThat(migratedUser.getString("student_id")).isEqualTo("20260001");
                assertThat(migratedUser.getString("account_status")).isEqualTo("ACTIVE");
                assertThat(migratedUser.getLong("session_version")).isZero();
            }
        }

        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration(
                REDIS.getHost(), REDIS.getMappedPort(6379));
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisConfiguration);
        connectionFactory.afterPropertiesSet();
        try {
            RedisConnection connection = connectionFactory.getConnection();
            try {
                connection.set("csa:integration:key".getBytes(), "ok".getBytes());
                assertThat(new String(connection.get("csa:integration:key".getBytes()))).isEqualTo("ok");
            } finally {
                connection.close();
            }
        } finally {
            connectionFactory.destroy();
        }
    }
}
