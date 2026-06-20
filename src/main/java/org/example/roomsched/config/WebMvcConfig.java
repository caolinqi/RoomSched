package org.example.roomsched.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * 配置静态资源映射等
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置静态资源映射
     * Spring Boot 默认已映射 /static/**，此处可做额外配置
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // CSS/JS/图片等静态资源（Spring Boot 默认已处理，此处确保无遗漏）
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
                
        // 用户上传的头像等文件映射到项目根目录下的 uploads 文件夹
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        // 注册全局布局拦截器
        registry.addInterceptor(new LayoutInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/static/**", "/css/**", "/js/**", "/img/**", "/api/**", "/layui/**",
                        "/fullcalendar/**", "/font/**", "/uploads/**");
    }

}
