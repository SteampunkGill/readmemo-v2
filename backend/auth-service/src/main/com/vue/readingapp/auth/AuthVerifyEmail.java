package com.vue.readingapp.auth;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

/**
 * 邮箱验证控制器
 * 
 * 负责处理用户注册后的邮箱激活流程，确保用户提供的邮箱地址真实有效。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthVerifyEmail {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 辅助方法：打印验证请求日志
     */
    private void printRequest(Object request) {
        System.out.println("=== 收到验证邮箱请求 ===");
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
     * 验证邮箱请求的数据结构
     */
    public static class VerifyEmailRequest {
        private String token; // 验证令牌
        private String email; // 待验证的邮箱

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    /**
     * 验证邮箱响应的统一格式
     */
    public static class VerifyEmailResponse {
        private boolean success;
        private String message;

        public VerifyEmailResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * 验证邮箱接口
     * 
     * 用户点击邮件中的链接后，前端会调用此接口完成验证。
     * 
     * @param request 包含 token 和 email 的请求体
     * @return 验证结果
     */
    @PostMapping("/verify-email")
    public ResponseEntity<VerifyEmailResponse> verifyEmail(@RequestBody VerifyEmailRequest request) {
        printRequest(request);

        try {
            // 1. 基础验证：确保参数不为空
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new VerifyEmailResponse(false, "验证令牌不能为空")
                );
            }

            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new VerifyEmailResponse(false, "邮箱不能为空")
                );
            }

            // 2. 格式验证
            if (!request.getEmail().contains("@")) {
                return ResponseEntity.badRequest().body(
                        new VerifyEmailResponse(false, "邮箱格式不正确")
                );
            }

            // 3. 数据库检查：确认该邮箱是否存在
            String checkEmailSql = "SELECT user_id, is_verified FROM users WHERE email = ?";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(checkEmailSql, request.getEmail());

            if (users.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new VerifyEmailResponse(false, "邮箱不存在")
                );
            }

            Map<String, Object> user = users.get(0);
            Integer userId = (Integer) user.get("user_id");
            Boolean isVerified = (Boolean) user.get("is_verified");

            // 4. 状态检查：如果已经验证过，则无需重复操作
            if (isVerified != null && isVerified) {
                return ResponseEntity.badRequest().body(
                        new VerifyEmailResponse(false, "邮箱已经验证过了")
                );
            }

            // 5. 逻辑处理：在实际项目中，此处应比对数据库中存储的验证令牌。
            // 为了简化演示，我们假设只要传了令牌就视为有效。
            System.out.println("正在验证邮箱令牌: " + request.getToken() + " 对应邮箱: " + request.getEmail());

            // 6. 更新状态：将用户的 is_verified 字段设为 TRUE
            String updateVerificationSql = "UPDATE users SET is_verified = TRUE WHERE user_id = ?";
            int updatedRows = jdbcTemplate.update(updateVerificationSql, userId);

            if (updatedRows == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new VerifyEmailResponse(false, "更新验证状态失败")
                );
            }

            System.out.println("用户ID " + userId + " 的邮箱已验证成功");

            // 7. 返回成功响应
            VerifyEmailResponse response = new VerifyEmailResponse(true, "邮箱验证成功");
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new VerifyEmailResponse(false, "服务器内部错误: " + e.getMessage())
            );
        }
    }

    /**
     * 发送邮箱验证邮件接口
     * 
     * 用于在用户注册后或手动点击“重新发送”时，生成验证令牌并模拟发送邮件。
     */
    @PostMapping("/send-verification-email")
    public ResponseEntity<VerifyEmailResponse> sendVerificationEmail(HttpServletRequest httpRequest) {
        printRequest("发送邮箱验证邮件");

        try {
            // 1. 身份识别：必须是已登录用户才能请求发送验证邮件
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new VerifyEmailResponse(false, "未提供有效的认证令牌")
                );
            }

            String token = authHeader.substring(7);

            // 2. 关联查询：查找当前登录用户及其邮箱状态
            String findUserSql = "SELECT s.user_id, u.email, u.is_verified FROM user_sessions s " +
                    "JOIN users u ON s.user_id = u.user_id " +
                    "WHERE s.access_token = ? AND s.expires_at > ?";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(findUserSql, token, LocalDateTime.now());

            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new VerifyEmailResponse(false, "令牌无效或已过期")
                );
            }

            Map<String, Object> user = users.get(0);
            Integer userId = (Integer) user.get("user_id");
            String email = (String) user.get("email");
            Boolean isVerified = (Boolean) user.get("is_verified");

            // 3. 状态检查
            if (isVerified != null && isVerified) {
                return ResponseEntity.badRequest().body(
                        new VerifyEmailResponse(false, "邮箱已经验证过了")
                );
            }

            // 4. 生成令牌：创建一个唯一的随机字符串作为验证令牌
            String verificationToken = "verify_" + java.util.UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusHours(24); // 令牌有效期设为 24 小时

            // 5. 模拟发送：在控制台打印验证信息（实际项目中应调用邮件服务发送邮件）
            System.out.println("--- 模拟发送验证邮件 ---");
            System.out.println("收件人: " + email);
            System.out.println("验证令牌: " + verificationToken);
            System.out.println("过期时间: " + expiresAt);
            System.out.println("验证链接: http://localhost:8080/api/auth/verify-email?token=" +
                    verificationToken + "&email=" + email);
            System.out.println("-----------------------");

            // 6. 返回成功响应
            VerifyEmailResponse response = new VerifyEmailResponse(true, "验证邮件已发送，请查收您的邮箱");
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new VerifyEmailResponse(false, "服务器内部错误: " + e.getMessage())
            );
        }
    }
}