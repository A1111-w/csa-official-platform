package com.csa.official.common.cache;

import com.csa.official.config.RedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RedisKeyValueStoreTest {

    @Test
    @SuppressWarnings("unchecked")
    void versionedPublicValuesAreReturnedAsWireJsonWithoutLegacyDeserialization() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(values);
        RedisKeyValueStore store = new RedisKeyValueStore(template, new RedisConfig().redisJsonSerializer());
        String key = CacheKeys.competitionDetail(7L);
        for (String json : new String[]{"{\"id\":1}", "[{\"id\":1}]", "\"\"", "\"intro\"", "__NULL__"}) {
            when(values.get(key)).thenReturn(json);
            assertThat(store.getString(key)).isEqualTo(json);
        }
        verify(template, times(5)).opsForValue();
        verify(values, times(5)).get(key);
    }

    @Test
    @SuppressWarnings("unchecked")
    void versionedPublicValuesAreWrittenAsPlainJsonStrings() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(values);
        RedisKeyValueStore store = new RedisKeyValueStore(template, new RedisConfig().redisJsonSerializer());
        String key = CacheKeys.competitionDetail(8L);

        store.setString(key, "{\"id\":8}", 30, java.util.concurrent.TimeUnit.SECONDS);

        verify(values).set(key, "{\"id\":8}", 30, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Test
    @SuppressWarnings("unchecked")
    void legacyAuthScalarsRemainCompatible() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(values);
        when(values.get("verification:mail")).thenReturn("\"123456\"");
        RedisKeyValueStore store = new RedisKeyValueStore(template, new RedisConfig().redisJsonSerializer());
        assertThat(store.getString("verification:mail")).isEqualTo("123456");
    }
}
