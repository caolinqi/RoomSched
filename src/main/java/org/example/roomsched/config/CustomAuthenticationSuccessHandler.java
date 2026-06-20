package org.example.roomsched.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 自定义登录成功处理器
 * 根据用户角色跳转到不同页面：
 * - ADMIN 角色 → /admin/dashboard
 * - USER 角色 → /
 */
@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        // 判断是否为管理员角色
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            // 管理员跳转到仪表盘
            setDefaultTargetUrl("/admin/dashboard");
        } else {
            // 普通用户跳转到首页
            setDefaultTargetUrl("/");
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
