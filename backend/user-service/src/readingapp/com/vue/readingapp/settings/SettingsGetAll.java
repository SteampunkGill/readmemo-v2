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
@RequestMapping("/api/v1/user/settings/all")
public class SettingsGetAll {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到获取所有设置请求 ===");
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

    public static class AllSettingsResponse {
        private boolean success;
        private String message;
        private AllSettingsData data;

        public AllSettingsResponse(boolean success, String message, AllSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public AllSettingsData getData() { return data; }
        public void setData(AllSettingsData data) { this.data = data; }
    }

    public static class AllSettingsData {
        private SettingsGet.SettingsData settings;
        private SettingsGetReading.ReadingSettingsData readingSettings;
        private SettingsGetReview.ReviewSettingsData reviewSettings;
        private SettingsGetNotification.NotificationSettingsData notificationSettings;

        public AllSettingsData() {
            this.settings = new SettingsGet.SettingsData();
            this.readingSettings = new SettingsGetReading.ReadingSettingsData();
            this.reviewSettings = new SettingsGetReview.ReviewSettingsData();
            this.notificationSettings = new SettingsGetNotification.NotificationSettingsData();
        }

        // Getters and Setters
        public SettingsGet.SettingsData getSettings() { return settings; }
        public void setSettings(SettingsGet.SettingsData settings) { this.settings = settings; }

        public SettingsGetReading.ReadingSettingsData getReadingSettings() { return readingSettings; }
        public void setReadingSettings(SettingsGetReading.ReadingSettingsData readingSettings) { this.readingSettings = readingSettings; }

        public SettingsGetReview.ReviewSettingsData getReviewSettings() { return reviewSettings; }
        public void setReviewSettings(SettingsGetReview.ReviewSettingsData reviewSettings) { this.reviewSettings = reviewSettings; }

        public SettingsGetNotification.NotificationSettingsData getNotificationSettings() { return notificationSettings; }
        public void setNotificationSettings(SettingsGetNotification.NotificationSettingsData notificationSettings) { this.notificationSettings = notificationSettings; }
    }

    @GetMapping("")
    public ResponseEntity<AllSettingsResponse> getAllSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            printRequest("Authorization: " + authHeader);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new AllSettingsResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new AllSettingsResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 查询所有设置
            String settingsSql = "SELECT setting_type, setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ?";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 4. 构建响应数据
            AllSettingsData allSettingsData = new AllSettingsData();

            // 按设置类型分组
            Map<String, List<Map<String, Object>>> groupedSettings = new HashMap<>();

            for (Map<String, Object> setting : settingsList) {
                String settingType = (String) setting.get("setting_type");
                String key = (String) setting.get("setting_key");
                String value = (String) setting.get("setting_value");

                // 根据设置类型处理
                switch (settingType) {
                    case "general":
                        updateGeneralSettings(allSettingsData.getSettings(), key, value);
                        break;
                    case "reading":
                        updateReadingSettings(allSettingsData.getReadingSettings(), key, value);
                        break;
                    case "review":
                        updateReviewSettings(allSettingsData.getReviewSettings(), key, value);
                        break;
                    case "notification":
                        updateNotificationSettings(allSettingsData.getNotificationSettings(), key, value);
                        break;
                }
            }

