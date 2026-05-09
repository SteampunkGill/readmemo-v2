package com.vue.readingapp.user;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
public class UserUpdatePreferences {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到更新偏好设置请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=================");
    }

    // 打印查询结果
    private void printQueryResult(Object result) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("查询结果: " + result);
        System.out.println("===================");
    }

    // 打印返回数据
    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 请求DTO
    public static class UpdatePreferencesRequest {
        private ReadingPreferences reading;
        private ReviewPreferences review;
        private NotificationPreferences notification;

        public ReadingPreferences getReading() { return reading; }
        public void setReading(ReadingPreferences reading) { this.reading = reading; }

        public ReviewPreferences getReview() { return review; }
        public void setReview(ReviewPreferences review) { this.review = review; }

        public NotificationPreferences getNotification() { return notification; }
        public void setNotification(NotificationPreferences notification) { this.notification = notification; }
    }

    public static class ReadingPreferences {
        private Integer fontSize;
        private String theme;
        private Double lineHeight;

        public Integer getFontSize() { return fontSize; }
        public void setFontSize(Integer fontSize) { this.fontSize = fontSize; }

        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }

        public Double getLineHeight() { return lineHeight; }
        public void setLineHeight(Double lineHeight) { this.lineHeight = lineHeight; }
    }

    public static class ReviewPreferences {
        private Integer dailyGoal;
        private String reminderTime;

        public Integer getDailyGoal() { return dailyGoal; }
        public void setDailyGoal(Integer dailyGoal) { this.dailyGoal = dailyGoal; }

        public String getReminderTime() { return reminderTime; }
        public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }
    }

    public static class NotificationPreferences {
        private Boolean email;
        private Boolean push;

        public Boolean getEmail() { return email; }
        public void setEmail(Boolean email) { this.email = email; }

        public Boolean getPush() { return push; }
        public void setPush(Boolean push) { this.push = push; }
    }

    // 用户数据DTO（内联于此）
    public static class UserData {
        private int id;
        private String username;
        private String email;
        private String nickname;
        private String avatar;
        private String bio;
        private String location;
        private String website;
        private java.time.LocalDateTime joinDate;
        private java.time.LocalDateTime lastLogin;
        private boolean isVerified;
        private Preferences preferences;
        private PrivacySettings privacySettings;

        public UserData(int id, String username, String email, String nickname, String avatar,
                        String bio, String location, String website, java.time.LocalDateTime joinDate,
                        java.time.LocalDateTime lastLogin, boolean isVerified) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.nickname = nickname;
            this.avatar = avatar;
            this.bio = bio;
            this.location = location;
            this.website = website;
            this.joinDate = joinDate;
            this.lastLogin = lastLogin;
            this.isVerified = isVerified;
            this.preferences = new Preferences();
            this.privacySettings = new PrivacySettings();
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }

        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }

        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getWebsite() { return website; }
        public void setWebsite(String website) { this.website = website; }

        public java.time.LocalDateTime getJoinDate() { return joinDate; }
        public void setJoinDate(java.time.LocalDateTime joinDate) { this.joinDate = joinDate; }

        public java.time.LocalDateTime getLastLogin() { return lastLogin; }
        public void setLastLogin(java.time.LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

        public boolean isVerified() { return isVerified; }
        public void setVerified(boolean verified) { isVerified = verified; }

        public Preferences getPreferences() { return preferences; }
        public void setPreferences(Preferences preferences) { this.preferences = preferences; }

        public PrivacySettings getPrivacySettings() { return privacySettings; }
        public void setPrivacySettings(PrivacySettings privacySettings) { this.privacySettings = privacySettings; }
    }

    public static class Preferences {
        private Reading reading = new Reading();
        private Review review = new Review();
        private Notification notification = new Notification();

        public Reading getReading() { return reading; }
        public void setReading(Reading reading) { this.reading = reading; }

        public Review getReview() { return review; }
        public void setReview(Review review) { this.review = review; }

        public Notification getNotification() { return notification; }
        public void setNotification(Notification notification) { this.notification = notification; }
    }

    public static class Reading {
        private int fontSize = 16;
        private String theme = "light";
        private double lineHeight = 1.6;

        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }

        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }

        public double getLineHeight() { return lineHeight; }
        public void setLineHeight(double lineHeight) { this.lineHeight = lineHeight; }
    }

    public static class Review {
        private int dailyGoal = 20;
        private String reminderTime = "20:00";

        public int getDailyGoal() { return dailyGoal; }
        public void setDailyGoal(int dailyGoal) { this.dailyGoal = dailyGoal; }

        public String getReminderTime() { return reminderTime; }
        public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }
    }

    public static class Notification {
        private boolean email = true;
        private boolean push = true;

        public boolean isEmail() { return email; }
        public void setEmail(boolean email) { this.email = email; }

        public boolean isPush() { return push; }
        public void setPush(boolean push) { this.push = push; }
    }

    public static class PrivacySettings {
        private String profileVisibility = "public";
        private String activityVisibility = "friends";
        private String dataSharing = "limited";

        public String getProfileVisibility() { return profileVisibility; }
        public void setProfileVisibility(String profileVisibility) { this.profileVisibility = profileVisibility; }

        public String getActivityVisibility() { return activityVisibility; }
        public void setActivityVisibility(String activityVisibility) { this.activityVisibility = activityVisibility; }

        public String getDataSharing() { return dataSharing; }
        public void setDataSharing(String dataSharing) { this.dataSharing = dataSharing; }
    }

    // 响应DTO
    public static class UpdatePreferencesResponse {
        private boolean success;
        private String message;
        private UserData user;

        public UpdatePreferencesResponse(boolean success, String message, UserData user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UserData getUser() { return user; }
        public void setUser(UserData user) { this.user = user; }
    }

    @PutMapping("/preferences")
    public ResponseEntity<UpdatePreferencesResponse> updateUserPreferences(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdatePreferencesRequest request) {

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdatePreferencesResponse(false, "请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 验证token有效性
            String tokenSql = "SELECT user_id, expires_at FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdatePreferencesResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            int userId = (int) session.get("user_id");

            // 3. 验证请求数据
            if (request == null || (request.getReading() == null && request.getReview() == null && request.getNotification() == null)) {
                return ResponseEntity.badRequest().body(
                        new UpdatePreferencesResponse(false, "偏好设置数据不能为空", null)
                );
            }

            // 4. 查询现有的用户偏好设置
            String userSql = "SELECT preferences FROM users WHERE user_id = ?";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, userId);

            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdatePreferencesResponse(false, "用户不存在", null)
                );
            }

            Map<String, Object> user = users.get(0);
            String existingPreferencesJson = (String) user.get("preferences");

            // 5. 解析现有的偏好设置（简化处理，实际应该使用JSON解析）
            // 这里我们假设preferences字段存储的是JSON字符串
            // 由于是课设，我们简化处理，直接构建新的JSON

            // 6. 构建新的偏好设置JSON
            Map<String, Object> newPreferences = new HashMap<>();

            // 6.1 阅读偏好
            Map<String, Object> readingPrefs = new HashMap<>();
            if (request.getReading() != null) {
                if (request.getReading().getFontSize() != null) {
                    readingPrefs.put("fontSize", request.getReading().getFontSize());
                } else {
                    readingPrefs.put("fontSize", 16);
                }

                if (request.getReading().getTheme() != null) {
                    readingPrefs.put("theme", request.getReading().getTheme());
                } else {
                    readingPrefs.put("theme", "light");
                }

                if (request.getReading().getLineHeight() != null) {
                    readingPrefs.put("lineHeight", request.getReading().getLineHeight());
                } else {
                    readingPrefs.put("lineHeight", 1.6);
                }
            } else {
                // 使用默认值
                readingPrefs.put("fontSize", 16);
                readingPrefs.put("theme", "light");
                readingPrefs.put("lineHeight", 1.6);
            }
            newPreferences.put("reading", readingPrefs);

            // 6.2 复习偏好
            Map<String, Object> reviewPrefs = new HashMap<>();
            if (request.getReview() != null) {
                if (request.getReview().getDailyGoal() != null) {
                    reviewPrefs.put("dailyGoal", request.getReview().getDailyGoal());
                } else {
                    reviewPrefs.put("dailyGoal", 20);
                }

                if (request.getReview().getReminderTime() != null) {
                    reviewPrefs.put("reminderTime", request.getReview().getReminderTime());
                } else {
                    reviewPrefs.put("reminderTime", "20:00");
                }
            } else {
                // 使用默认值
                reviewPrefs.put("dailyGoal", 20);
                reviewPrefs.put("reminderTime", "20:00");
            }
            newPreferences.put("review", reviewPrefs);

            // 6.3 通知偏好
            Map<String, Object> notificationPrefs = new HashMap<>();
            if (request.getNotification() != null) {
                if (request.getNotification().getEmail() != null) {
                    notificationPrefs.put("email", request.getNotification().getEmail());
                } else {
                    notificationPrefs.put("email", true);
                }

                if (request.getNotification().getPush() != null) {
                    notificationPrefs.put("push", request.getNotification().getPush());
                } else {
                    notificationPrefs.put("push", true);
                }
            } else {
                // 使用默认值
                notificationPrefs.put("email", true);
                notificationPrefs.put("push", true);
            }
            newPreferences.put("notification", notificationPrefs);

            // 7. 更新用户偏好设置
            // 使用 Jackson 将 Map 转换为真正的 JSON 字符串
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String newPreferencesJson = mapper.writeValueAsString(newPreferences);

            String updateSql = "UPDATE users SET preferences = ? WHERE user_id = ?";
            int rowsUpdated = jdbcTemplate.update(updateSql, newPreferencesJson, userId);

            printQueryResult("更新偏好设置行数: " + rowsUpdated);

            if (rowsUpdated == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdatePreferencesResponse(false, "用户不存在", null)
                );
            }

            // 8. 查询更新后的用户信息（确保日期字段以LocalDateTime类型返回）
            String updatedUserSql = "SELECT user_id, username, email, nickname, avatar_url, bio, location, website, " +
                    "is_verified, CAST(created_at AS DATETIME) as created_at, CAST(last_login_at AS DATETIME) as last_login_at FROM users WHERE user_id = ?";
            
            List<Map<String, Object>> updatedUsers = jdbcTemplate.queryForList(updatedUserSql, userId);

            if (updatedUsers.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdatePreferencesResponse(false, "用户不存在", null)
                );
            }

            Map<String, Object> updatedUser = updatedUsers.get(0);

            // 9. 格式化用户信息（处理 Timestamp 到 LocalDateTime 的转换）
            Object createdAtObj = updatedUser.get("created_at");
            Object lastLoginObj = updatedUser.get("last_login_at");
            
            LocalDateTime createdAt = null;
            if (createdAtObj instanceof java.sql.Timestamp) {
                createdAt = ((java.sql.Timestamp) createdAtObj).toLocalDateTime();
            } else if (createdAtObj instanceof LocalDateTime) {
                createdAt = (LocalDateTime) createdAtObj;
            } else if (createdAtObj instanceof String) {
                createdAt = LocalDateTime.parse(((String) createdAtObj).replace(" ", "T"));
            }

            LocalDateTime lastLoginAt = null;
            if (lastLoginObj instanceof java.sql.Timestamp) {
                lastLoginAt = ((java.sql.Timestamp) lastLoginObj).toLocalDateTime();
            } else if (lastLoginObj instanceof LocalDateTime) {
                lastLoginAt = (LocalDateTime) lastLoginObj;
            } else if (lastLoginObj instanceof String) {
                lastLoginAt = LocalDateTime.parse(((String) lastLoginObj).replace(" ", "T"));
            }
            
            UserData userData = new UserData(
                    (int) updatedUser.get("user_id"),
                    (String) updatedUser.get("username"),
                    (String) updatedUser.get("email"),
                    updatedUser.get("nickname") != null ? (String) updatedUser.get("nickname") : (String) updatedUser.get("username"),
                    updatedUser.get("avatar_url") != null ? (String) updatedUser.get("avatar_url") : "",
                    updatedUser.get("bio") != null ? (String) updatedUser.get("bio") : "",
                    updatedUser.get("location") != null ? (String) updatedUser.get("location") : "",
                    updatedUser.get("website") != null ? (String) updatedUser.get("website") : "",
                    createdAt,
                    lastLoginAt,
                    updatedUser.get("is_verified") != null && (boolean) updatedUser.get("is_verified")
            );

            // 10. 准备响应数据
            UpdatePreferencesResponse response = new UpdatePreferencesResponse(true, "偏好设置已更新", userData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新用户偏好设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdatePreferencesResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
