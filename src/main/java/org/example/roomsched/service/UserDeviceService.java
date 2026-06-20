package org.example.roomsched.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.roomsched.entity.UserDevice;

/**
 * 用户设备 Service 接口
 */
public interface UserDeviceService extends IService<UserDevice> {
    
    /**
     * 记录或更新设备登录信息
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @param deviceInfo 设备信息(User-Agent)
     * @param sessionId 当前会话ID
     */
    void recordDeviceLogin(Long userId, String ipAddress, String deviceInfo, String sessionId);
}