            // 5. 准备响应
            AllSettingsResponse response = new AllSettingsResponse(true, "获取所有设置成功", allSettingsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取所有设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new AllSettingsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private void updateGeneralSettings(SettingsGet.SettingsData settings, String key, String value) {
        if (value == null) return;

        switch (key) {
            case "language":
                settings.setLanguage(value);
                break;
            case "theme":
                settings.setTheme(value);
                break;
            case "fontSize":
                try {
                    settings.setFontSize(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    settings.setFontSize(14);
                }
                break;
            case "timezone":
                settings.setTimezone(value);
                break;
            case "dateFormat":
                settings.setDateFormat(value);
                break;
            case "timeFormat":
                settings.setTimeFormat(value);
                break;
            case "autoSave":
                settings.setAutoSave("true".equalsIgnoreCase(value) || "1".equals(value));
                break;
            case "autoSaveInterval":
                try {
                    settings.setAutoSaveInterval(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    settings.setAutoSaveInterval(30);
                }
                break;
            case "syncEnabled":
                settings.setSyncEnabled("true".equalsIgnoreCase(value) || "1".equals(value));
                break;
            case "syncInterval":
                try {
                    settings.setSyncInterval(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    settings.setSyncInterval(5);
                }
                break;
            case "dataRetentionDays":
                try {
                    settings.setDataRetentionDays(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    settings.setDataRetentionDays(90);
                }
                break;
            case "privacyLevel":
                settings.setPrivacyLevel(value);
                break;
        }
    }

    private void updateReadingSettings(SettingsGetReading.ReadingSettingsData readingSettings, String key, String value) {
        if (value == null) return;

        switch (key) {
            case "fontFamily":
                readingSettings.setFontFamily(value);
                break;
            case "fontSize":
                try {
                    readingSettings.setFontSize(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    readingSettings.setFontSize(16);
                }
                break;
            case "lineHeight":
                try {
                    readingSettings.setLineHeight(Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    readingSettings.setLineHeight(1.6);
                }
                break;
            case "paragraphSpacing":
                try {
                    readingSettings.setParagraphSpacing(Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    readingSettings.setParagraphSpacing(1.2);
                }
                break;
            case "theme":
                readingSettings.setTheme(value);
                break;
            case "contrast":
                readingSettings.setContrast(value);
                break;
            case "highlightColor":
                readingSettings.setHighlightColor(value);
                break;
            case "noteColor":
                readingSettings.setNoteColor(value);
                break;
            case "autoScroll":
                readingSettings.setAutoScroll("true".equalsIgnoreCase(value) || "1".equals(value));
                break;
            case "scrollSpeed":
                try {
                    readingSettings.setScrollSpeed(Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    readingSettings.setScrollSpeed(1.0);
                }
                break;
            case "showTranslation":
                readingSettings.setShowTranslation("true".equalsIgnoreCase(value) || "1".equals(value));
                break;
            case "translationPosition":
                readingSettings.setTranslationPosition(value);
                break;
            case "showPhonetic":
                readingSettings.setShowPhonetic("true".equalsIgnoreCase(value) || "1".equals(value));
                break;
            case "showDefinition":
                readingSettings.setShowDefinition("true".equalsIgnoreCase(value) || "1".equals(value));
                break;
            case "autoPlayAudio":
                readingSettings.setAutoPlayAudio("true".equalsIgnoreCase(value) || "1".equals(value));
                break;
            case "audioVolume":
                try {
                    readingSettings.setAudioVolume(Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    readingSettings.setAudioVolume(0.7);
                }
                break;
            case "textSelection":
                readingSettings.setTextSelection("true".equalsIgnoreCase(value) || "1".equals(value));
                break;
            case "doubleClickTranslate":
                readingSettings.setDoubleClickTranslate("true".equalsIgnoreCase(value) || "1".equals(value));
                break;
            case "wordBreak":
                readingSettings.setWordBreak(value);
                break;
            case "textAlign":
                readingSettings.setTextAlign(value);
                break;
            case "maxWidth":
                try {
                    readingSettings.setMaxWidth(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    readingSettings.setMaxWidth(800);
                }
                break;
            case "margin":
                try {
                    readingSettings.setMargin(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    readingSettings.setMargin(20);
                }
                break;
        }
    }

    private void updateReviewSettings(SettingsGetReview.ReviewSettingsData reviewSettings, String key, String value) {
        if (value == null) return;

        switch (key) {
            case "dailyGoal":
                try {
                    reviewSettings.setDailyGoal(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    reviewSettings.setDailyGoal(20);
                }
                break;
            case "reviewTime":
                reviewSettings.setReviewTime(value);
                break;
            case "reminderEnabled":
                reviewSettings.setReminderEnabled("true".equalsIgnoreCase(value) || "1".equals(value));
                break;
            case "reminderTime":
                reviewSettings.setReminderTime(value);
                break;
            case "difficultyAlgorithm":
                reviewSettings.setDifficultyAlgorithm(value);
                break;
            case "newWordsPerDay":
                try {
                    reviewSettings.setNewWordsPerDay(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    reviewSettings.setNewWordsPerDay(10);
                }
                break;
            case "reviewWordsPerDay":
                try {
                    reviewSettings.setReviewWordsPerDay(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    reviewSettings.setReviewWordsPerDay(50);
                }
                break;
            case "autoGenerateReviews":
                reviewSettings.setAutoGenerateReviews("true".equalsIgnoreCase(value) || "1".equals(value));
                break;
            case "reviewInterval_again":
                try {
                    reviewSettings.getReviewInterval().setAgain(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    reviewSettings.getReviewInterval().setAgain(1);
                }
                break;
            case "reviewInterval_hard":
                try {
                    reviewSettings.getReviewInterval().setHard(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    reviewSettings.getReviewInterval().setHard(10);
                }
                break;
            case "reviewInterval_good":
                try {
                    reviewSettings.getReviewInterval().setGood(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    reviewSettings.getReviewInterval().setGood(1);
                }
                break;
            case "reviewInterval_easy":
                try {
                    reviewSettings.getReviewInterval().setEasy(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    reviewSettings.getReviewInterval().setEasy(3);
                }
                break;
            case "showExamples":
                reviewSettings.setShowExamples("true".equalsIgnoreCase(value) || "1".equals(value));
                break;
            case "showImages":
                reviewSettings.setShowImages("true".equalsIgnoreCase(value) || "1".equals(value));
                break;
            case "autoPlayAudioInReview":
                reviewSettings.setAutoPlayAudioInReview("true".equalsIgnoreCase(value) || "1".equals(value));
                break;
            case "shuffleQuestions":
                reviewSettings.setShuffleQuestions("true".equalsIgnoreCase(value) || "1".equals(value));
                break;
            case "enableStreak":
                reviewSettings.setEnableStreak("true".equalsIgnoreCase(value) || "1".equals(value));
                break;
            case "streakGoal":
                try {
                    reviewSettings.setStreakGoal(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    reviewSettings.setStreakGoal(7);
                }
                break;
            case "enableNotifications":
                reviewSettings.setEnableNotifications("true".equalsIgnoreCase(value) || "1".equals(value));
                break;
        }
    }

    private void updateNotificationSettings(SettingsGetNotification.NotificationSettingsData notificationSettings, String key, String value) {
        if (value == null) return;

        String[] parts = key.split("_");
        if (parts.length >= 2) {
            String category = parts[0];
            String subKey = parts[1];

            switch (category) {
                case "email":
                    updateEmailSetting(notificationSettings.getEmailNotifications(), subKey, value);
                    break;
                case "push":
                    updatePushSetting(notificationSettings.getPushNotifications(), subKey, value);
                    break;
                case "inApp":
                    updateInAppSetting(notificationSettings.getInAppNotifications(), subKey, value);
                    break;
                case "quietHours":
                    updateQuietHoursSetting(notificationSettings.getQuietHours(), subKey, value);
                    break;
            }
        }
    }

    private void updateEmailSetting(SettingsGetNotification.EmailNotifications email, String key, String value) {
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
        if (key.equals("enabled")) {
            quietHours.setEnabled("true".equalsIgnoreCase(value) || "1".equals(value));
        } else if (key.equals("startTime")) {
            quietHours.setStartTime(value);
        } else if (key.equals("endTime")) {
            quietHours.setEndTime(value);
        }
    }
}
