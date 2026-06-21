package org.example.roomsched.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.roomsched.entity.SysUser;
import org.example.roomsched.mapper.UserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 自定义 UserDetailsService 实现
 * Spring Security 认证时通过此类加载用户信息
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    /**
     * 根据用户名加载用户信息
     * 
     * @param username 登录账号
     * @return UserDetails 对象
     * @throws UsernameNotFoundException 用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 对于非 admin 账号，强制必须使用邮箱格式登录
        if (!"admin".equals(username)) {
            String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
            if (!username.matches(emailRegex)) {
                throw new UsernameNotFoundException("非 admin 账号必须使用注册邮箱登录");
            }
        }

        // 根据用户名查询用户
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username));

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }

        // 封装为自定义 UserDetails
        return new CustomUserDetails(user);
    }
}
