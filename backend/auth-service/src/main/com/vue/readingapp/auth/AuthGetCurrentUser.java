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
 * 获取当前用户信息控制器
 * 
 * 负责根据请求头中的 Token 识别当前登录用户，并返回其详细资料及个性化设置。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthGetCurrentUser {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 辅助方法：打印获取用户信息请求日志
     */
    private void printRequest(Object request) {
        System.out.println("=== 收到获取当前用户请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=======================");
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
        if (value instanceof java.sql.Timestamp) return ((java.sql.Timestamp) value).toLocalDateTime();
        return null;
    }

    /**
     * 获取当前登录用户信息接口
     * 
     * @param httpRequest 用于提取 Authorization 头
     * @return 当前用户的详细信息及偏好设置
     */
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> getCurrentUser(HttpServletRequest httpRequest) {
        printRequest("获取当前用户信息");

        try {
            // 1. 身份识别：从请求头获取 Bearer Token
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new CurrentUserResponse(false, "未提供有效的认证令牌", null)
                );
            }

            String token = authHeader.substring(7);

            // 2. 会话验证：检查 Token 是否存在且未过期
            String findSessionSql = "SELECT s.user_id FROM user_sessions s WHERE s.access_token = ? AND s.expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(findSessionSql, token);
            printQueryResult(sessions);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new CurrentUserResponse(false, "令牌无效或已过期", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 用户资料查询：获取用户的基本信息
            String userSql = "SELECT user_id, username, email, nickname, avatar_url, role, is_verified, created_at, last_login_at " +
                    "FROM users WHERE user_id = ?";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, userId);

            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new CurrentUserResponse(false, "用户不存在", null)
                );
            }

            Map<String, Object> user = users.get(0);
            LocalDateTime createdAt = convertToLocalDateTime(user.get("created_at"));
            LocalDateTime lastLoginAt = convertToLocalDateTime(user.get("last_login_at"));

            // 4. 偏好设置查询：从 user_settings 表中加载用户的个性化配置
            Preferences preferences = new Preferences();
            try {
                String settingsSql = "SELECT setting_key, setting_value FROM user_settings WHERE user_id = ?";
                List<Map<String, Object>> settings = jdbcTemplate.queryForList(settingsSql, userId);

                for (Map<String, Object> setting : settings) {
                    String key = (String) setting.get("setting_key");
                    String value = (String) setting.get("setting_value");
                    if (key == null || value == null) continue;

                    // 根据不同的 Key 填充偏好设置对象
                    switch (key) {
                        case "theme": preferences.setTheme(value); break;
                        case "language": preferences.setLanguage(value); break;
                        case "notification_enabled": preferences.setNotificationEnabled(Boolean.parseBoolean(value)); break;
                        case "auto_save": preferences.setAutoSave(Boolean.parseBoolean(value)); break;
                        case "reading_mode": preferences.setReadingMode(value); break;
                        case "font_size": 
                            try { preferences.setFontSize(Integer.parseInt(value)); } catch (Exception e) {}
                            break;
                        case "line_height":
                            try { preferences.setLineHeight(Double.parseDouble(value)); } catch (Exception e) {}
                            break;
                    }
                }
            } catch (Exception e) {
                System.out.println("获取用户设置失败: " + e.getMessage());
            }

            // 5. 封装 DTO 对象并返回
            CurrentUserDTO currentUser = new CurrentUserDTO(
                    (Integer) user.get("user_id"),
                    (String) user.get("username"),
                    (String) user.get("email"),
                    (String) user.get("nickname"),
                    (String) user.get("avatar_url"),
                    (String) user.get("role"),
                    (Boolean) user.get("is_verified"),
                    createdAt,
                    lastLoginAt,
                    preferences
            );

            CurrentUserData userData = new CurrentUserData(currentUser);
            CurrentUserResponse response = new CurrentUserResponse(true, "获取用户信息成功", userData);

            printResponse(response);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CurrentUserResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    /**
     * 获取当前用户响应的统一格式
     */
    public static class CurrentUserResponse {
        private boolean success;
        private String message;
        private CurrentUserData data;

        public CurrentUserResponse() {}
        public CurrentUserResponse(boolean success, String message, CurrentUserData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public CurrentUserData getData() { return data; }
        public void setData(CurrentUserData data) { this.data = data; }
    }

    /**
     * 响应中的数据包装类
     */
    public static class CurrentUserData {
        private CurrentUserDTO currentUser;
        private CurrentUserDTO user; // 兼容旧版前端的字段

        public CurrentUserData() {}
        public CurrentUserData(CurrentUserDTO currentUser) {
            this.currentUser = currentUser;
            this.user = currentUser;
        }
        public CurrentUserDTO getCurrentUser() { return currentUser; }
        public void setCurrentUser(CurrentUserDTO currentUser) { 
            this.currentUser = currentUser; 
            this.user = currentUser;
        }
        public CurrentUserDTO getUser() { return user; }
        public void setUser(CurrentUserDTO user) { 
            this.user = user; 
            this.currentUser = user;
        }
    }

    /**
     * 用户详细资料传输对象
     */
    public static class CurrentUserDTO {
        private Integer id;
        private Integer userId; // 兼容性字段
        private String username;
        private String email;
        private String nickname;
        private String avatarUrl;
        private String role;
        private Boolean isVerified;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;
        private Preferences preferences;

        public CurrentUserDTO() {}
        public CurrentUserDTO(Integer id, String username, String email, String nickname, String avatarUrl,
                              String role, Boolean isVerified, LocalDateTime createdAt, LocalDateTime lastLoginAt,
                              Preferences preferences) {
            this.id = id;
            this.userId = id;
            this.username = username;
            this.email = email;
            this.nickname = nickname;
            this.avatarUrl = avatarUrl;
            this.role = role;
            this.isVerified = isVerified;
            this.createdAt = createdAt;
            this.lastLoginAt = lastLoginAt;
            this.preferences = preferences;
        }

        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; this.userId = id; }
        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; this.id = userId; }
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
        public Preferences getPreferences() { return preferences; }
        public void setPreferences(Preferences preferences) { this.preferences = preferences; }
    }

    /**
     * 用户个性化偏好设置类
     */
    public static class Preferences {
        private String theme;               // 主题（深色/浅色）
        private String language;            // 界面语言
        private Boolean notificationEnabled;// 是否开启通知
        private Boolean autoSave;           // 是否自动保存阅读进度
        private String readingMode;         // 阅读模式（翻页/滚动）
        private Integer fontSize;           // 阅读字体大小
        private Double lineHeight;          // 阅读行高

        public Preferences() {}
        // Getters and Setters
        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public Boolean getNotificationEnabled() { return notificationEnabled; }
        public void setNotificationEnabled(Boolean notificationEnabled) { this.notificationEnabled = notificationEnabled; }
        public Boolean getAutoSave() { return autoSave; }
        public void setAutoSave(Boolean autoSave) { this.autoSave = autoSave; }
        public String getReadingMode() { return readingMode; }
        public void setReadingMode(String readingMode) { this.readingMode = readingMode; }
        public Integer getFontSize() { return fontSize; }
        public void setFontSize(Integer fontSize) { this.fontSize = fontSize; }
        public Double getLineHeight() { return lineHeight; }
        public void setLineHeight(Double lineHeight) { this.lineHeight = lineHeight; }
    }
}