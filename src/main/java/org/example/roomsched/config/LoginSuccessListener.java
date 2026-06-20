package org.example.roomsched.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.roomsched.security.CustomUserDetails;
import org.example.roomsched.service.UserDeviceService;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 监听登录成功事件，记录设备指纹和 IP
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {

    private final UserDeviceService userDeviceService;

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();

        if (principal instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) principal;
            Long userId = userDetails.getUser().getId();

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                String ipAddress = getClientIpAddress(request);
                String userAgent = request.getHeader("User-Agent");
                String sessionId = request.getSession().getId();
                
                if (userAgent == null) {
                    userAgent = "Unknown Device";
                }

                // 异步记录或直接同步记录（这里使用同步，因为操作较轻）
                try {
                    userDeviceService.recordDeviceLogin(userId, ipAddress, userAgent, sessionId);
                    log.info("Recorded login device for user {}: IP={}, Device={}", userId, ipAddress, userAgent);
                } catch (Exception e) {
                    log.error("Failed to record device login info for user {}", userId, e);
                }
            }
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headersToCheck = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headersToCheck) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                // 如果经过多个代理，只取第一个有效的IP
                if (ip.contains(",")) {
                    return ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        return request.getRemoteAddr();
    }
}
