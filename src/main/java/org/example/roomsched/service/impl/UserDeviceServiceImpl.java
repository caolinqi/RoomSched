package org.example.roomsched.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.roomsched.entity.UserDevice;
import org.example.roomsched.mapper.UserDeviceMapper;
import org.example.roomsched.service.UserDeviceService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户设备 Service 实现类
 */
@Service
public class UserDeviceServiceImpl extends ServiceImpl<UserDeviceMapper, UserDevice> implements UserDeviceService {

    @Override
    public void recordDeviceLogin(Long userId, String ipAddress, String deviceInfo, String sessionId) {
        // 查找同一IP和设备指纹的记录
        LambdaQueryWrapper<UserDevice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDevice::getUserId, userId)
                    .eq(UserDevice::getIpAddress, ipAddress)
                    .eq(UserDevice::getDeviceInfo, deviceInfo);

        UserDevice existingDevice = this.getOne(queryWrapper, false);

        if (existingDevice != null) {
            existingDevice.setLastActiveTime(LocalDateTime.now());
            existingDevice.setSessionId(sessionId);
            this.updateById(existingDevice);
        } else {
            UserDevice newDevice = new UserDevice();
            newDevice.setUserId(userId);
            newDevice.setIpAddress(ipAddress);
            newDevice.setDeviceInfo(deviceInfo);
            newDevice.setSessionId(sessionId);
            newDevice.setCreateTime(LocalDateTime.now());
            newDevice.setLastActiveTime(LocalDateTime.now());
            this.save(newDevice);
        }
    }
}
