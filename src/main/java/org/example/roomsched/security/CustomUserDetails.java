package org.example.roomsched.security;

import lombok.Getter;
import org.example.roomsched.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 自定义 UserDetails 实现
 * 封装 SysUser 实体，适配 Spring Security 认证体系
 */
@Getter
public class CustomUserDetails implements UserDetails {

    /** 封装的完整用户信息 */
    private final SysUser user;

    public CustomUserDetails(SysUser user) {
        this.user = user;
    }

    /**
     * 返回用户权限列表
     * 根据 role 字段返回对应角色权限
     * Spring Security 角色需以 "ROLE_" 前缀存储
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 角色格式：ROLE_USER / ROLE_ADMIN
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /** 账号是否未过期 */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** 账号是否未锁定 */
    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() == 1;
    }

    /** 凭证是否未过期 */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** 账号是否启用 */
    @Override
    public boolean isEnabled() {
        return user.getStatus() == 1;
    }
}
