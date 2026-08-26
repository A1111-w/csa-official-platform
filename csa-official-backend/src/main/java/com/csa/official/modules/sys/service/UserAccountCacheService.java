package com.csa.official.modules.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.UserMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class UserAccountCacheService {

    private final UserMapper userMapper;

    public UserAccountCacheService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Cacheable(value = "auth_user", key = "#username", unless = "#result == null")
    public User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    @CacheEvict(value = "auth_user", key = "#username")
    public void evict(String username) {
        // Cache eviction is deliberately a separate proxied method so every account mutation
        // invalidates both Redis and the local development cache.
    }

    @CacheEvict(value = "auth_user", allEntries = true)
    public void evictAll() {
    }
}
