package com.vue.readingapp.auth;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.sql.ResultSet; 
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * 用户登录控制器
 * 
 * 负责处理用户的登录请求。它的核心任务是：
 * 1. 接收用户输入的账号（邮箱或用户名）和密码。
 * 2. 在数据库中查找该用户。
 * 3. 验证密码是否正确。
 * 4. 如果验证通过，生成一个“通行证”（Token）返回给前端。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthLogin {

    @Autowired
    private JdbcTemplate jdbcTemplate; // 用于执行 SQL 语句操作数据库

    /**
     * 辅助方法：在控制台打印登录请求数据，方便调试
     */
    private void printRequest(Object request) {
        System.out.println("=== 收到登录请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=================");
    }

    /**
     * 辅助方法：打印数据库查询到的原始结果
     */
    private void printQueryResult(Object result) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("查询结果: " + result);
        System.out.println("===================");
    }

    /**
     * 辅助方法：打印准备返回给前端的响应数据
     */
    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    /**
     * 登录接口
     * 
     * @param request 包含用户输入的 email 和 password 的对象
     * @return 返回登录结果（成功或失败的信息，以及成功时的 Token）
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        printRequest(request);

        try {
            // 1. 基础检查：确保用户填了账号和密码
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new LoginResponse(false, "邮箱或用户名不能为空", null)
                );
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new LoginResponse(false, "密码不能为空", null)
                );
            }

            // 2. 数据库查找：根据邮箱或用户名搜索用户
            // SQL 解释：从 users 表中查找 email 等于输入值，或者 username 等于输入值的记录
            String sql = "SELECT user_id, username, email, password_hash, nickname, avatar_url, created_at, last_login_at " +
                    "FROM users WHERE email = ? OR username = ?";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, request.getEmail(), request.getEmail());
            printQueryResult(users);

            // 如果列表为空，说明账号不存在
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new LoginResponse(false, "邮箱/用户名或密码错误", null)
                );
            }

            // 获取查询到的第一个用户（账号应该是唯一的）
            Map<String, Object> user = users.get(0);

            // 3. 密码比对：验证输入的密码是否与数据库中存储的哈希值匹配
            String passwordHash = (String) user.get("password_hash");
            boolean passwordValid = false;
            
            // 检查存储的是否是加密后的密码（BCrypt 格式）
            if (passwordHash.startsWith("$2a$") || passwordHash.startsWith("$2b$") || passwordHash.startsWith("$2y$")) {
                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                // matches 方法会自动处理加密比对逻辑
                passwordValid = passwordEncoder.matches(request.getPassword(), passwordHash);
            } else {
                // 如果是明文（仅用于开发测试），直接比较字符串
                passwordValid = passwordHash.equals(request.getPassword());
            }
            
            if (!passwordValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new LoginResponse(false, "邮箱/用户名或密码错误", null)
                );
            }

            // 4. 安全清理：登录前先删除该用户旧的会话记录，防止重复登录或残留数据
            String deleteSessionSql = "DELETE FROM user_sessions WHERE user_id = ?";
            jdbcTemplate.update(deleteSessionSql, user.get("user_id"));

            // 5. 令牌生成：创建一个唯一的随机字符串作为用户的“通行证”
            // UUID 会生成类似 "550e8400-e29b-41d4-a716-446655440000" 的唯一标识
            String accessToken = "access_" + UUID.randomUUID().toString();
            String refreshToken = "refresh_" + UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();
            // 设置有效期为 1 小时（3600 秒）
            int expiresInSeconds = 3600;

            // 6. 存储会话：将生成的 Token 存入数据库，以便后续接口验证用户身份
            String insertSessionSql = "INSERT INTO user_sessions (user_id, access_token, refresh_token, expires_at, created_at) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(insertSessionSql,
                    user.get("user_id"),
                    accessToken,
                    refreshToken,
                    now.plusSeconds(expiresInSeconds),
                    now
            );

            // 7. 记录足迹：更新用户的最后登录时间
            String updateUserSql = "UPDATE users SET last_login_at = ? WHERE user_id = ?";
            jdbcTemplate.update(updateUserSql, now, user.get("user_id"));

            // 8. 成功返回：将 Token 发送给前端，前端会将其保存在本地（如 LocalStorage）
            LoginData loginData = new LoginData(accessToken, refreshToken, expiresInSeconds);
            LoginResponse response = new LoginResponse(true, "登录成功", loginData);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // 异常处理：如果程序运行出错，打印错误并告知前端
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new LoginResponse(false, "服务器内部错误", null)
            );
        }
    }

    /**
     * 登录请求的数据模型（DTO）
     */
    public static class LoginRequest {
        private String email;    // 账号（邮箱或用户名）
        private String password; // 密码

        public LoginRequest() {}

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * 登录响应的统一包装类
     */
    public static class LoginResponse {
        private boolean success; // 是否成功
        private String message;  // 提示消息
        private LoginData data;  // 成功时的具体数据

        public LoginResponse() {}

        public LoginResponse(boolean success, String message, LoginData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public LoginData getData() { return data; }
        public void setData(LoginData data) { this.data = data; }
    }

    /**
     * 登录成功后返回的令牌数据
     */
    public static class LoginData {
        private String accessToken;  // 访问令牌（用于调用其他接口）
        private String refreshToken; // 刷新令牌（用于续期）
        private int expiresIn;       // 有效时长（秒）

        public LoginData() {}

        public LoginData(String accessToken, String refreshToken, int expiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

        public int getExpiresIn() { return expiresIn; }
        public void setExpiresIn(int expiresIn) { this.expiresIn = expiresIn; }
    }
}