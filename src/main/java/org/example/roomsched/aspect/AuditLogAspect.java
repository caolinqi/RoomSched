package org.example.roomsched.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.roomsched.annotation.LogAction;
import org.example.roomsched.entity.SysAuditLog;
import org.example.roomsched.mapper.AuditLogMapper;
import org.example.roomsched.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogMapper auditLogMapper;

    @AfterReturning(pointcut = "@annotation(org.example.roomsched.annotation.LogAction)", returning = "result")
    public void recordLog(JoinPoint joinPoint, Object result) {
        try {
            SysAuditLog auditLog = new SysAuditLog();
            
            // 获取动作描述
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            LogAction logAction = method.getAnnotation(LogAction.class);
            if (logAction != null) {
                auditLog.setAction(logAction.value());
            }

            // 获取当前用户信息
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                auditLog.setUserId(userDetails.getUser().getId());
                auditLog.setUsername(userDetails.getUsername());
            } else {
                auditLog.setUsername("SYSTEM/ANONYMOUS");
            }

            // 获取IP地址
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                auditLog.setIpAddress(ip);
            }

            // 记录参数详情
            Object[] args = joinPoint.getArgs();
            String details = Arrays.toString(args);
            // 简单截断，防止过长
            if (details.length() > 500) {
                details = details.substring(0, 500) + "...";
            }
            auditLog.setDetails(details);
            
            // 设置时间
            auditLog.setCreateTime(LocalDateTime.now());

            // 入库
            auditLogMapper.insert(auditLog);
            log.info("记录审计日志成功: {}", auditLog.getAction());
            
        } catch (Exception e) {
            log.error("记录审计日志失败", e);
        }
    }
}
