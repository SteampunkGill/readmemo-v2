package com.vue.readingapp.user;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.io.IOException;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/user")
public class UserUpdateAvatar {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到更新头像请求 ===");
        System.out.println("请求信息: " + request);
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
    public static class UpdateAvatarResponse {
        private boolean success;
        private String message;
        private UserData user;

        public UpdateAvatarResponse(boolean success, String message, UserData user) {
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

    @PostMapping("/avatar")
    public ResponseEntity<UpdateAvatarResponse> updateAvatar(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam("avatar") MultipartFile file) {

        // 打印接收到的请求
        printRequest("文件大小: " + file.getSize() + " bytes, 文件类型: " + file.getContentType());

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateAvatarResponse(false, "请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 验证token有效性
            String tokenSql = "SELECT user_id, expires_at FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateAvatarResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            int userId = (int) session.get("user_id");

            // 3. 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UpdateAvatarResponse(false, "请选择头像文件", null)
                );
            }

            // 验证文件大小（5MB限制）
            long maxSize = 5 * 1024 * 1024; // 5MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest().body(
                        new UpdateAvatarResponse(false, "图片文件太大，请选择小于5MB的图片", null)
                );
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null ||
                    (!contentType.equals("image/jpeg") &&
                            !contentType.equals("image/png") &&
                            !contentType.equals("image/gif"))) {
                return ResponseEntity.badRequest().body(
                        new UpdateAvatarResponse(false, "图片格式不支持，请选择JPG、PNG或GIF图片", null)
                );
            }

            // 4. 将图片转换为Base64字符串
            byte[] fileBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(fileBytes);

            // 构建完整的Data URL
            String avatarUrl = "data:" + contentType + ";base64," + base64Image;

            // 5. 更新用户头像
            String updateSql = "UPDATE users SET avatar_url = ? WHERE user_id = ?";
            int rowsUpdated = jdbcTemplate.update(updateSql, avatarUrl, userId);

            printQueryResult("更新头像行数: " + rowsUpdated);

            if (rowsUpdated == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdateAvatarResponse(false, "用户不存在", null)
                );
            }

            // 6. 查询更新后的用户信息
            String userSql = "SELECT user_id, username, email, nickname, avatar_url, bio, location, website, " +
                    "is_verified, created_at, last_login_at FROM users WHERE user_id = ?";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, userId);

            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdateAvatarResponse(false, "用户不存在", null)
                );
            }

            Map<String, Object> user = users.get(0);

            // 7. 格式化用户信息（保持LocalDateTime类型）
            UserData userData = new UserData(
                    (int) user.get("user_id"),
                    (String) user.get("username"),
                    (String) user.get("email"),
                    user.get("nickname") != null ? (String) user.get("nickname") : (String) user.get("username"),
                    user.get("avatar_url") != null ? (String) user.get("avatar_url") : "",
                    user.get("bio") != null ? (String) user.get("bio") : "",
                    user.get("location") != null ? (String) user.get("location") : "",
                    user.get("website") != null ? (String) user.get("website") : "",
                    (LocalDateTime) user.get("created_at"),
                    (LocalDateTime) user.get("last_login_at"),
                    user.get("is_verified") != null && (boolean) user.get("is_verified")
            );

            // 8. 准备响应数据
            UpdateAvatarResponse response = new UpdateAvatarResponse(true, "头像更新成功", userData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            System.err.println("读取图片文件过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateAvatarResponse(false, "读取图片文件失败: " + e.getMessage(), null)
            );
        } catch (Exception e) {
            System.err.println("更新头像过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateAvatarResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
