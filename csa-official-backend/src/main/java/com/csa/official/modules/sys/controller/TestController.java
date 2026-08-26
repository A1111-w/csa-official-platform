package com.csa.official.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test")
@PreAuthorize("hasRole('ADMIN')")
@Profile({"dev", "local"})
public class TestController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public List<User> testDb() {
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                .select(
                        User::getId,
                        User::getUsername,
                        User::getRealName,
                        User::getEmail,
                        User::getRoleLevel,
                        User::getDepartmentId,
                        User::getDeleted));
    }

    @GetMapping("/password")
    public String makePassword(@RequestParam String raw) {
        return passwordEncoder.encode(raw);
    }
}
