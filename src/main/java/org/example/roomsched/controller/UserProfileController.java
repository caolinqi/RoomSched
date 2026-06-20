package org.example.roomsched.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.roomsched.entity.SysUser;
import org.example.roomsched.security.CustomUserDetails;
import org.example.roomsched.service.UserService;
import org.example.roomsched.util.Result;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;
    private final org.example.roomsched.service.UserDeviceService userDeviceService;

    /**
     * 头像上传
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        try {
            // 获取当前工作目录，创建 uploads/avatars 文件夹
            String uploadDir = System.getProperty("user.dir") + "/uploads/avatars/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 保存文件
            Path filePath = Paths.get(uploadDir, newFilename);
            Files.write(filePath, file.getBytes());

            // 存入数据库的是一个相对路径，后续可以通过 WebMvcConfigurer 映射
            String avatarUrl = "/uploads/avatars/" + newFilename;
            
            SysUser user = userService.getById(userDetails.getUser().getId());
            user.setAvatarUrl(avatarUrl);
            userService.updateById(user);
            
            // 同步更新 Spring Security 的缓存用户实体
            userDetails.getUser().setAvatarUrl(avatarUrl);

            return Result.ok(avatarUrl);
        } catch (IOException e) {
            log.error("头像上传失败", e);
            return Result.error("头像上传失败");
        }
    }

    /**
     * 更新偏好设置
     */
    @PostMapping("/preferences")
    public Result<Void> updatePreferences(@RequestBody Map<String, Object> params,
                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            SysUser user = userService.getById(userDetails.getUser().getId());
            
            if (params.containsKey("emailNotify")) {
                user.setEmailNotify(Boolean.parseBoolean(params.get("emailNotify").toString()) ? 1 : 0);
            }
            if (params.containsKey("timezone")) {
                user.setTimezone(params.get("timezone").toString());
            }
            
            userService.updateById(user);
            
            // 同步更新缓存
            userDetails.getUser().setEmailNotify(user.getEmailNotify());
            userDetails.getUser().setTimezone(user.getTimezone());
            
            return Result.ok(null);
        } catch (Exception e) {
            return Result.error("保存失败：" + e.getMessage());
        }
    }

    /**
     * 申请停用账号
     */
    @PostMapping("/deactivate")
    public Result<Void> deactivateAccount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            SysUser user = userService.getById(userDetails.getUser().getId());
            // 2 代表禁用状态
            user.setStatus(0); // 假设 0 为禁用
            userService.updateById(user);
            
            // 注意：真正安全的下线需要清理 Session，这里仅改变数据库状态。
            // 前端收到成功后，应自动引导调用 /logout 或清空缓存。
            return Result.ok(null);
        } catch (Exception e) {
            return Result.error("停用申请失败：" + e.getMessage());
        }
    }

    /**
     * 修改基础信息
     */
    @PostMapping("/update-info")
    public Result<Void> updateInfo(@RequestBody Map<String, Object> params,
                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            SysUser user = userService.getById(userDetails.getUser().getId());
            if (params.containsKey("realName")) {
                user.setRealName(params.get("realName").toString());
                userDetails.getUser().setRealName(user.getRealName());
            }
            if (params.containsKey("phone")) {
                user.setPhone(params.get("phone").toString());
                userDetails.getUser().setPhone(user.getPhone());
            }
            userService.updateById(user);
            return Result.ok(null);
        } catch (Exception e) {
            return Result.error("修改失败：" + e.getMessage());
        }
    }

    /**
     * 踢出下线指定设备
     */
    @PostMapping("/device/offline")
    public Result<Void> offlineDevice(@RequestBody Map<String, Long> params,
                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Long deviceId = params.get("deviceId");
            if (deviceId == null) {
                return Result.error("设备ID不能为空");
            }
            // 校验权限：只能踢出自己的设备
            org.example.roomsched.entity.UserDevice device = userDeviceService.getById(deviceId);
            if (device != null && device.getUserId().equals(userDetails.getUser().getId())) {
                userDeviceService.removeById(deviceId);
                // TODO: 若需要真正踢出下线，还需要清理对应的 Spring Session 存储
                return Result.ok(null);
            }
            return Result.error("设备不存在或无权操作");
        } catch (Exception e) {
            return Result.error("操作失败：" + e.getMessage());
        }
    }
}
