package com.csa.official.common.cache;

import java.util.concurrent.TimeUnit;

public interface KeyValueStore {
    boolean hasKey(String key);

    void setString(String key, String value, long timeout, TimeUnit unit);

    boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit);

    String getString(String key);

    void delete(String key);

    boolean deleteIfValue(String key, String value);

    Long increment(String key);

    void expire(String key, long timeout, TimeUnit unit);

    default Long increment(String key, long timeout, TimeUnit unit) {
        Long count = increment(key);
        if (count != null && count == 1) {
            expire(key, timeout, unit);
        }
        return count;
    }

    default void set(String key, Object value, long timeout, TimeUnit unit) {
        if (value == null) {
            delete(key);
            return;
        }
        setString(key, String.valueOf(value), timeout, unit);
    }

    default Object get(String key) {
        return getString(key);
    }

    default void setLong(String key, long value, long timeout, TimeUnit unit) {
        setString(key, Long.toString(value), timeout, unit);
    }

    default Long getLong(String key) {
        String value = getString(key);
        return value == null ? null : Long.parseLong(value);
    }
}
