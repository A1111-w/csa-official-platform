package com.csa.official.common.security;

import com.csa.official.common.constant.RoleConsts; // 👈 引入常量
import com.csa.official.modules.sys.entity.User;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
public class LoginUser implements UserDetails {

    private User user;

    public LoginUser(User user) {
        this.user = user;
    }

    public Long getId() {
        return user.getId();
    }

    /**
     * 核心权限映射逻辑
     * 策略：层级向下兼容 (Level 4 拥有 Level 0-4 的所有权限)
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        int level = user.getRoleLevel();

        // 1. 基础层级权限生成 (ROLE_LEVEL_0 ~ ROLE_LEVEL_X)
        // 这样写 Controller 时，@PreAuthorize("hasRole('LEVEL_3')")
        // 会长(Level 4) 因为也有 LEVEL_3 的权限，所以也能访问。
        for (int i = 0; i <= level; i++) {
            authorities.add(new SimpleGrantedAuthority("ROLE_LEVEL_" + i));
        }

        // 2. 特殊身份别名 (Alias)
        // 给 Root 账号额外加一个通用的 ADMIN 标签，方便有时候写通用逻辑
        if (level == RoleConsts.ROOT) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

            // 💡 防御性编程：
            // 如果 RoleConsts.ROOT 是 99，上面的循环只加到了 LEVEL_99。
            // 但如果中间有空档（比如 5-98 级没人用），Root 最好也拥有这些中间权限，以防万一。
            // 这里补齐 0 到 ROOT 之间的所有空缺
            for (int i = level + 1; i <= RoleConsts.ROOT; i++) {
                // 实际上上面的循环已经覆盖了 0-99，这里只是为了逻辑严谨
                // 如果 level > ROOT (不可能发生)，不处理
            }
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getDeleted() == 0;
    }
}
