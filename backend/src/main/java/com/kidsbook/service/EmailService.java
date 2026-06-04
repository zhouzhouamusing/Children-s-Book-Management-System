package com.kidsbook.service;

import com.kidsbook.common.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private boolean mailConfigured = false;
    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();

    private static class CodeEntry {
        String code;
        long expireTime;
        int attempts;

        CodeEntry(String code, long expireTime) {
            this.code = code;
            this.expireTime = expireTime;
            this.attempts = 0;
        }
    }

    @PostConstruct
    public void validateConfig() {
        if (fromEmail == null || fromEmail.equals("test@qq.com") || fromEmail.isEmpty()) {
            log.warn("邮件服务未正确配置 (MAIL_USERNAME={}), 邮件功能将不可用", fromEmail);
            mailConfigured = false;
        } else {
            mailConfigured = true;
            log.info("邮件服务已配置, 发送邮箱: {}", fromEmail);
        }
    }

    private void checkMailConfigured() {
        if (!mailConfigured) {
            throw new BusinessException(500, "邮件服务未配置，请联系管理员设置邮箱");
        }
    }

    public void sendVerificationCode(String toEmail) {
        checkMailConfigured();

        CodeEntry existing = codeStore.get(toEmail);
        if (existing != null && System.currentTimeMillis() - (existing.expireTime - 300000) < 60000) {
            throw new BusinessException(400, "验证码发送过于频繁，请稍后再试");
        }

        String code = generateCode();
        long expireTime = System.currentTimeMillis() + 300000;
        codeStore.put(toEmail, new CodeEntry(code, expireTime));

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【童书乐园】密码重置验证码");
            message.setText("您好！\n\n您的密码重置验证码为：" + code + "\n\n验证码5分钟内有效，请尽快使用。\n\n如非本人操作，请忽略此邮件。\n\n童书乐园管理系统");
            mailSender.send(message);
            log.info("验证码已发送至: {}", toEmail);
        } catch (Exception e) {
            codeStore.remove(toEmail);
            log.error("邮件发送失败: {}", e.getMessage());
            throw new BusinessException(500, "邮件发送失败，请检查邮箱地址是否正确");
        }
    }

    public boolean verifyCode(String email, String code) {
        CodeEntry entry = codeStore.get(email);
        if (entry == null) {
            return false;
        }
        if (System.currentTimeMillis() > entry.expireTime) {
            codeStore.remove(email);
            return false;
        }
        if (entry.attempts >= 5) {
            codeStore.remove(email);
            return false;
        }
        if (entry.code.equals(code)) {
            codeStore.remove(email);
            return true;
        }
        entry.attempts++;
        return false;
    }

    @Async
    public void sendOverdueReminder(String toEmail, String readerName, String bookTitle, LocalDate dueDate) {
        if (!mailConfigured) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【童书乐园】图书逾期提醒");
            message.setText(String.format("亲爱的%s：\n\n您借阅的《%s》已于%s到期，请尽快归还，以免影响您的借阅权限。\n\n童书乐园管理系统",
                    readerName, bookTitle, dueDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))));
            mailSender.send(message);
            log.info("逾期提醒已发送至: {}", toEmail);
        } catch (Exception e) {
            log.error("逾期提醒邮件发送失败: {} - {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendBorrowConfirmation(String toEmail, String readerName, String bookTitle, LocalDate dueDate) {
        if (!mailConfigured) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【童书乐园】借阅成功通知");
            message.setText(String.format("亲爱的%s：\n\n您已成功借阅《%s》，请在%s前归还。\n\n祝您阅读愉快！\n\n童书乐园管理系统",
                    readerName, bookTitle, dueDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))));
            mailSender.send(message);
            log.info("借阅确认已发送至: {}", toEmail);
        } catch (Exception e) {
            log.error("借阅确认邮件发送失败: {} - {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendReservationReady(String toEmail, String readerName, String bookTitle) {
        if (!mailConfigured) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【童书乐园】预约图书到馆通知");
            message.setText(String.format("亲爱的%s：\n\n您预约的《%s》已到馆，请在3日内前来借阅。\n\n逾期将自动取消预约。\n\n童书乐园管理系统",
                    readerName, bookTitle));
            mailSender.send(message);
            log.info("预约到馆通知已发送至: {}", toEmail);
        } catch (Exception e) {
            log.error("预约通知邮件发送失败: {} - {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendSuspensionNotification(String toEmail, String readerName, String reason) {
        if (!mailConfigured) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【童书乐园】借阅权限暂停通知");
            message.setText(String.format("亲爱的%s：\n\n由于%s，您的借阅权限已被暂停。\n\n您可以登录系统提交申诉，管理员将尽快处理。\n\n童书乐园管理系统",
                    readerName, "overdue".equals(reason) ? "逾期次数过多" : reason));
            mailSender.send(message);
            log.info("暂停通知已发送至: {}", toEmail);
        } catch (Exception e) {
            log.error("暂停通知邮件发送失败: {} - {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendReviewResultNotification(String toEmail, String readerName, String bookTitle, String result) {
        if (!mailConfigured) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【童书乐园】评价审核结果通知");
            String resultText = "approved".equals(result) ? "已通过审核并公开展示" : "未通过审核";
            message.setText(String.format("亲爱的%s：\n\n您对《%s》的评价%s。\n\n%s\n\n童书乐园管理系统",
                    readerName, bookTitle, resultText,
                    "approved".equals(result) ? "感谢您的分享！" : "如有疑问，请联系管理员。"));
            mailSender.send(message);
            log.info("评价审核通知已发送至: {}", toEmail);
        } catch (Exception e) {
            log.error("评价审核通知邮件发送失败: {} - {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendDueReminderNotification(String toEmail, String readerName, String bookTitle, LocalDate dueDate) {
        if (!mailConfigured) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【童书乐园】借阅即将到期提醒");
            message.setText(String.format("亲爱的%s：\n\n您借阅的《%s》将于%s到期，请注意按时归还，以免产生逾期记录。\n\n如需续借，请登录系统操作。\n\n童书乐园管理系统",
                    readerName, bookTitle, dueDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))));
            mailSender.send(message);
            log.info("到期提醒已发送至: {}", toEmail);
        } catch (Exception e) {
            log.error("到期提醒邮件发送失败: {} - {}", toEmail, e.getMessage());
        }
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
