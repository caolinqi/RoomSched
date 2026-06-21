//package org.example.roomsched.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.example.roomsched.entity.BookingRecord;
//import org.example.roomsched.entity.MeetingRoom;
//import org.example.roomsched.entity.SysUser;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//
//import java.time.format.DateTimeFormatter;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class MailService {
//
//    private final JavaMailSender mailSender;
//
//    @Value("${spring.mail.username:}")
//    private String fromEmail;
//
//    /**
//     * 发送预约成功/通过邮件
//     */
//    @Async
//    public void sendReservationSuccessEmail(SysUser user, BookingRecord record, MeetingRoom room) {
//        if (user.getEmailNotify() != null && user.getEmailNotify() == 0) {
//            log.info("用户 {} 已关闭邮件通知，跳过发送。", user.getUsername());
//            return;
//        }
//        if (user.getEmail() == null || !user.getEmail().contains("@")) {
//            log.warn("用户 {} 邮箱地址无效 ({})，跳过发送。", user.getUsername(), user.getEmail());
//            return;
//        }
//        if (fromEmail == null || fromEmail.isEmpty() || fromEmail.contains("your_email")) {
//            log.warn("系统未配置真实的发件邮箱，模拟发送成功邮件...");
//            return;
//        }
//
//        try {
//            SimpleMailMessage message = new SimpleMailMessage();
//            message.setFrom(fromEmail);
//            message.setTo(user.getEmail());
//            message.setSubject("【会议预定成功】" + record.getMeetingTitle());
//
//            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
//            String text = String.format("尊敬的 %s：\n\n您好！您发起的会议室预约已被成功锁定。\n\n【会议主题】：%s\n【会议室】：%s\n【时间】：%s 至 %s\n【地点】：%s\n\n请按时参加会议！\n\n(此邮件为系统自动发送，请勿回复)",
//                    user.getUsername(),
//                    record.getMeetingTitle(),
//                    room.getRoomName(),
//                    record.getStartTime().format(dtf),
//                    record.getEndTime().format(dtf),
//                    room.getLocation());
//
//            message.setText(text);
//            mailSender.send(message);
//            log.info("预约成功邮件已发送至 {}", user.getEmail());
//        } catch (Exception e) {
//            log.error("发送预约成功邮件失败", e);
//        }
//    }
//
//    /**
//     * 发送预约驳回邮件
//     */
//    @Async
//    public void sendReservationRejectEmail(SysUser user, BookingRecord record, MeetingRoom room) {
//        if (user.getEmailNotify() != null && user.getEmailNotify() == 0) {
//            log.info("用户 {} 已关闭邮件通知，跳过发送。", user.getUsername());
//            return;
//        }
//        if (user.getEmail() == null || !user.getEmail().contains("@")) {
//            return;
//        }
//        if (fromEmail == null || fromEmail.isEmpty() || fromEmail.contains("your_email")) {
//            log.warn("系统未配置真实的发件邮箱，模拟发送驳回邮件...");
//            return;
//        }
//
//        try {
//            SimpleMailMessage message = new SimpleMailMessage();
//            message.setFrom(fromEmail);
//            message.setTo(user.getEmail());
//            message.setSubject("【会议预定驳回】" + record.getMeetingTitle());
//
//            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
//            String text = String.format("尊敬的 %s：\n\n您好！很抱歉，您的会议室预约已被管理员驳回。\n\n【会议主题】：%s\n【会议室】：%s\n【时间】：%s 至 %s\n【驳回原因】：%s\n\n如有疑问请联系管理员。\n\n(此邮件为系统自动发送，请勿回复)",
//                    user.getUsername(),
//                    record.getMeetingTitle(),
//                    room.getRoomName(),
//                    record.getStartTime().format(dtf),
//                    record.getEndTime().format(dtf),
//                    record.getApproveRemark() != null ? record.getApproveRemark() : "无");
//
//            message.setText(text);
//            mailSender.send(message);
//            log.info("预约驳回邮件已发送至 {}", user.getEmail());
//        } catch (Exception e) {
//            log.error("发送预约驳回邮件失败", e);
//        }
//    }
//}
package org.example.roomsched.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.roomsched.entity.BookingRecord;
import org.example.roomsched.entity.MeetingRoom;
import org.example.roomsched.entity.SysUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    /**
     * 发送预约成功/通过邮件
     */
    @Async
    public void sendReservationSuccessEmail(SysUser user, BookingRecord record, MeetingRoom room) {
        if (!shouldSendMail(user)) return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true 表示支持多部分/HTML
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("【会议预定成功】" + record.getMeetingTitle());

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String timeStr = record.getStartTime().format(dtf) + " 至 " + record.getEndTime().format(dtf);

            // 构建 HTML 内容
            String htmlContent = buildHtmlTemplate(
                    "会议预定成功",
                    user.getUsername(),
                    "您发起的会议室预约已被成功锁定，请按时参加。",
                    "#16a34a", // 成功使用绿色
                    record.getMeetingTitle(),
                    room.getRoomName(),
                    timeStr,
                    room.getLocation(),
                    null
            );

            helper.setText(htmlContent, true); // 第二个参数 true 开启 HTML 解析
            mailSender.send(message);
            log.info("预约成功邮件已发送至 {}", user.getEmail());
        } catch (Exception e) {
            log.error("发送预约成功邮件失败", e);
        }
    }

    /**
     * 发送预约驳回邮件
     */
    @Async
    public void sendReservationRejectEmail(SysUser user, BookingRecord record, MeetingRoom room) {
        if (!shouldSendMail(user)) return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("【会议预定驳回】" + record.getMeetingTitle());

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String timeStr = record.getStartTime().format(dtf) + " 至 " + record.getEndTime().format(dtf);
            String rejectReason = record.getApproveRemark() != null ? record.getApproveRemark() : "无详细原因";

            // 构建 HTML 内容
            String htmlContent = buildHtmlTemplate(
                    "会议预定被驳回",
                    user.getUsername(),
                    "很抱歉，您的会议室预约已被管理员驳回。",
                    "#dc2626", // 驳回使用红色
                    record.getMeetingTitle(),
                    room.getRoomName(),
                    timeStr,
                    room.getLocation(),
                    rejectReason
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("预约驳回邮件已发送至 {}", user.getEmail());
        } catch (Exception e) {
            log.error("发送预约驳回邮件失败", e);
        }
    }

    /**
     * 校验是否应该发送邮件的公共逻辑
     */
    private boolean shouldSendMail(SysUser user) {
        if (user.getEmailNotify() != null && user.getEmailNotify() == 0) {
            log.info("用户 {} 已关闭邮件通知，跳过发送。", user.getUsername());
            return false;
        }
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            log.warn("用户 {} 邮箱地址无效 ({})，跳过发送。", user.getUsername(), user.getEmail());
            return false;
        }
        if (fromEmail == null || fromEmail.isEmpty() || fromEmail.contains("your_email")) {
            log.warn("系统未配置真实的发件邮箱，跳过发送...");
            return false;
        }
        return true;
    }

    /**
     * 构建大厂风格的 HTML 邮件模板
     * 注意：邮件客户端对 CSS 支持有限，必须使用行内样式 (Inline CSS)
     */
    private String buildHtmlTemplate(String title, String username, String intro, String themeColor,
                                     String meetingTitle, String roomName, String timeStr, String location, String remark) {

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><body style=\"margin: 0; padding: 0; background-color: #f5f5f5; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #333333;\">");
        html.append("<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #f5f5f5; padding: 40px 0;\"><tr><td align=\"center\">");

        // 主卡片
        html.append("<table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); overflow: hidden;\">");

        // 顶部彩条 (使用传入的主题色)
        html.append("<tr><td style=\"height: 4px; background-color: ").append(themeColor).append(";\"></td></tr>");

        // 卡片内容区
        html.append("<tr><td style=\"padding: 40px;\">");

        // 标题
        html.append("<h2 style=\"margin: 0 0 24px 0; font-size: 20px; color: #1f2329;\">").append(title).append("</h2>");

        // 问候语
        html.append("<p style=\"margin: 0 0 16px 0; font-size: 14px; line-height: 1.6; color: #5c5f66;\">");
        html.append("尊敬的 <b>").append(username).append("</b>：<br>").append(intro);
        html.append("</p>");

        // 信息面板 (浅灰底色区域)
        html.append("<div style=\"background-color: #fafafa; border: 1px solid #e8e9eb; border-radius: 6px; padding: 20px; margin-bottom: 24px;\">");
        html.append("<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"font-size: 14px; line-height: 2;\">");

        html.append("<tr><td width=\"80\" style=\"color: #8f959e;\">会议主题：</td><td style=\"color: #1f2329; font-weight: 500;\">").append(meetingTitle).append("</td></tr>");
        html.append("<tr><td style=\"color: #8f959e;\">会议室：</td><td style=\"color: #1f2329; font-weight: 500;\">").append(roomName).append("</td></tr>");
        html.append("<tr><td style=\"color: #8f959e;\">会议时间：</td><td style=\"color: #1677ff; font-weight: 500;\">").append(timeStr).append("</td></tr>");
        html.append("<tr><td style=\"color: #8f959e;\">会议地点：</td><td style=\"color: #1f2329;\">").append(location).append("</td></tr>");

        // 驳回原因 (如果存在)
        if (remark != null) {
            html.append("<tr><td style=\"color: #8f959e;\">附加说明：</td><td style=\"color: #dc2626;\">").append(remark).append("</td></tr>");
        }

        html.append("</table></div>");

        // 结尾语
        html.append("<p style=\"margin: 0; font-size: 14px; color: #8f959e;\">祝您工作顺利！</p>");

        html.append("</td></tr>");

        // 页脚
        html.append("<tr><td style=\"background-color: #fafafa; border-top: 1px solid #f0f0f0; padding: 20px 40px; text-align: center;\">");
        html.append("<p style=\"margin: 0; font-size: 12px; color: #babbc0;\">此为系统自动发送的通知邮件，请勿直接回复。<br>© RoomSched 智能会议室系统</p>");
        html.append("</td></tr>");

        html.append("</table>");
        html.append("</td></tr></table></body></html>");

        return html.toString();
    }
}