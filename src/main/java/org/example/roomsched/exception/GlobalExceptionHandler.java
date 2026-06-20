package org.example.roomsched.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.roomsched.util.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.context.MessageSource messageSource;

    @ExceptionHandler(Exception.class)
    public Object handleException(HttpServletRequest request, Exception e) {
        log.error("发生全局异常，请求路径: {}", request.getRequestURI(), e);
        
        String message = "服务器内部繁忙，请稍后再试";
        
        if (e instanceof BusinessException) {
            BusinessException be = (BusinessException) e;
            try {
                // 尝试从 i18n 获取消息，获取当前请求的 Locale
                java.util.Locale locale = org.springframework.web.servlet.support.RequestContextUtils.getLocale(request);
                message = messageSource.getMessage(be.getErrorCode().getMessageKey(), be.getArgs(), locale);
            } catch (Exception ex) {
                // 如果翻译失败，回退到默认消息
                message = be.getErrorCode().getDefaultMessage();
            }
        } else if (e instanceof RuntimeException) {
            message = e.getMessage();
        }

        // 如果是 AJAX 或 API 请求，返回 JSON
        if (request.getRequestURI().startsWith("/api") || 
            "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return org.springframework.http.ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error(500, message));
        }

        // 否则返回 HTML 错误页面
        request.setAttribute("errorMessage", message);
        return "error/500";
    }
}
