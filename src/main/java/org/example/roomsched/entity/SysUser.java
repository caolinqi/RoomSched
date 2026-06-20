package org.example.roomsched.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表：sys_user
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 用户ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** BCrypt加密密码 */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 角色：USER普通用户 ADMIN管理员 */
    private String role;

    /** 状态：0禁用 1正常 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 头像路径 */
    private String avatarUrl;

    /** 邮件通知开关 1开启 0关闭 */
    private Integer emailNotify;

    /** 用户偏好时区 */
    private String timezone;
}
