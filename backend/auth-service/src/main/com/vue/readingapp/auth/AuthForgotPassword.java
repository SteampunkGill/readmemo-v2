package com.vue.readingapp.auth;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.List;

/**
 * 忘记密码控制器
 * 
 * 负责处理用户找回密码的流程，包括发送验证码邮件、验证验证码等功能。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthForgotPassword {

    @Autowired
    private JavaMailSender mailSender; // Spring 提供的邮件发送工具

    @Autowired
    private JdbcTemplate jdbcTemplate; // 数据库操作工具

    @Value("${app.frontend.url:http://localhost:8080}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail; // 发件人邮箱地址

    /**
     * 辅助方法：打印请求日志
     */
    private void printRequest(Object request) {
        System.out.println("=== 收到请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("====================");
    }

    /**
     * 辅助方法：打印返回响应日志
     */
    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    /**
     * 安全转换数据库时间类型为 Java LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        if (value instanceof Timestamp) return ((Timestamp) value).toLocalDateTime();
        return null;
    }

    /**
     * 忘记密码请求的数据结构
     */
    public static class ForgotPasswordRequest {
        private String email;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        @Override
        public String toString() {
            return "ForgotPasswordRequest{email='" + email + "'}";
        }
    }

    /**
     * 验证码校验请求的数据结构
     */
    public static class VerifyCodeRequest {
        private String email;
        private String code;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        @Override
        public String toString() {
            return "VerifyCodeRequest{email='" + email + "', code='" + code + "'}";
        }
    }

    /**
     * 统一的响应格式
     */
    public static class ForgotPasswordResponse {
        private boolean success;
        private String message;

        public ForgotPasswordResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * 生成 4 位数字验证码
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 1000 + random.nextInt(9000); // 范围 1000-9999
        return String.valueOf(code);
    }

    /**
     * 核心逻辑：发送验证码邮件并将令牌存入数据库
     * 
     * @param toEmail 收件人
     * @param verificationCode 验证码
     * @param expiresAt 过期时间
     * @return 是否操作成功
     */
    private boolean sendVerificationEmailAndStoreToken(String toEmail, String verificationCode, LocalDateTime expiresAt) {
        try {
            // 1. 获取用户 ID
            Integer userId = getUserIdByEmail(toEmail);
            if (userId == null) {
                return false;
            }

            // 2. 将验证码存入数据库，以便后续校验
            String insertSql = "INSERT INTO password_reset_tokens (user_id, token, expires_at, created_at) VALUES (?, ?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"token_id"});
                ps.setInt(1, userId);
                ps.setString(2, verificationCode);
                ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                return ps;
            }, keyHolder);

            // 3. 构造并发送邮件
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("密码重置验证码 - 阅记星");

            String emailContent = "您的密码重置验证码是：\n\n" +
                    verificationCode + "\n\n" +
                    "该验证码将在 10 分钟内有效。\n\n" +
                    "如果您没有请求重置密码，请忽略此邮件。\n\n" +
                    "阅记星团队";

            message.setText(emailContent);
            mailSender.send(message);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 辅助方法：根据邮箱查询用户 ID
     */
    private Integer getUserIdByEmail(String email) {
        String sql = "SELECT user_id FROM users WHERE email = ?";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, email);
        if (result.isEmpty()) {
            return null;
        }
        return (Integer) result.get(0).get("user_id");
    }

    /**
     * 忘记密码接口：用户输入邮箱，系统发送验证码
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        printRequest(request);

        try {
            // 1. 基础验证
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ForgotPasswordResponse(false, "邮箱不能为空")
                );
            }

            // 2. 格式验证
            String email = request.getEmail().trim();
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return ResponseEntity.badRequest().body(
                        new ForgotPasswordResponse(false, "邮箱格式不正确")
                );
            }

            // 3. 检查用户是否存在
            Integer userId = getUserIdByEmail(email);
            if (userId == null) {
                // 安全考虑：不明确告知邮箱未注册，防止邮箱探测
                return ResponseEntity.badRequest().body(
                        new ForgotPasswordResponse(false, "若此邮箱已注册，我们将发送验证码。")
                );
            }

            // 4. 生成并发送验证码
            String verificationCode = generateVerificationCode();
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10); // 10 分钟有效期

            boolean success = sendVerificationEmailAndStoreToken(email, verificationCode, expiresAt);

            if (!success) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new ForgotPasswordResponse(false, "邮件发送失败，请稍后重试")
                );
            }

            // 5. 返回成功提示
            ForgotPasswordResponse response = new ForgotPasswordResponse(true,
                    "验证码已发送到您的注册邮箱，请查收。");
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ForgotPasswordResponse(false, "服务器内部错误，请稍后重试")
            );
        }
    }

    /**
     * 验证码校验接口：用户输入收到的验证码，系统进行比对
     */
    @PostMapping("/verify-code")
    public ResponseEntity<ForgotPasswordResponse> verifyCode(@RequestBody VerifyCodeRequest request) {
        printRequest(request);

        try {
            // 1. 基础验证
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ForgotPasswordResponse(false, "邮箱不能为空")
                );
            }

            if (request.getCode() == null || request.getCode().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ForgotPasswordResponse(false, "验证码不能为空")
                );
            }

            String email = request.getEmail().trim();
            String code = request.getCode().trim();

            // 2. 查找用户
            Integer userId = getUserIdByEmail(email);
            if (userId == null) {
                return ResponseEntity.badRequest().body(
                        new ForgotPasswordResponse(false, "无效的请求")
                );
            }

            // 3. 数据库比对验证码
            String selectSql = "SELECT token_id, token, expires_at FROM password_reset_tokens WHERE user_id = ? AND token = ?";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(selectSql, userId, code);

            if (result.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ForgotPasswordResponse(false, "验证码错误或已失效")
                );
            }

            // 4. 过期检查
            Map<String, Object> tokenInfo = result.get(0);
            LocalDateTime expiresAt = convertToLocalDateTime(tokenInfo.get("expires_at"));
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(expiresAt)) {
                // 清理过期令牌
                jdbcTemplate.update("DELETE FROM password_reset_tokens WHERE token_id = ?", tokenInfo.get("token_id"));
                return ResponseEntity.badRequest().body(
                        new ForgotPasswordResponse(false, "验证码已过期，请重新获取")
                );
            }

            // 5. 验证成功
            ForgotPasswordResponse response = new ForgotPasswordResponse(true, "验证码验证成功");
            printResponse(response);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ForgotPasswordResponse(false, "服务器内部错误，请稍后重试")
            );
        }
    }
}