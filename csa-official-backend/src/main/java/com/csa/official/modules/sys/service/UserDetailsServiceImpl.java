package com.csa.official.modules.sys.service;

import com.csa.official.common.security.LoginUser;
import com.csa.official.modules.sys.entity.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserAccountCacheService userAccountCacheService;

    public UserDetailsServiceImpl(UserAccountCacheService userAccountCacheService) {
        this.userAccountCacheService = userAccountCacheService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userAccountCacheService.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User does not exist");
        }
        return new LoginUser(user);
    }
}
