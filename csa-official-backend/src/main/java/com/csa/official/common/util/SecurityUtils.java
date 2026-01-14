package com.csa.official.common.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.common.exception.CsaException;
import com.csa.official.common.security.LoginUser;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    private static UserMapper userMapper;

    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        SecurityUtils.userMapper = userMapper;
    }

    /**
     * 获取当前登录用户 ID
     */
    public static Long getUserId() {
        return getCurrentUser().getId();
    }

    /**
     * 获取当前登录用户完整信息
     * 优先从 Context 内存获取，无需查库
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new CsaException(401, "登录已过期");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof LoginUser) {
            return ((LoginUser) principal).getUser();
        }

        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new CsaException(401, "用户异常");
        }
        return user;
    }
}