package com.csa.official.modules.sys.controller;

import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder; // 👈 记得导入这个
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private UserMapper userMapper;

    //  注入密码加密器
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public List<User> testDb() {
        return userMapper.selectList(null);
    }

    // 生成加密密码的工具接口
    @GetMapping("/password")
    public String makePassword(@RequestParam String raw) {
        return passwordEncoder.encode(raw);
    }
}
