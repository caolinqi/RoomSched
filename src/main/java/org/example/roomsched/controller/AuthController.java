package org.example.roomsched.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.roomsched.dto.RegisterForm;
import org.example.roomsched.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "public/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "public/register";
    }

    @PostMapping("/register")
    public String register(@Valid RegisterForm form, BindingResult bindingResult, Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            model.addAttribute("registerForm", form);
            return "public/register";
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            model.addAttribute("error", "两次密码输入不一致");
            model.addAttribute("registerForm", form);
            return "public/register";
        }

        try {
            userService.register(form.getUsername(), form.getPassword(), form.getRealName());
            redirectAttributes.addFlashAttribute("success", "注册成功，请登录");
            return "redirect:/login";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("registerForm", form);
            return "public/register";
        }
    }
}
