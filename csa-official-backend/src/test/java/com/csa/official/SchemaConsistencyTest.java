package com.csa.official;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验实体类和全部 Flyway migrations 不会各走各的。
 *
 * <p>解决的是一个很常见、又很难当场发现的问题：给 Entity 加了个字段，
 * 忘了新增对应的 Flyway migration。本地因为表是手工改的，跑起来一切正常；
 * 换台机器按版本链初始化，就会在运行时报
 * {@code Unknown column 'xxx' in 'field list'}。
 *
 * <p>做法：从 {@code @TableName} 拿表名，把实体里的驼峰字段转成下划线，
 * 和全部 Flyway migration 里解析出来的列名做双向 diff。
 */
class SchemaConsistencyTest {

    private static final Pattern TABLE_NAME = Pattern.compile("@TableName\\(\"([^\"]+)\"\\)");
    private static final Pattern ENTITY_FIELD =
            Pattern.compile("(?m)^\\s*private\\s+[\\w<>.\\[\\]]+\\s+(\\w+)\\s*;");
    private static final Pattern CREATE_TABLE =
            Pattern.compile("CREATE TABLE (?:IF NOT EXISTS )?(\\w+)\\s*\\((.*?)\\n\\)\\s*ENGINE", Pattern.DOTALL);
    private static final Pattern ALTER_TABLE =
            Pattern.compile("ALTER TABLE\\s+(\\w+)\\s+(.*?);", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern ADD_COLUMN =
            Pattern.compile("ADD\\s+COLUMN\\s+(\\w+)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern COLUMN_NAME = Pattern.compile("^(\\w+)\\s");

    @Test
    void everyEntityFieldHasMatchingColumnInFlywayMigration() throws IOException {
        Map<String, Set<String>> fromEntities = readEntities();
        Map<String, Set<String>> fromSchema = readSchema();

        assertThat(fromEntities)
                .as("应该能从 modules/**/entity 下解析到实体")
                .isNotEmpty();

        StringBuilder problems = new StringBuilder();
        fromEntities.forEach((table, entityColumns) -> {
            Set<String> schemaColumns = fromSchema.get(table);
            if (schemaColumns == null) {
                problems.append("\n  Flyway migrations 缺少建表语句: ").append(table);
                return;
            }

            Set<String> missing = new TreeSet<>(entityColumns);
            missing.removeAll(schemaColumns);
            if (!missing.isEmpty()) {
                problems.append("\n  [").append(table).append("] 实体有但 Flyway migrations 没有的列: ").append(missing);
            }

            Set<String> extra = new TreeSet<>(schemaColumns);
            extra.removeAll(entityColumns);
            if (!extra.isEmpty()) {
                problems.append("\n  [").append(table).append("] Flyway migrations 有但实体没有的列: ").append(extra);
            }
        });

        assertThat(problems.toString())
                .as("实体与 Flyway migrations 不一致，新环境初始化数据库时会在运行时报 Unknown column：%s", problems)
                .isEmpty();
    }

    private Map<String, Set<String>> readEntities() throws IOException {
        Map<String, Set<String>> result = new TreeMap<>();
        Path modules = Path.of("src", "main", "java", "com", "csa", "official", "modules");

        try (Stream<Path> paths = Files.walk(modules)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.getParent() != null && p.getParent().getFileName().toString().equals("entity"))
                    .toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                Matcher tableName = TABLE_NAME.matcher(source);
                if (!tableName.find()) {
                    continue;
                }

                Set<String> columns = new TreeSet<>();
                Matcher field = ENTITY_FIELD.matcher(source);
                while (field.find()) {
                    columns.add(toSnakeCase(field.group(1)));
                }
                result.put(tableName.group(1), columns);
            }
        }
        return result;
    }

    private Map<String, Set<String>> readSchema() throws IOException {
        Path migrationDirectory = Path.of("src", "main", "resources", "db", "migration");
        assertThat(migrationDirectory).isDirectory();
        Map<String, Set<String>> result = new TreeMap<>();

        try (Stream<Path> paths = Files.list(migrationDirectory)) {
            for (Path migration : paths
                    .filter(path -> path.getFileName().toString().matches("V\\d+__.*\\.sql"))
                    .sorted()
                    .toList()) {
                String sql = Files.readString(migration, StandardCharsets.UTF_8);

                Matcher table = CREATE_TABLE.matcher(sql);
                while (table.find()) {
                    Set<String> columns = new TreeSet<>();
                    for (String rawLine : table.group(2).split("\n")) {
                        String line = rawLine.trim();
                        if (line.isEmpty() || line.startsWith("--") || line.startsWith(")")
                                || line.startsWith("PRIMARY KEY") || line.startsWith("KEY")
                                || line.startsWith("UNIQUE") || line.startsWith("CONSTRAINT")
                                || line.startsWith("INDEX")) {
                            continue;
                        }
                        Matcher column = COLUMN_NAME.matcher(line);
                        if (column.find()) {
                            columns.add(column.group(1));
                        }
                    }
                    result.put(table.group(1), columns);
                }

                Matcher alterTable = ALTER_TABLE.matcher(sql);
                while (alterTable.find()) {
                    Set<String> columns = result.computeIfAbsent(alterTable.group(1), ignored -> new TreeSet<>());
                    Matcher addColumn = ADD_COLUMN.matcher(alterTable.group(2));
                    while (addColumn.find()) {
                        columns.add(addColumn.group(1));
                    }
                }
            }
        }
        return result;
    }

    /** userId -> user_id，和 MyBatis-Plus 的 map-underscore-to-camel-case 保持一致。 */
    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
