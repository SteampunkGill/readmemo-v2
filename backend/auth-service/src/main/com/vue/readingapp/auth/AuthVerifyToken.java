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
 * 令牌验证控制器
 * 
 * 负责检查用户持有的访问令牌（Token）是否依然有效，并返回关联的用户基本信息。
 * 常用于前端页面刷新时恢复登录状态。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthVerifyToken {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 辅助方法：打印验证请求日志
     */
    private void printRequest(Object request) {
        System.out.println("=== 收到验证令牌请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=====================");
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
     * 验证令牌请求的数据结构
     */
    public static class VerifyTokenRequest {
        private String token;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }

    /**
     * 验证令牌响应的统一格式
     */
    public static class VerifyTokenResponse {
        private boolean success;
        private String message;
        private UserInfo data;

        public VerifyTokenResponse(boolean success, String message, UserInfo data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UserInfo getData() { return data; }
        public void setData(UserInfo data) { this.data = data; }
    }

    /**
     * 令牌关联的用户简要信息
     */
    public static class UserInfo {
        private Integer userId;
        private String username;
        private String email;
        private String role;
        private Boolean isValid; // 标记令牌是否真正有效

        public UserInfo(Integer userId, String username, String email, String role, Boolean isValid) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.role = role;
            this.isValid = isValid;
        }

        // Getters and Setters
        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public Boolean getIsValid() { return isValid; }
        public void setIsValid(Boolean isValid) { this.isValid = isValid; }
    }

    /**
     * 验证令牌接口（通过请求体传参）
     * 
     * @param request 包含 token 字符串的对象
     * @return 验证结果及用户信息
     */
    @PostMapping("/verify-token")
    public ResponseEntity<VerifyTokenResponse> verifyToken(@RequestBody VerifyTokenRequest request) {
        printRequest(request);

        try {
            // 1. 基础验证
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new VerifyTokenResponse(false, "令牌不能为空", null)
                );
            }

            // 2. 数据库查询：通过 Token 关联查询用户信息
            String findSessionSql = "SELECT s.session_id, s.user_id, s.expires_at, u.username, u.email, u.role " +
                    "FROM user_sessions s " +
                    "JOIN users u ON s.user_id = u.user_id " +
                    "WHERE s.access_token = ?";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(findSessionSql, request.getToken());
            printQueryResult(sessions);

            if (sessions.isEmpty()) {
                return ResponseEntity.ok(
                        new VerifyTokenResponse(false, "令牌无效", new UserInfo(null, null, null, null, false))
                );
            }

            // 3. 过期检查：对比当前时间与令牌过期时间
            Map<String, Object> session = sessions.get(0);
            LocalDateTime expiresAt = (LocalDateTime) session.get("expires_at");
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(expiresAt)) {
                // 如果已过期，则从数据库清理该会话
                String deleteSessionSql = "DELETE FROM user_sessions WHERE session_id = ?";
                jdbcTemplate.update(deleteSessionSql, session.get("session_id"));

                return ResponseEntity.ok(
                        new VerifyTokenResponse(false, "令牌已过期", new UserInfo(null, null, null, null, false))
                );
            }

            // 4. 封装成功结果
            Integer userId = (Integer) session.get("user_id");
            String username = (String) session.get("username");
            String email = (String) session.get("email");
            String role = (String) session.get("role");

            UserInfo userInfo = new UserInfo(userId, username, email, role, true);
            VerifyTokenResponse response = new VerifyTokenResponse(true, "令牌有效", userInfo);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new VerifyTokenResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    /**
     * 验证令牌接口（通过 Authorization 请求头传参）
     * 这种方式更符合 RESTful 规范。
     */
    @GetMapping("/verify-token-header")
    public ResponseEntity<VerifyTokenResponse> verifyTokenHeader(HttpServletRequest httpRequest) {
        printRequest("通过请求头验证令牌");

        try {
            // 1. 提取 Token
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.ok(
                        new VerifyTokenResponse(false, "未提供有效的认证令牌", new UserInfo(null, null, null, null, false))
                );
            }

            String token = authHeader.substring(7);

            // 2. 数据库查询
            String findSessionSql = "SELECT s.session_id, s.user_id, s.expires_at, u.username, u.email, u.role " +
                    "FROM user_sessions s " +
                    "JOIN users u ON s.user_id = u.user_id " +
                    "WHERE s.access_token = ?";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(findSessionSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.ok(
                        new VerifyTokenResponse(false, "令牌无效", new UserInfo(null, null, null, null, false))
                );
            }

            // 3. 过期检查
            Map<String, Object> session = sessions.get(0);
            LocalDateTime expiresAt = (LocalDateTime) session.get("expires_at");
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(expiresAt)) {
                String deleteSessionSql = "DELETE FROM user_sessions WHERE session_id = ?";
                jdbcTemplate.update(deleteSessionSql, session.get("session_id"));

                return ResponseEntity.ok(
                        new VerifyTokenResponse(false, "令牌已过期", new UserInfo(null, null, null, null, false))
                );
            }

            // 4. 封装结果
            Integer userId = (Integer) session.get("user_id");
            String username = (String) session.get("username");
            String email = (String) session.get("email");
            String role = (String) session.get("role");

            UserInfo userInfo = new UserInfo(userId, username, email, role, true);
            VerifyTokenResponse response = new VerifyTokenResponse(true, "令牌有效", userInfo);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new VerifyTokenResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}