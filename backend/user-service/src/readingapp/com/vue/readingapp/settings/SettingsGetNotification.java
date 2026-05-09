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
public class SettingsGetNotification {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到获取通知设置请求 ===");
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

    public static class NotificationSettingsResponse {
        private boolean success;
        private String message;
        private NotificationSettingsData data;

        public NotificationSettingsResponse(boolean success, String message, NotificationSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public NotificationSettingsData getData() { return data; }
        public void setData(NotificationSettingsData data) { this.data = data; }
    }

    public static class NotificationSettingsData {
        private EmailNotifications emailNotifications;
        private PushNotifications pushNotifications;
        private InAppNotifications inAppNotifications;
        private QuietHours quietHours;

        public NotificationSettingsData() {
            // 设置默认值
            this.emailNotifications = new EmailNotifications();
            this.pushNotifications = new PushNotifications();
            this.inAppNotifications = new InAppNotifications();
            this.quietHours = new QuietHours();
        }

        // Getters and Setters
        public EmailNotifications getEmailNotifications() { return emailNotifications; }
        public void setEmailNotifications(EmailNotifications emailNotifications) { this.emailNotifications = emailNotifications; }

        public PushNotifications getPushNotifications() { return pushNotifications; }
        public void setPushNotifications(PushNotifications pushNotifications) { this.pushNotifications = pushNotifications; }

        public InAppNotifications getInAppNotifications() { return inAppNotifications; }
        public void setInAppNotifications(InAppNotifications inAppNotifications) { this.inAppNotifications = inAppNotifications; }

        public QuietHours getQuietHours() { return quietHours; }
        public void setQuietHours(QuietHours quietHours) { this.quietHours = quietHours; }
    }

    public static class EmailNotifications {
        private boolean enabled;
        private boolean dailyDigest;
        private boolean weeklyReport;
        private boolean achievementUnlocked;
        private boolean reviewReminder;
        private boolean systemUpdates;

        public EmailNotifications() {
            this.enabled = true;
            this.dailyDigest = true;
            this.weeklyReport = true;
            this.achievementUnlocked = true;
            this.reviewReminder = true;
            this.systemUpdates = true;
        }

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public boolean isDailyDigest() { return dailyDigest; }
        public void setDailyDigest(boolean dailyDigest) { this.dailyDigest = dailyDigest; }

        public boolean isWeeklyReport() { return weeklyReport; }
        public void setWeeklyReport(boolean weeklyReport) { this.weeklyReport = weeklyReport; }

        public boolean isAchievementUnlocked() { return achievementUnlocked; }
        public void setAchievementUnlocked(boolean achievementUnlocked) { this.achievementUnlocked = achievementUnlocked; }

        public boolean isReviewReminder() { return reviewReminder; }
        public void setReviewReminder(boolean reviewReminder) { this.reviewReminder = reviewReminder; }

        public boolean isSystemUpdates() { return systemUpdates; }
        public void setSystemUpdates(boolean systemUpdates) { this.systemUpdates = systemUpdates; }
    }

    public static class PushNotifications {
        private boolean enabled;
        private boolean reviewReminder;
        private boolean dailyGoalReminder;
        private boolean streakReminder;
        private boolean newFeature;
        private boolean promotional;

        public PushNotifications() {
            this.enabled = true;
            this.reviewReminder = true;
            this.dailyGoalReminder = true;
            this.streakReminder = true;
            this.newFeature = true;
            this.promotional = false;
        }

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public boolean isReviewReminder() { return reviewReminder; }
        public void setReviewReminder(boolean reviewReminder) { this.reviewReminder = reviewReminder; }

        public boolean isDailyGoalReminder() { return dailyGoalReminder; }
        public void setDailyGoalReminder(boolean dailyGoalReminder) { this.dailyGoalReminder = dailyGoalReminder; }

        public boolean isStreakReminder() { return streakReminder; }
        public void setStreakReminder(boolean streakReminder) { this.streakReminder = streakReminder; }

        public boolean isNewFeature() { return newFeature; }
        public void setNewFeature(boolean newFeature) { this.newFeature = newFeature; }

        public boolean isPromotional() { return promotional; }
        public void setPromotional(boolean promotional) { this.promotional = promotional; }
    }

    public static class InAppNotifications {
        private boolean enabled;
        private boolean showBadge;
        private boolean soundEnabled;
        private boolean vibrationEnabled;

        public InAppNotifications() {
            this.enabled = true;
            this.showBadge = true;
            this.soundEnabled = true;
            this.vibrationEnabled = true;
        }

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public boolean isShowBadge() { return showBadge; }
        public void setShowBadge(boolean showBadge) { this.showBadge = showBadge; }

        public boolean isSoundEnabled() { return soundEnabled; }
        public void setSoundEnabled(boolean soundEnabled) { this.soundEnabled = soundEnabled; }

        public boolean isVibrationEnabled() { return vibrationEnabled; }
        public void setVibrationEnabled(boolean vibrationEnabled) { this.vibrationEnabled = vibrationEnabled; }
    }

    public static class QuietHours {
        private boolean enabled;
        private String startTime;
        private String endTime;

        public QuietHours() {
            this.enabled = false;
            this.startTime = "22:00";
            this.endTime = "07:00";
        }

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }

        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
    }

    @GetMapping("")
    public ResponseEntity<NotificationSettingsResponse> getNotificationSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            printRequest("Authorization: " + authHeader);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new NotificationSettingsResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new NotificationSettingsResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 查询通知设置
            String settingsSql = "SELECT setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ? AND setting_type = 'notification'";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 4. 构建响应数据
            NotificationSettingsData settingsData = new NotificationSettingsData();

            for (Map<String, Object> setting : settingsList) {
                String key = (String) setting.get("setting_key");
                String value = (String) setting.get("setting_value");

                // 解析设置键，格式如：email_enabled, push_reviewReminder, quietHours_startTime
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

            // 5. 准备响应
            NotificationSettingsResponse response = new NotificationSettingsResponse(true, "获取通知设置成功", settingsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取通知设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new NotificationSettingsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private void updateEmailSetting(EmailNotifications email, String key, String value) {
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

    private void updatePushSetting(PushNotifications push, String key, String value) {
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

    private void updateInAppSetting(InAppNotifications inApp, String key, String value) {
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

    private void updateQuietHoursSetting(QuietHours quietHours, String key, String value) {
        if (value == null) return;

        if (key.equals("enabled")) {
            quietHours.setEnabled("true".equalsIgnoreCase(value) || "1".equals(value));
        } else if (key.equals("startTime")) {
            quietHours.setStartTime(value);
        } else if (key.equals("endTime")) {
            quietHours.setEndTime(value);
        }
    }
}
