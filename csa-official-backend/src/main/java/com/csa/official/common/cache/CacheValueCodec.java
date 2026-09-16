package com.csa.official.common.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * JSON-only cache codec. Cache values are deliberately not Java native
 * serialization so rolling deployments can reason about the wire format.
 */
@Component
public class CacheValueCodec {

    public static final String NULL_MARKER = "__NULL__";

    private final ObjectMapper objectMapper;

    public CacheValueCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cache value is not JSON serializable", e);
        }
    }

    public <T> T read(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cached value cannot be decoded as " + type.getSimpleName(), e);
        }
    }

    public <T> T read(String value, TypeReference<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cached value cannot be decoded", e);
        }
    }
}
