package com.csa.official.config;

import com.csa.official.modules.sys.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConfigSerializerTest {

    @Test
    void roundTripsUserWithBigDecimal() {
        GenericJackson2JsonRedisSerializer serializer = new RedisConfig().redisJsonSerializer();
        User user = new User();
        user.setUsername("serializer-test");
        user.setBalance(new BigDecimal("12.34"));

        User restored = (User) serializer.deserialize(serializer.serialize(user));

        assertThat(restored).isNotNull();
        assertThat(restored.getUsername()).isEqualTo("serializer-test");
        assertThat(restored.getBalance()).isEqualByComparingTo("12.34");
    }
}
