package org.example.roomsched.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.roomsched.entity.SysUser;

/**
 * 用户 Service 接口
 */
public interface UserService extends IService<SysUser> {

    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名
     * @return 用户实体，不存在返回 null
     */
    SysUser findByUsername(String username);

    /**
     * 用户注册
     * 
     * @param username 用户名
     * @param password 明文密码
     * @param realName 真实姓名
     * @return 注册成功返回用户实体
     */
    SysUser register(String username, String password, String realName);

    /**
     * 更新密码
     * 
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void updatePassword(Long userId, String oldPassword, String newPassword);
}
