package com.kidsbook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();

    private static class CodeEntry {
        String code;
        long expireTime;

        CodeEntry(String code, long expireTime) {
            this.code = code;
            this.expireTime = expireTime;
        }
    }

    public void sendVerificationCode(String toEmail) {
        CodeEntry existing = codeStore.get(toEmail);
        if (existing != null && System.currentTimeMillis() - (existing.expireTime - 300000) < 60000) {
            throw new RuntimeException("验证码发送过于频繁，请稍后再试");
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
            throw new RuntimeException("邮件发送失败，请检查邮箱地址是否正确");
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
        if (entry.code.equals(code)) {
            codeStore.remove(email);
            return true;
        }
        return false;
    }

    private String generateCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
