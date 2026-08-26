package com.csa.official.modules.sys.controller;

import com.csa.official.common.result.R;
import com.csa.official.common.security.AuthCookieService;
import com.csa.official.common.security.JwtRevocationService;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.dto.ChangePasswordDto;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.service.AccountService;
import com.csa.official.modules.sys.service.PersonalDataExportService;
import com.csa.official.modules.sys.vo.PersonalDataExportVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;
    private final AuthCookieService authCookieService;
    private final JwtRevocationService jwtRevocationService;
    private final PersonalDataExportService personalDataExportService;

    public AccountController(AccountService accountService, AuthCookieService authCookieService,
                             JwtRevocationService jwtRevocationService,
                             PersonalDataExportService personalDataExportService) {
        this.accountService = accountService;
        this.authCookieService = authCookieService;
        this.jwtRevocationService = jwtRevocationService;
        this.personalDataExportService = personalDataExportService;
    }

    @PostMapping("/change-password")
    public R<String> changePassword(@RequestBody @Valid ChangePasswordDto dto,
                                    HttpServletRequest request, HttpServletResponse response) {
        User user = SecurityUtils.getCurrentUser();
        accountService.changePassword(user, dto.getCurrentPassword(), dto.getNewPassword());
        revokeCurrentTokenAndClearCookies(request, response);
        return R.ok("密码已修改，请重新登录");
    }

    @PostMapping("/revoke-sessions")
    public R<String> revokeSessions(HttpServletRequest request, HttpServletResponse response) {
        accountService.revokeAllSessions(SecurityUtils.getCurrentUser());
        revokeCurrentTokenAndClearCookies(request, response);
        return R.ok("所有会话已吊销");
    }

    @PostMapping("/deactivate")
    public R<String> deactivate(HttpServletRequest request, HttpServletResponse response) {
        accountService.deactivate(SecurityUtils.getCurrentUser());
        revokeCurrentTokenAndClearCookies(request, response);
        return R.ok("账号已停用");
    }

    @PostMapping("/deletion-request")
    public R<String> requestDeletion(HttpServletRequest request, HttpServletResponse response) {
        accountService.requestDeletion(SecurityUtils.getCurrentUser());
        revokeCurrentTokenAndClearCookies(request, response);
        return R.ok("账号删除申请已提交");
    }

    @GetMapping("/export")
    public R<PersonalDataExportVO> exportPersonalData() {
        return R.ok(personalDataExportService.exportFor(SecurityUtils.getUserId()));
    }

    private void revokeCurrentTokenAndClearCookies(HttpServletRequest request, HttpServletResponse response) {
        String token = authCookieService.resolveToken(request);
        if (token != null) {
            try {
                jwtRevocationService.revoke(token);
            } catch (RuntimeException ignored) {
                // session_version is the authoritative all-session revocation mechanism.
            }
        }
        authCookieService.clear(response);
    }
}
