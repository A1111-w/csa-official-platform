package com.csa.official.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.csa.official.common.annotation.RateLimit;
import com.csa.official.common.constant.AccountStatus;
import com.csa.official.common.constant.RoleConsts;
import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.exception.CsaException;
import com.csa.official.common.result.R;
import com.csa.official.common.security.CsrfTokenService;
import com.csa.official.common.security.AuthCookieService;
import com.csa.official.common.security.JwtRevocationService;
import com.csa.official.common.util.AccountNormalizer;
import com.csa.official.common.util.JwtUtils;
import com.csa.official.modules.sys.dto.LoginDto;
import com.csa.official.modules.sys.dto.RegisterDto;
import com.csa.official.modules.sys.dto.ForgotPasswordDto;
import com.csa.official.modules.sys.dto.ResetPasswordDto;
import com.csa.official.modules.sys.entity.InviteCode;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.InviteCodeMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.service.MailService;
import com.csa.official.modules.sys.service.AccountService;
import com.csa.official.modules.sys.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
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

    @Autowired
    private InviteCodeMapper inviteCodeMapper;

    @Autowired
    private MailService mailService;

    @Autowired
    private CsrfTokenService csrfTokenService;

    @Autowired
    private JwtRevocationService jwtRevocationService;

    @Autowired
    private AuthCookieService authCookieService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AuditService auditService;

    @Value("${csa.privacy.current-version:2026-01}")
    private String privacyVersion;

    @RateLimit(key = "login", time = 300, count = 10, identifiers = {"username"})
    @PostMapping("/login")
    public ResponseEntity<R<Map<String, String>>> login(@RequestBody @Valid LoginDto loginDto,
                                                        HttpServletResponse response) {
        Authentication authentication;
        String username = AccountNormalizer.username(loginDto.getUsername());
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginDto.getPassword()));
        } catch (AuthenticationException e) {
            if (e instanceof AuthenticationServiceException) {
                throw e;
            }
            auditService.recordBestEffort("LOGIN_FAILURE", "USERNAME", username, "FAILURE", username,
                    Map.of("reason", "AUTHENTICATION_FAILED"));
            log.warn("Login failed: username={}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(R.fail(HttpStatus.UNAUTHORIZED.value(), ApiErrorCode.AUTHENTICATION_FAILED.name(),
                            "账号或密码错误"));
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new CsaException(ApiErrorCode.INTERNAL_ERROR, "认证状态异常");
        }

        userMapper.updateLastLoginAt(user.getId(), LocalDateTime.now());

        String jwt = jwtUtils.generateToken(user, 0);
        String csrfToken = csrfTokenService.generateToken();
        authCookieService.issueAuth(response, jwt, Duration.ofMillis(jwtUtils.getExpirationMillis()));
        authCookieService.issueCsrf(response, csrfToken, Duration.ofMillis(jwtUtils.getExpirationMillis()));
        auditService.recordBestEffort("LOGIN_SUCCESS", "USER", String.valueOf(user.getId()),
                "SUCCESS", username, Map.of("roleLevel", user.getRoleLevel()));

        // token 仅通过 httpOnly cookie 下发，不再回传响应体，避免被前端 JS / XSS 读取
        return ResponseEntity.ok(R.ok(Map.of(
                "csrfToken", csrfToken,
                "username", user.getUsername(),
                "roleLevel", String.valueOf(user.getRoleLevel()))));
    }

    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request, HttpServletResponse response) {
        revokeCurrentToken(request);
        authCookieService.clear(response);
        return R.ok("退出成功");
    }

    @GetMapping("/csrf")
    public R<Map<String, String>> csrf(HttpServletResponse response) {
        String csrfToken = csrfTokenService.generateToken();
        authCookieService.issueCsrf(response, csrfToken, Duration.ofMillis(jwtUtils.getExpirationMillis()));

        return R.ok(Map.of("csrfToken", csrfToken));
    }

    @Transactional(rollbackFor = Exception.class)
    @RateLimit(key = "register", time = 60, count = 2, identifiers = {"username", "email"})
    @PostMapping("/register")
    public ResponseEntity<R<String>> register(@RequestBody @Valid RegisterDto dto) {
        if (!privacyVersion.equals(dto.getPrivacyConsentVersion())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(R.fail(HttpStatus.BAD_REQUEST.value(), "隐私政策版本已更新，请重新确认"));
        }
        String username = AccountNormalizer.username(dto.getUsername());
        String email = AccountNormalizer.email(dto.getEmail());
        String studentId = StringUtils.hasText(dto.getStudentId())
                ? AccountNormalizer.studentId(dto.getStudentId()) : null;

        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, username))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(R.fail(HttpStatus.CONFLICT.value(), "用户名已存在"));
        }
        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getEmail, email))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(R.fail(HttpStatus.CONFLICT.value(), "邮箱已注册"));
        }
        if (studentId != null && userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getStudentId, studentId))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(R.fail(HttpStatus.CONFLICT.value(), "学号已注册"));
        }

        mailService.verifyCode(email, dto.getCode());

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(email);
        user.setRealName(dto.getRealName());
        user.setStudentId(studentId);
        user.setCollege(dto.getCollege());
        user.setClassName(dto.getClassName());
        user.setBalance(new BigDecimal("0.00"));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setSessionVersion(0L);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPrivacyConsentVersion(privacyVersion);
        user.setPrivacyConsentAt(LocalDateTime.now());

        if (StringUtils.hasText(dto.getInviteCode())) {
            InviteCode code = inviteCodeMapper.selectOne(new LambdaQueryWrapper<InviteCode>()
                    .eq(InviteCode::getCode, dto.getInviteCode()));
            if (code == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(R.fail(HttpStatus.BAD_REQUEST.value(), "邀请码不存在"));
            }

            int rows = inviteCodeMapper.update(null, new LambdaUpdateWrapper<InviteCode>()
                    .setSql("current_usage = current_usage + 1")
                    .eq(InviteCode::getId, code.getId())
                    .lt(InviteCode::getCurrentUsage, code.getMaxUsage()));

            if (rows <= 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(R.fail(HttpStatus.CONFLICT.value(), "邀请码次数已耗尽"));
            }

            user.setRoleLevel(RoleConsts.MEMBER);
            user.setUsedInviteCode(dto.getInviteCode());
        } else {
            if (StringUtils.hasText(dto.getMerchantNo())) {
                log.warn("Ignoring unverified merchant registration promotion: username={}", username);
            }
            user.setRoleLevel(RoleConsts.GUEST);
            user.setBalance(new BigDecimal("1.00"));
        }

        userMapper.insert(user);
        auditService.recordBestEffort("REGISTER", "USER", String.valueOf(user.getId()),
                "SUCCESS", username, Map.of("roleLevel", user.getRoleLevel(), "privacyVersion", privacyVersion));
        log.info("User registration succeeded: username={}, roleLevel={}", user.getUsername(), user.getRoleLevel());
        return ResponseEntity.ok(R.ok("注册成功"));
    }

    @RateLimit(key = "send_code", time = 60, count = 1, identifiers = {"email"})
    @PostMapping("/send-code")
    public R<String> sendCode(@RequestParam String email) {
        String normalizedEmail = AccountNormalizer.email(email);
        if (!StringUtils.hasText(normalizedEmail) || !normalizedEmail.contains("@")) {
            throw new CsaException(HttpStatus.BAD_REQUEST.value(), "邮箱格式不正确");
        }
        mailService.sendCode(normalizedEmail);
        return R.ok("发送成功");
    }

    @RateLimit(key = "forgot_password", time = 60, count = 2, identifiers = {"email"})
    @PostMapping("/forgot-password")
    public R<String> forgotPassword(@RequestBody @Valid ForgotPasswordDto dto) {
        String email = AccountNormalizer.email(dto.getEmail());
        User user = accountService.findByNormalizedEmail(email);
        if (user != null && AccountStatus.ACTIVE.equals(user.getAccountStatus())) {
            mailService.sendPasswordResetCode(email);
            auditService.recordBestEffort("PASSWORD_RESET_REQUEST", "USER", String.valueOf(user.getId()),
                    "ACCEPTED", null, Map.of());
        } else {
            auditService.recordBestEffort("PASSWORD_RESET_REQUEST", "EMAIL", email, "ACCEPTED", null, Map.of());
        }
        return R.ok("如果账号存在，密码重置验证码将发送到绑定邮箱");
    }

    @RateLimit(key = "reset_password", time = 60, count = 3, identifiers = {"email"})
    @PostMapping("/reset-password")
    public R<String> resetPassword(@RequestBody @Valid ResetPasswordDto dto) {
        String email = AccountNormalizer.email(dto.getEmail());
        mailService.verifyPasswordResetCode(email, dto.getCode());
        User user = accountService.findByNormalizedEmail(email);
        if (user == null) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "验证码无效或账号不存在");
        }
        accountService.resetPassword(user, dto.getNewPassword());
        return R.ok("密码已重置，请重新登录");
    }

    private void revokeCurrentToken(HttpServletRequest request) {
        String token = authCookieService.resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return;
        }

        try {
            jwtRevocationService.revoke(token);
        } catch (Exception e) {
            log.warn("注销时 token 吊销失败，已继续清理 cookie", e);
        }
    }

}
