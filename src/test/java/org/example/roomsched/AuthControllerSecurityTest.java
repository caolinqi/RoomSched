package org.example.roomsched;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring Security 安全测试
 * 验证：访问控制、CSRF 保护、角色隔离
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Spring Security 安全测试")
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // ========== 访问控制 ==========

    @Test
    @DisplayName("未登录访问管理员页面 → 重定向到登录页")
    void adminDashboard_notAuthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("未登录访问用户首页 → 重定向到登录页")
    void userHome_notAuthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/booking"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("普通用户访问管理员页面 → 403 Forbidden")
    @WithMockUser(username = "user", roles = "USER")
    void adminDashboard_regularUser_forbids() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("管理员访问管理员页面 → 200 OK")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminDashboard_adminUser_allows() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("公开页面（登录页）无需认证 → 200 OK")
    void loginPage_publicAccess_allowed() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("状态大屏接口无需认证 → 200 OK")
    void screenApi_publicAccess_allowed() throws Exception {
        mockMvc.perform(get("/api/screen/rooms"))
                .andExpect(status().isOk());
    }

    // ========== CSRF 保护 ==========

    @Test
    @DisplayName("POST 请求不带 CSRF Token → 403 Forbidden")
    @WithMockUser(username = "user", roles = "USER")
    void post_withoutCsrfToken_forbidden() throws Exception {
        mockMvc.perform(post("/booking")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("roomId", "1")
                        .param("meetingTitle", "测试会议"))
                // 不带 CSRF token，应返回 403
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST 请求带正确 CSRF Token → 不因 CSRF 拒绝（可能因业务逻辑返回其他状态）")
    @WithMockUser(username = "user", roles = "USER")
    void post_withCsrfToken_notForbiddenByCsrf() throws Exception {
        mockMvc.perform(post("/booking")
                        .with(csrf()) // Spring Security Test 注入有效 CSRF token
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("roomId", "999"))
                // 带正确 CSRF token，不应因 CSRF 返回 403（可能因业务逻辑返回 400/302 等）
                .andExpect(status().is(not(403)));
    }

    @Test
    @DisplayName("API PUT 请求不带 CSRF Token → 403")
    @WithMockUser(username = "user", roles = "USER")
    void apiPut_withoutCsrfToken_forbidden() throws Exception {
        mockMvc.perform(post("/api/booking/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ========== 角色隔离 ==========

    @Test
    @DisplayName("普通用户调用管理员 API → 403")
    @WithMockUser(username = "user", roles = "USER")
    void adminApi_regularUser_forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/bookings/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("管理员调用管理员 API → 不返回 403")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminApi_adminUser_notForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/bookings/pending"))
                .andExpect(status().is(not(403)));
    }
}
