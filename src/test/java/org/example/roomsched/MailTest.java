package org.example.roomsched;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
public class MailTest {
    @Autowired
    private JavaMailSender mailSender;

    @Test
    public void testSend() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("3341538119@qq.com");
        message.setTo("3341538119@qq.com");
        message.setSubject("【系统自检】SMTP 测试");
        message.setText("如果您收到这封邮件，说明后端的 SMTP 邮件发送配置完全正确！");
        
        System.out.println("====== 开始发送邮件 ======");
        try {
            mailSender.send(message);
            System.out.println("====== 邮件发送成功！======");
        } catch (Exception e) {
            System.out.println("====== 邮件发送失败！======");
            e.printStackTrace();
            throw e;
        }
    }
}
