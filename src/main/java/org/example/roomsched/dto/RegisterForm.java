package org.example.roomsched.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterForm {

    @NotBlank(message = "登录邮箱不能为空")
    @Size(min = 5, max = 50, message = "邮箱长度必须在5到50位之间")
    @jakarta.validation.constraints.Email(message = "请输入有效的邮箱格式")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 72, message = "密码长度必须在6到72位之间")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名不能超过50个字符")
    private String realName;
}
