package com.vue.readingapp.auth;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * 用户注册控制器
 * 
 * 负责处理新用户的注册流程，包括信息验证、查重、数据持久化以及注册后的自动登录。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthRegister {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 用于密码加密的工具类
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 辅助方法：打印注册请求数据
     */
    private void printRequest(Object request) {
        System.out.println("=== 收到注册请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=================");
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
     * 用户注册接口
     * 
     * @param request 包含用户名、邮箱、密码等信息的请求体
     * @return 注册结果及用户信息
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        // 记录请求日志
        printRequest(request);

        try {
            // 1. 基础数据验证：确保必填项不为空
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new RegisterResponse(false, "邮箱不能为空", null)
                );
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new RegisterResponse(false, "密码不能为空", null)
                );
            }

            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new RegisterResponse(false, "用户名不能为空", null)
                );
            }

            // 2. 格式验证：简单检查邮箱是否包含 @ 符号
            if (!request.getEmail().contains("@")) {
                return ResponseEntity.badRequest().body(
                        new RegisterResponse(false, "邮箱格式不正确", null)
                );
            }

            // 3. 安全验证：限制密码最短长度
            if (request.getPassword().length() < 6) {
                return ResponseEntity.badRequest().body(
                        new RegisterResponse(false, "密码长度不能少于6位", null)
                );
            }

            // 4. 唯一性检查：检查邮箱是否已被注册
            String checkEmailSql = "SELECT COUNT(*) FROM users WHERE email = ?";
            Integer emailCount = jdbcTemplate.queryForObject(checkEmailSql, Integer.class, request.getEmail());

            if (emailCount > 0) {
                return ResponseEntity.badRequest().body(
                        new RegisterResponse(false, "该邮箱已被注册", null)
                );
            }

            // 5. 唯一性检查：检查用户名是否已被占用
            String checkUsernameSql = "SELECT COUNT(*) FROM users WHERE username = ?";
            Integer usernameCount = jdbcTemplate.queryForObject(checkUsernameSql, Integer.class, request.getUsername());

            if (usernameCount > 0) {
                return ResponseEntity.badRequest().body(
                        new RegisterResponse(false, "该用户名已被使用", null)
                );
            }

            // 6. 默认值处理：如果没有提供昵称，则默认使用用户名
            String nickname = request.getNickname();
            if (nickname == null || nickname.trim().isEmpty()) {
                nickname = request.getUsername();
            }

            // 7. 执行用户创建（插入数据库）
            LocalDateTime now = LocalDateTime.now();
            String insertUserSql = "INSERT INTO users (username, email, password_hash, nickname, avatar_url, role, is_verified, created_at, last_login_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            // 注意：在实际生产环境中，必须使用 passwordEncoder.encode(request.getPassword()) 加密密码
            String passwordHash = request.getPassword(); 
            String avatarUrl = "";
            String role = "user";
            Boolean isVerified = false;

            jdbcTemplate.update(insertUserSql,
                    request.getUsername(),
                    request.getEmail(),
                    passwordHash,
                    nickname,
                    avatarUrl,
                    role,
                    isVerified,
                    now,
                    now
            );

            // 8. 获取新创建的用户 ID（用于后续生成会话）
            String getUserIdSql = "SELECT user_id FROM users WHERE email = ?";
            Integer userId = jdbcTemplate.queryForObject(getUserIdSql, Integer.class, request.getEmail());

            // 9. 自动登录：为新注册用户创建登录会话 (Token)
            String accessToken = "access_" + UUID.randomUUID().toString();
            String refreshToken = "refresh_" + UUID.randomUUID().toString();

            String insertSessionSql = "INSERT INTO user_sessions (user_id, access_token, refresh_token, expires_at, created_at) " +
                    "VALUES (?, ?, ?, ?, ?)";
            // 设置 Token 有效期为 1 小时
            jdbcTemplate.update(insertSessionSql,
                    userId,
                    accessToken,
                    refreshToken,
                    now.plusHours(1),
                    now
            );

            // 10. 封装返回给前端的用户数据
            User user = new User();
            user.setId(userId);
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setNickname(nickname);
            user.setAvatarUrl(avatarUrl);
            user.setRole(role);
            user.setIsVerified(isVerified);
            user.setCreatedAt(now);
            user.setLastLoginAt(now);
            user.setPasswordHash(passwordHash);
            user.setUpdatedAt(now);

            UserData userData = new UserData(user);
            RegisterResponse response = new RegisterResponse(true, "注册成功", userData);

            printResponse(response);

            // 返回 201 Created 状态码表示资源创建成功
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            // 异常处理：记录错误并返回 500 状态码
            System.err.println("注册过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new RegisterResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    /**
     * 注册请求的数据结构
     */
    public static class RegisterRequest {
        private String email;
        private String password;
        private String username;
        private String nickname;

        public RegisterRequest() {}

        public RegisterRequest(String email, String password, String username, String nickname) {
            this.email = email;
            this.password = password;
            this.username = username;
            this.nickname = nickname;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
    }

    /**
     * 注册响应的统一格式
     */
    public static class RegisterResponse {
        private boolean success;
        private String message;
        private UserData data;

        public RegisterResponse() {}

        public RegisterResponse(boolean success, String message, UserData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UserData getData() { return data; }
        public void setData(UserData data) { this.data = data; }
    }

    /**
     * 响应中的用户数据包装类
     */
    public static class UserData {
        private User user;

        public UserData() {}

        public UserData(User user) {
            this.user = user;
        }

        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }
    }

    /**
     * 用户实体类：对应数据库中的 users 表结构
     */
    public static class User {
        private Integer id;
        private String username;
        private String email;
        private String nickname;
        private String avatarUrl;
        private String role;
        private Boolean isVerified;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;
        private String passwordHash;
        private LocalDateTime emailVerifiedAt;
        private LocalDateTime updatedAt;
        private String bio;
        private String location;
        private String website;

        public User() {}

        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }

        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public Boolean getIsVerified() { return isVerified; }
        public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getLastLoginAt() { return lastLoginAt; }
        public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

        public String getPasswordHash() { return passwordHash; }
        public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

        public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
        public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getWebsite() { return website; }
        public void setWebsite(String website) { this.website = website; }
    }
}