package org.example.roomsched.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class LayoutInterceptor implements HandlerInterceptor {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (modelAndView != null && modelAndView.getViewName() != null) {
            String viewName = modelAndView.getViewName();
            // 如果是重定向，或者是 public 页面（如 login/register），或者是 common 页面本身，则不进行拦截包装
            if (!viewName.startsWith("redirect:") 
                && !viewName.startsWith("public/") 
                && !viewName.startsWith("common/")
                && !viewName.startsWith("error")
                && !viewName.equals("front/screen")) {
                // 将真实的视图名称存入 content 变量中，供布局模板动态加载
                modelAndView.addObject("content", viewName);
                // 管理后台页面使用 admin-layout，前台页面使用 layout
                if (viewName.startsWith("admin/")) {
                    modelAndView.setViewName("common/admin-layout");
                } else {
                    modelAndView.setViewName("common/layout");
                }
            }
        }
    }
}
