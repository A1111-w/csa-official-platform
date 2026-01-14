package com.csa.official.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.csa.official.common.result.R;
import com.csa.official.common.util.JwtUtils;
import com.csa.official.modules.sys.dto.LoginDto;
import com.csa.official.modules.sys.dto.RegisterDto;
import com.csa.official.modules.sys.entity.InviteCode;
import com.csa.official.common.annotation.RateLimit;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.service.MailService;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.mapper.InviteCodeMapper;
import com.csa.official.common.constant.RoleConsts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @RateLimit(key = "login", time = 300, count = 10)
    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody @Valid LoginDto loginDto) {
        try {
            // 1. 尝试认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 2. 认证成功，查询用户信息
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, loginDto.getUsername()));

            // 3. 生成 Token
            String jwt = jwtUtils.generateToken(user, 0);

            Map<String, String> map = new HashMap<>();
            map.put("token", jwt);
            map.put("username", user.getUsername());
            map.put("roleLevel", String.valueOf(user.getRoleLevel()));

            return R.ok(map);

        } catch (Exception e) {
            log.error("登录失败: username={}", loginDto.getUsername(), e);
            return R.fail("账号或密码错误");
        }
    }

    @Autowired
    private InviteCodeMapper inviteCodeMapper;

    @RateLimit(key = "register", time = 60, count = 2)
    @PostMapping("/register")
    public R<String> register(@RequestBody @Valid RegisterDto dto) {
        mailService.verifyCode(dto.getEmail(), dto.getCode());
        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()))) {
            return R.fail("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setRealName(dto.getRealName());

        // 补全学籍信息
        user.setStudentId(dto.getStudentId());
        user.setCollege(dto.getCollege());
        user.setClassName(dto.getClassName());
        user.setBalance(new java.math.BigDecimal("0.00"));

        // === 核心判定逻辑 ===

        // 1. 优先判断邀请码 (互斥逻辑：有码就不看支付)
        if (dto.getInviteCode() != null && !dto.getInviteCode().isEmpty()) {
            InviteCode code = inviteCodeMapper.selectOne(new LambdaQueryWrapper<InviteCode>()
                    .eq(InviteCode::getCode, dto.getInviteCode()));

            // 校验码是否有效
            if (code == null)
                return R.fail("邀请码不存在");
            int rows = inviteCodeMapper.update(null, new LambdaUpdateWrapper<InviteCode>()
                    .setSql("current_usage = current_usage + 1") // SQL 原子递增
                    .eq(InviteCode::getId, code.getId())
                    .lt(InviteCode::getCurrentUsage, code.getMaxUsage()) // 乐观锁条件
            );

            if (rows <= 0) {
                return R.fail("邀请码次数已耗尽");
            }

            // 设为会员 (Level 1)
            user.setRoleLevel(1);
            user.setUsedInviteCode(dto.getInviteCode());

        } else if (dto.getMerchantNo() != null && !dto.getMerchantNo().isEmpty()) {
            // 2. 没有邀请码，但有支付单号 -> 视为付费会员
            // TODO: 这里应该去微信API查一下这个单号是不是真的付了钱，现在先信前端
            user.setRoleLevel(1);
            user.setMerchantNo(dto.getMerchantNo());
            user.setBalance(new java.math.BigDecimal("30.00")); // 记录押金

        } else {
            // 3. 啥都没有 -> 路人 (Level 0)
            user.setRoleLevel(RoleConsts.GUEST);
            user.setBalance(new java.math.BigDecimal("1.00")); // 模拟路人押金
        }

        userMapper.insert(user);
        log.info("新用户注册成功: username={}, roleLevel={}", user.getUsername(), user.getRoleLevel());
        return R.ok("注册成功");
    }

    @Autowired
    private MailService mailService; // 注入

    @RateLimit(key = "send_code", time = 60, count = 1)
    @PostMapping("/send-code")
    public R<String> sendCode(@RequestParam String email) {
        if (email == null || !email.contains("@")) {
            return R.fail("邮箱格式不正确");
        }
        mailService.sendCode(email);
        return R.ok("发送成功");
    }
}