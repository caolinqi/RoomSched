package org.example.roomsched.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户登录设备实体类
 * 对应数据库表：sys_user_device
 */
@Data
@TableName("sys_user_device")
public class UserDevice {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户ID */
    private Long userId;

    /** 设备与浏览器信息 (User-Agent 解析或原始串) */
    private String deviceInfo;

    /** IP地址 */
    private String ipAddress;

    /** Spring Session ID，用于强制下线 */
    private String sessionId;

    /** 最后活跃时间 */
    private LocalDateTime lastActiveTime;

    /** 首次登录时间 */
    private LocalDateTime createTime;
}
