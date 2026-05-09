
package com.vue.readingapp.user;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
public class UserGetProfile {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取用户信息请求 ===");
        System.out.println("请求头信息");
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

    // 响应DTO
    public static class UserProfileResponse {
        private boolean success;
        private String message;
        private UserData user;

        public UserProfileResponse(boolean success, String message, UserData user) {
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

    public static class UserData {
        private int id;
        private String username;
        private String email;
        private String nickname;
        private String avatar;
        private String bio;
        private String location;
        private String website;
        private LocalDateTime joinDate;
        private LocalDateTime lastLogin;
        private boolean isVerified;
        private Preferences preferences;
        private PrivacySettings privacySettings;

        public UserData(int id, String username, String email, String nickname, String avatar,
                        String bio, String location, String website, LocalDateTime joinDate,
                        LocalDateTime lastLogin, boolean isVerified) {
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

        public LocalDateTime getJoinDate() { return joinDate; }
        public void setJoinDate(LocalDateTime joinDate) { this.joinDate = joinDate; }

        public LocalDateTime getLastLogin() { return lastLogin; }
        public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

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

    private LocalDateTime convertToLocalDateTime(Object obj) {
        if (obj instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) obj).toLocalDateTime();
        } else if (obj instanceof LocalDateTime) {
            return (LocalDateTime) obj;
        }
        return null;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 打印接收到的请求
        printRequest("Authorization: " + authHeader);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UserProfileResponse(false, "请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 验证token有效性
            String tokenSql = "SELECT user_id, expires_at FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UserProfileResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            int userId = ((Number) session.get("user_id")).intValue();

            // 3. 查询用户信息
            String userSql = "SELECT user_id, username, email, nickname, avatar_url, bio, location, website, " +
                    "is_verified, created_at, last_login_at FROM users WHERE user_id = ?";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, userId);
            printQueryResult(users);

            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UserProfileResponse(false, "用户不存在", null)
                );
            }

            Map<String, Object> user = users.get(0);

            // 4. 格式化用户信息（处理 Timestamp 到 LocalDateTime 的转换）
            UserData userData = new UserData(
                    (int) user.get("user_id"),
                    (String) user.get("username"),
                    (String) user.get("email"),
                    user.get("nickname") != null ? (String) user.get("nickname") : (String) user.get("username"),
                    user.get("avatar_url") != null ? (String) user.get("avatar_url") : "",
                    "", // bio (暂时设为空，因为数据库字段可能缺失)
                    "", // location
                    "", // website
                    convertToLocalDateTime(user.get("created_at")),
                    convertToLocalDateTime(user.get("last_login_at")),
                    user.get("is_verified") != null && (user.get("is_verified") instanceof Boolean ? (Boolean) user.get("is_verified") : ((Number) user.get("is_verified")).intValue() == 1)
            );

            // 5. 准备响应数据
            UserProfileResponse response = new UserProfileResponse(true, "获取用户信息成功", userData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取用户信息过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UserProfileResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}