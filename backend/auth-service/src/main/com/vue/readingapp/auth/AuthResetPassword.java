package com.vue.readingapp.auth;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

/**
 * 重置密码控制器
 * 
 * 负责处理用户在通过验证码校验后，设置新密码的最终步骤。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthResetPassword {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 辅助方法：打印重置密码请求日志
     */
    private void printRequest(Object request) {
        System.out.println("=== 收到重置密码请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("====================");
    }

    /**
     * 辅助方法：打印数据库查询结果
     */
    private void printQueryResult(Object result) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("查询结果: " + result);
        System.out.println("===================");
    }

    /**
     * 辅助方法：打印返回给前端的响应
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
     * 重置密码请求的数据结构
     */
    public static class ResetPasswordRequest {
        private String token;                 // 之前验证通过的令牌/验证码
        private String email;                 // 用户邮箱
        private String password;              // 新密码
        private String password_confirmation; // 确认新密码

        // Getters and Setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getPassword_confirmation() { return password_confirmation; }
        public void setPassword_confirmation(String password_confirmation) { this.password_confirmation = password_confirmation; }

        @Override
        public String toString() {
            // 注意：重写 toString 时隐藏密码，防止在日志中泄露敏感信息
            return "ResetPasswordRequest{" +
                    "token='" + token + '\'' +
                    ", email='" + email + '\'' +
                    ", password='[PROTECTED]'" +
                    ", password_confirmation='[PROTECTED]'" +
                    '}';
        }
    }

    /**
     * 重置密码响应的统一格式
     */
    public static class ResetPasswordResponse {
        private boolean success;
        private String message;

        public ResetPasswordResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * 重置密码接口
     * 
     * @param request 包含令牌、邮箱和新密码的请求体
     * @return 重置结果
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        printRequest(request);

        try {
            // 1. 基础验证：确保所有字段都已填写
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ResetPasswordResponse(false, "重置令牌不能为空")
                );
            }

            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ResetPasswordResponse(false, "邮箱不能为空")
                );
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ResetPasswordResponse(false, "新密码不能为空")
                );
            }

            // 2. 逻辑验证：检查两次输入的密码是否一致
            if (!request.getPassword().equals(request.getPassword_confirmation())) {
                return ResponseEntity.badRequest().body(
                        new ResetPasswordResponse(false, "两次输入的密码不一致")
                );
            }

            // 3. 安全验证：检查密码强度（此处仅检查长度）
            if (request.getPassword().length() < 6) {
                return ResponseEntity.badRequest().body(
                        new ResetPasswordResponse(false, "密码长度不能少于 6 位")
                );
            }

            // 4. 用户识别：确认邮箱对应的用户是否存在
            String checkEmailSql = "SELECT user_id FROM users WHERE email = ?";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(checkEmailSql, request.getEmail());

            if (users.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ResetPasswordResponse(false, "此邮箱未注册")
                );
            }

            Integer userId = (Integer) users.get(0).get("user_id");

            // 5. 令牌验证：再次确认重置令牌是否有效且属于该用户
            String checkTokenSql = "SELECT token_id, expires_at FROM password_reset_tokens WHERE user_id = ? AND token = ?";
            List<Map<String, Object>> tokens = jdbcTemplate.queryForList(checkTokenSql, userId, request.getToken());

            if (tokens.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ResetPasswordResponse(false, "重置令牌无效或已失效")
                );
            }

            Map<String, Object> tokenInfo = tokens.get(0);
            LocalDateTime expiresAt = convertToLocalDateTime(tokenInfo.get("expires_at"));
            LocalDateTime now = LocalDateTime.now();

            // 6. 过期检查
            if (now.isAfter(expiresAt)) {
                jdbcTemplate.update("DELETE FROM password_reset_tokens WHERE token_id = ?", tokenInfo.get("token_id"));
                return ResponseEntity.badRequest().body(
                        new ResetPasswordResponse(false, "重置令牌已过期，请重新申请")
                );
            }

            // 7. 执行更新：修改数据库中的用户密码
            // 注意：实际项目中应使用 BCryptPasswordEncoder 加密后再存储
            String updatePasswordSql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
            jdbcTemplate.update(updatePasswordSql, request.getPassword(), userId);

            // 8. 清理工作：删除已使用的重置令牌
            jdbcTemplate.update("DELETE FROM password_reset_tokens WHERE token_id = ?", tokenInfo.get("token_id"));

            // 9. 安全增强：强制该用户的所有旧会话失效（登出所有设备）
            jdbcTemplate.update("DELETE FROM user_sessions WHERE user_id = ?", userId);

            // 10. 返回成功响应
            ResetPasswordResponse response = new ResetPasswordResponse(true, "密码重置成功，请使用新密码登录");
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ResetPasswordResponse(false, "服务器内部错误，请稍后重试")
            );
        }
    }
}