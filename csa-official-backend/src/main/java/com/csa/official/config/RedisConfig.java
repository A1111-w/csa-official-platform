package com.csa.official.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
@EnableCaching
public class RedisConfig {

        /**
         * 配置一个 JSON 序列化器
         */
        private GenericJackson2JsonRedisSerializer getJsonSerializer() {
                ObjectMapper objectMapper = new ObjectMapper();

                // 1. 解决 LocalDateTime 序列化报错的问题
                JavaTimeModule javaTimeModule = new JavaTimeModule();
                // 配置序列化格式 (yyyy-MM-dd HH:mm:ss)
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
                javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
                objectMapper.registerModule(javaTimeModule);

                // 2. 设置可见性 (允许序列化私有字段)
                objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

                // 3. 启用类型信息 (这样 Redis 才知道存的是 Dept 还是 User)
                objectMapper.activateDefaultTyping(
                                LaissezFaireSubTypeValidator.instance,
                                ObjectMapper.DefaultTyping.NON_FINAL);

                return new GenericJackson2JsonRedisSerializer(objectMapper);
        }

        @Bean
        @SuppressWarnings("null")
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
                RedisTemplate<String, Object> template = new RedisTemplate<>();
                template.setConnectionFactory(factory);

                // 使用自定义的序列化器
                GenericJackson2JsonRedisSerializer jsonSerializer = getJsonSerializer();

                template.setKeySerializer(new StringRedisSerializer());
                template.setValueSerializer(jsonSerializer);
                template.setHashKeySerializer(new StringRedisSerializer());
                template.setHashValueSerializer(jsonSerializer);

                template.afterPropertiesSet();
                return template;
        }

        @Bean
        @SuppressWarnings("null")
        public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
                // 使用自定义的序列化器
                GenericJackson2JsonRedisSerializer jsonSerializer = getJsonSerializer();

                RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofHours(1))
                                // 配置 Key 和 Value 的序列化方式
                                .serializeKeysWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(jsonSerializer))
                                .disableCachingNullValues();

                return RedisCacheManager.builder(factory)
                                .cacheDefaults(config)
                                .build();
        }
}