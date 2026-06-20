package org.example.roomsched.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.example.roomsched.entity.SysUser;
import org.example.roomsched.mapper.UserMapper;
import org.example.roomsched.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户 Service 实现类
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUser> implements UserService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public SysUser findByUsername(String username) {
        return getOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username));
    }

    @Override
    public SysUser register(String username, String password, String realName) {
        username = username == null ? null : username.trim();
        realName = realName == null ? null : realName.trim();
        if (username == null || username.isBlank()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (password == null || password.length() < 6 || password.length() > 72) {
            throw new RuntimeException("密码长度必须在6到72位之间");
        }
        if (realName == null || realName.isBlank()) {
            throw new RuntimeException("真实姓名不能为空");
        }
        // 检查用户名是否已存在
        if (findByUsername(username) != null) {
            throw new RuntimeException("用户名已存在：" + username);
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // BCrypt 加密
        user.setRealName(realName);
        user.setRole("USER"); // 默认普通用户
        user.setStatus(1); // 默认启用

        save(user);
        return user;
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 72) {
            throw new RuntimeException("新密码长度必须在6到72位之间");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
    }
}
