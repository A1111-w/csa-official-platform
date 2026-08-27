package com.csa.official.common.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(prefix = "csa.cache", name = "type", havingValue = "redis")
public class RedisKeyValueStore implements KeyValueStore {

    private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL_SCRIPT = new DefaultRedisScript<>(
            """
                    local current = redis.call('INCR', KEYS[1])
                    if current == 1 then
                        redis.call('PEXPIRE', KEYS[1], ARGV[1])
                    end
                    return current
                    """,
            Long.class);

    private static final DefaultRedisScript<Long> DELETE_IF_VALUE_SCRIPT = new DefaultRedisScript<>(
            """
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                        return redis.call('DEL', KEYS[1])
                    end
                    return 0
                    """,
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final GenericJackson2JsonRedisSerializer legacyValueSerializer;

    public RedisKeyValueStore(StringRedisTemplate stringRedisTemplate,
                              GenericJackson2JsonRedisSerializer legacyValueSerializer) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.legacyValueSerializer = legacyValueSerializer;
    }

    @Override
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    @Override
    public void setString(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    @Override
    public boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, timeout, unit));
    }

    @Override
    public String getString(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }

        if (!looksLikeLegacyJson(value)) {
            return value;
        }

        Object legacyValue = stringRedisTemplate.execute((RedisCallback<Object>) connection -> {
            byte[] keyBytes = stringRedisTemplate.getStringSerializer().serialize(key);
            if (keyBytes == null) {
                return null;
            }
            byte[] rawValue = connection.get(keyBytes);
            return rawValue == null ? null : legacyValueSerializer.deserialize(rawValue);
        });
        return legacyValue == null ? value : stringify(legacyValue);
    }

    @Override
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    public boolean deleteIfValue(String key, String value) {
        Long deleted = stringRedisTemplate.execute(
                DELETE_IF_VALUE_SCRIPT,
                Collections.singletonList(key),
                value);
        return deleted != null && deleted > 0;
    }

    @Override
    public Long increment(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

    @Override
    public Long increment(String key, long timeout, TimeUnit unit) {
        long ttlMillis = unit.toMillis(timeout);
        return stringRedisTemplate.execute(INCREMENT_WITH_TTL_SCRIPT, Collections.singletonList(key),
                String.valueOf(ttlMillis));
    }

    @Override
    public void expire(String key, long timeout, TimeUnit unit) {
        stringRedisTemplate.expire(key, timeout, unit);
    }

    private boolean looksLikeLegacyJson(String value) {
        if (value.isEmpty()) {
            return false;
        }

        char firstChar = value.charAt(0);
        return firstChar == '"' || firstChar == '{' || firstChar == '[';
    }

    private String stringify(Object value) {
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return String.valueOf(value);
    }
}
