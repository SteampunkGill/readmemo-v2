package com.vue.readingapp.settings;

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
@RequestMapping("/api/v1/user/settings/notifications")
public class SettingsUpdateNotification {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到更新通知设置请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("========================");
    }

    private void printQueryResult(Object result) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("查询结果: " + result);
        System.out.println("===================");
    }

    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    public static class UpdateNotificationRequest {
        private EmailNotificationsRequest emailNotifications;
        private PushNotificationsRequest pushNotifications;
        private InAppNotificationsRequest inAppNotifications;
        private QuietHoursRequest quietHours;

        // Getters and Setters
        public EmailNotificationsRequest getEmailNotifications() { return emailNotifications; }
        public void setEmailNotifications(EmailNotificationsRequest emailNotifications) { this.emailNotifications = emailNotifications; }

        public PushNotificationsRequest getPushNotifications() { return pushNotifications; }
        public void setPushNotifications(PushNotificationsRequest pushNotifications) { this.pushNotifications = pushNotifications; }

        public InAppNotificationsRequest getInAppNotifications() { return inAppNotifications; }
        public void setInAppNotifications(InAppNotificationsRequest inAppNotifications) { this.inAppNotifications = inAppNotifications; }

        public QuietHoursRequest getQuietHours() { return quietHours; }
        public void setQuietHours(QuietHoursRequest quietHours) { this.quietHours = quietHours; }
    }

    public static class EmailNotificationsRequest {
        private Boolean enabled;
        private Boolean dailyDigest;
        private Boolean weeklyReport;
        private Boolean achievementUnlocked;
        private Boolean reviewReminder;
        private Boolean systemUpdates;

        // Getters and Setters
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }

        public Boolean getDailyDigest() { return dailyDigest; }
        public void setDailyDigest(Boolean dailyDigest) { this.dailyDigest = dailyDigest; }

        public Boolean getWeeklyReport() { return weeklyReport; }
        public void setWeeklyReport(Boolean weeklyReport) { this.weeklyReport = weeklyReport; }

        public Boolean getAchievementUnlocked() { return achievementUnlocked; }
        public void setAchievementUnlocked(Boolean achievementUnlocked) { this.achievementUnlocked = achievementUnlocked; }

        public Boolean getReviewReminder() { return reviewReminder; }
        public void setReviewReminder(Boolean reviewReminder) { this.reviewReminder = reviewReminder; }

        public Boolean getSystemUpdates() { return systemUpdates; }
        public void setSystemUpdates(Boolean systemUpdates) { this.systemUpdates = systemUpdates; }
    }

    public static class PushNotificationsRequest {
        private Boolean enabled;
        private Boolean reviewReminder;
        private Boolean dailyGoalReminder;
        private Boolean streakReminder;
        private Boolean newFeature;
        private Boolean promotional;

        // Getters and Setters
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }

        public Boolean getReviewReminder() { return reviewReminder; }
        public void setReviewReminder(Boolean reviewReminder) { this.reviewReminder = reviewReminder; }

        public Boolean getDailyGoalReminder() { return dailyGoalReminder; }
        public void setDailyGoalReminder(Boolean dailyGoalReminder) { this.dailyGoalReminder = dailyGoalReminder; }

        public Boolean getStreakReminder() { return streakReminder; }
        public void setStreakReminder(Boolean streakReminder) { this.streakReminder = streakReminder; }

        public Boolean getNewFeature() { return newFeature; }
        public void setNewFeature(Boolean newFeature) { this.newFeature = newFeature; }

        public Boolean getPromotional() { return promotional; }
        public void setPromotional(Boolean promotional) { this.promotional = promotional; }
    }

    public static class InAppNotificationsRequest {
        private Boolean enabled;
        private Boolean showBadge;
        private Boolean soundEnabled;
        private Boolean vibrationEnabled;

        // Getters and Setters
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }

        public Boolean getShowBadge() { return showBadge; }
        public void setShowBadge(Boolean showBadge) { this.showBadge = showBadge; }

        public Boolean getSoundEnabled() { return soundEnabled; }
        public void setSoundEnabled(Boolean soundEnabled) { this.soundEnabled = soundEnabled; }

        public Boolean getVibrationEnabled() { return vibrationEnabled; }
        public void setVibrationEnabled(Boolean vibrationEnabled) { this.vibrationEnabled = vibrationEnabled; }
    }

    public static class QuietHoursRequest {
        private Boolean enabled;
        private String startTime;
        private String endTime;

        // Getters and Setters
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }

        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }

        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
    }

    public static class UpdateNotificationResponse {
        private boolean success;
        private String message;
        private SettingsGetNotification.NotificationSettingsData data;

        public UpdateNotificationResponse(boolean success, String message, SettingsGetNotification.NotificationSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SettingsGetNotification.NotificationSettingsData getData() { return data; }
        public void setData(SettingsGetNotification.NotificationSettingsData data) { this.data = data; }
    }

    @PutMapping("")
    public ResponseEntity<UpdateNotificationResponse> updateNotificationSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateNotificationRequest request) {

        try {
            printRequest(request);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateNotificationResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateNotificationResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 验证通知设置数据
            if (request.getQuietHours() != null) {
                QuietHoursRequest quietHours = request.getQuietHours();
                if (quietHours.getStartTime() != null && !isValidTimeFormat(quietHours.getStartTime())) {
                    return ResponseEntity.badRequest().body(
                            new UpdateNotificationResponse(false, "无效的静默时段开始时间格式", null)
                    );
                }
                if (quietHours.getEndTime() != null && !isValidTimeFormat(quietHours.getEndTime())) {
                    return ResponseEntity.badRequest().body(
                            new UpdateNotificationResponse(false, "无效的静默时段结束时间格式", null)
                    );
                }
            }

            // 4. 更新通知设置
            LocalDateTime now = LocalDateTime.now();

            // 更新邮件通知设置
            if (request.getEmailNotifications() != null) {
                EmailNotificationsRequest email = request.getEmailNotifications();
                updateNotificationSetting(userId, "email_enabled", email.getEnabled() != null ? email.getEnabled().toString() : null, now);
                updateNotificationSetting(userId, "email_dailyDigest", email.getDailyDigest() != null ? email.getDailyDigest().toString() : null, now);
                updateNotificationSetting(userId, "email_weeklyReport", email.getWeeklyReport() != null ? email.getWeeklyReport().toString() : null, now);
                updateNotificationSetting(userId, "email_achievementUnlocked", email.getAchievementUnlocked() != null ? email.getAchievementUnlocked().toString() : null, now);
                updateNotificationSetting(userId, "email_reviewReminder", email.getReviewReminder() != null ? email.getReviewReminder().toString() : null, now);
                updateNotificationSetting(userId, "email_systemUpdates", email.getSystemUpdates() != null ? email.getSystemUpdates().toString() : null, now);
            }

            // 更新推送通知设置
            if (request.getPushNotifications() != null) {
                PushNotificationsRequest push = request.getPushNotifications();
                updateNotificationSetting(userId, "push_enabled", push.getEnabled() != null ? push.getEnabled().toString() : null, now);
                updateNotificationSetting(userId, "push_reviewReminder", push.getReviewReminder() != null ? push.getReviewReminder().toString() : null, now);
                updateNotificationSetting(userId, "push_dailyGoalReminder", push.getDailyGoalReminder() != null ? push.getDailyGoalReminder().toString() : null, now);
                updateNotificationSetting(userId, "push_streakReminder", push.getStreakReminder() != null ? push.getStreakReminder().toString() : null, now);
                updateNotificationSetting(userId, "push_newFeature", push.getNewFeature() != null ? push.getNewFeature().toString() : null, now);
                updateNotificationSetting(userId, "push_promotional", push.getPromotional() != null ? push.getPromotional().toString() : null, now);
            }

            // 更新应用内通知设置
            if (request.getInAppNotifications() != null) {
                InAppNotificationsRequest inApp = request.getInAppNotifications();
                updateNotificationSetting(userId, "inApp_enabled", inApp.getEnabled() != null ? inApp.getEnabled().toString() : null, now);
                updateNotificationSetting(userId, "inApp_showBadge", inApp.getShowBadge() != null ? inApp.getShowBadge().toString() : null, now);
                updateNotificationSetting(userId, "inApp_soundEnabled", inApp.getSoundEnabled() != null ? inApp.getSoundEnabled().toString() : null, now);
                updateNotificationSetting(userId, "inApp_vibrationEnabled", inApp.getVibrationEnabled() != null ? inApp.getVibrationEnabled().toString() : null, now);
            }

            // 更新静默时段设置
            if (request.getQuietHours() != null) {
                QuietHoursRequest quietHours = request.getQuietHours();
                updateNotificationSetting(userId, "quietHours_enabled", quietHours.getEnabled() != null ? quietHours.getEnabled().toString() : null, now);
                updateNotificationSetting(userId, "quietHours_startTime", quietHours.getStartTime(), now);
                updateNotificationSetting(userId, "quietHours_endTime", quietHours.getEndTime(), now);
            }

            // 5. 查询更新后的设置
            String settingsSql = "SELECT setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ? AND setting_type = 'notification'";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 6. 构建响应数据
            SettingsGetNotification.NotificationSettingsData settingsData = new SettingsGetNotification.NotificationSettingsData();

            for (Map<String, Object> setting : settingsList) {
                String key = (String) setting.get("setting_key");
                String value = (String) setting.get("setting_value");

                String[] parts = key.split("_");
                if (parts.length >= 2) {
                    String category = parts[0];
                    String subKey = parts[1];

                    switch (category) {
                        case "email":
                            updateEmailSetting(settingsData.getEmailNotifications(), subKey, value);
                            break;
                        case "push":
                            updatePushSetting(settingsData.getPushNotifications(), subKey, value);
                            break;
                        case "inApp":
                            updateInAppSetting(settingsData.getInAppNotifications(), subKey, value);
                            break;
                        case "quietHours":
                            updateQuietHoursSetting(settingsData.getQuietHours(), subKey, value);
                            break;
                    }
                }
            }

            // 7. 准备响应
            UpdateNotificationResponse response = new UpdateNotificationResponse(true, "通知设置更新成功", settingsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新通知设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateNotificationResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private void updateNotificationSetting(Integer userId, String key, String value, LocalDateTime now) {
        if (value != null) {
            // 检查设置是否存在
            String checkSql = "SELECT setting_id FROM user_settings WHERE user_id = ? AND setting_type = 'notification' AND setting_key = ?";
            List<Map<String, Object>> existing = jdbcTemplate.queryForList(checkSql, userId, key);

            if (existing.isEmpty()) {
                // 插入新设置
                String insertSql = "INSERT INTO user_settings (user_id, setting_type, setting_key, setting_value, created_at, updated_at) VALUES (?, 'notification', ?, ?, ?, ?)";
                jdbcTemplate.update(insertSql, userId, key, value, now, now);
            } else {
                // 更新现有设置
                String updateSql = "UPDATE user_settings SET setting_value = ?, updated_at = ? WHERE user_id = ? AND setting_type = 'notification' AND setting_key = ?";
                jdbcTemplate.update(updateSql, value, now, userId, key);
            }
        }
    }

    private void updateEmailSetting(SettingsGetNotification.EmailNotifications email, String key, String value) {
        if (value == null) return;

        boolean boolValue = "true".equalsIgnoreCase(value) || "1".equals(value);

        switch (key) {
            case "enabled":
                email.setEnabled(boolValue);
                break;
            case "dailyDigest":
                email.setDailyDigest(boolValue);
                break;
            case "weeklyReport":
                email.setWeeklyReport(boolValue);
                break;
            case "achievementUnlocked":
                email.setAchievementUnlocked(boolValue);
                break;
            case "reviewReminder":
                email.setReviewReminder(boolValue);
                break;
            case "systemUpdates":
                email.setSystemUpdates(boolValue);
                break;
        }
    }

    private void updatePushSetting(SettingsGetNotification.PushNotifications push, String key, String value) {
        if (value == null) return;

        boolean boolValue = "true".equalsIgnoreCase(value) || "1".equals(value);

        switch (key) {
            case "enabled":
                push.setEnabled(boolValue);
                break;
            case "reviewReminder":
                push.setReviewReminder(boolValue);
                break;
            case "dailyGoalReminder":
                push.setDailyGoalReminder(boolValue);
                break;
            case "streakReminder":
                push.setStreakReminder(boolValue);
                break;
            case "newFeature":
                push.setNewFeature(boolValue);
                break;
            case "promotional":
                push.setPromotional(boolValue);
                break;
        }
    }

    private void updateInAppSetting(SettingsGetNotification.InAppNotifications inApp, String key, String value) {
        if (value == null) return;

        boolean boolValue = "true".equalsIgnoreCase(value) || "1".equals(value);

        switch (key) {
            case "enabled":
                inApp.setEnabled(boolValue);
                break;
            case "showBadge":
                inApp.setShowBadge(boolValue);
                break;
            case "soundEnabled":
                inApp.setSoundEnabled(boolValue);
                break;
            case "vibrationEnabled":
                inApp.setVibrationEnabled(boolValue);
                break;
        }
    }

    private void updateQuietHoursSetting(SettingsGetNotification.QuietHours quietHours, String key, String value) {
        if (value == null) return;

        if (key.equals("enabled")) {
            quietHours.setEnabled("true".equalsIgnoreCase(value) || "1".equals(value));
        } else if (key.equals("startTime")) {
            quietHours.setStartTime(value);
        } else if (key.equals("endTime")) {
            quietHours.setEndTime(value);
        }
    }

    private boolean isValidTimeFormat(String time) {
        return time.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$");
    }
}
