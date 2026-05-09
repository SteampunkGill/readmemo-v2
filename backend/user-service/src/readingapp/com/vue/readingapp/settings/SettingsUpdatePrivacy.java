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
@RequestMapping("/api/v1/user/settings/privacy")
public class SettingsUpdatePrivacy {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到更新隐私设置请求 ===");
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

    public static class UpdatePrivacyRequest {
        private String privacyLevel;
        private Boolean shareReadingData;
        private Boolean shareVocabularyData;
        private Boolean shareAchievements;
        private Boolean allowDataCollection;
        private Boolean showOnlineStatus;
        private Boolean allowFriendRequests;
        private Boolean allowMessages;
        private Boolean showLearningStats;
        private Boolean showRecentActivity;

        // Getters and Setters
        public String getPrivacyLevel() { return privacyLevel; }
        public void setPrivacyLevel(String privacyLevel) { this.privacyLevel = privacyLevel; }

        public Boolean getShareReadingData() { return shareReadingData; }
        public void setShareReadingData(Boolean shareReadingData) { this.shareReadingData = shareReadingData; }

        public Boolean getShareVocabularyData() { return shareVocabularyData; }
        public void setShareVocabularyData(Boolean shareVocabularyData) { this.shareVocabularyData = shareVocabularyData; }

        public Boolean getShareAchievements() { return shareAchievements; }
        public void setShareAchievements(Boolean shareAchievements) { this.shareAchievements = shareAchievements; }

        public Boolean getAllowDataCollection() { return allowDataCollection; }
        public void setAllowDataCollection(Boolean allowDataCollection) { this.allowDataCollection = allowDataCollection; }

        public Boolean getShowOnlineStatus() { return showOnlineStatus; }
        public void setShowOnlineStatus(Boolean showOnlineStatus) { this.showOnlineStatus = showOnlineStatus; }

        public Boolean getAllowFriendRequests() { return allowFriendRequests; }
        public void setAllowFriendRequests(Boolean allowFriendRequests) { this.allowFriendRequests = allowFriendRequests; }

        public Boolean getAllowMessages() { return allowMessages; }
        public void setAllowMessages(Boolean allowMessages) { this.allowMessages = allowMessages; }

        public Boolean getShowLearningStats() { return showLearningStats; }
        public void setShowLearningStats(Boolean showLearningStats) { this.showLearningStats = showLearningStats; }

        public Boolean getShowRecentActivity() { return showRecentActivity; }
        public void setShowRecentActivity(Boolean showRecentActivity) { this.showRecentActivity = showRecentActivity; }
    }

    public static class UpdatePrivacyResponse {
        private boolean success;
        private String message;
        private PrivacySettingsData data;

        public UpdatePrivacyResponse(boolean success, String message, PrivacySettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public PrivacySettingsData getData() { return data; }
        public void setData(PrivacySettingsData data) { this.data = data; }
    }

    public static class PrivacySettingsData {
        private String privacyLevel;
        private boolean shareReadingData;
        private boolean shareVocabularyData;
        private boolean shareAchievements;
        private boolean allowDataCollection;
        private boolean showOnlineStatus;
        private boolean allowFriendRequests;
        private boolean allowMessages;
        private boolean showLearningStats;
        private boolean showRecentActivity;

        public PrivacySettingsData() {
            // 设置默认值
            this.privacyLevel = "standard";
            this.shareReadingData = true;
            this.shareVocabularyData = true;
            this.shareAchievements = true;
            this.allowDataCollection = true;
            this.showOnlineStatus = true;
            this.allowFriendRequests = true;
            this.allowMessages = true;
            this.showLearningStats = true;
            this.showRecentActivity = true;
        }

        // Getters and Setters
        public String getPrivacyLevel() { return privacyLevel; }
        public void setPrivacyLevel(String privacyLevel) { this.privacyLevel = privacyLevel; }

        public boolean isShareReadingData() { return shareReadingData; }
        public void setShareReadingData(boolean shareReadingData) { this.shareReadingData = shareReadingData; }

        public boolean isShareVocabularyData() { return shareVocabularyData; }
        public void setShareVocabularyData(boolean shareVocabularyData) { this.shareVocabularyData = shareVocabularyData; }

        public boolean isShareAchievements() { return shareAchievements; }
        public void setShareAchievements(boolean shareAchievements) { this.shareAchievements = shareAchievements; }

        public boolean isAllowDataCollection() { return allowDataCollection; }
        public void setAllowDataCollection(boolean allowDataCollection) { this.allowDataCollection = allowDataCollection; }

        public boolean isShowOnlineStatus() { return showOnlineStatus; }
        public void setShowOnlineStatus(boolean showOnlineStatus) { this.showOnlineStatus = showOnlineStatus; }

        public boolean isAllowFriendRequests() { return allowFriendRequests; }
        public void setAllowFriendRequests(boolean allowFriendRequests) { this.allowFriendRequests = allowFriendRequests; }

        public boolean isAllowMessages() { return allowMessages; }
        public void setAllowMessages(boolean allowMessages) { this.allowMessages = allowMessages; }

        public boolean isShowLearningStats() { return showLearningStats; }
        public void setShowLearningStats(boolean showLearningStats) { this.showLearningStats = showLearningStats; }

        public boolean isShowRecentActivity() { return showRecentActivity; }
        public void setShowRecentActivity(boolean showRecentActivity) { this.showRecentActivity = showRecentActivity; }
    }

    @PutMapping("")
    public ResponseEntity<UpdatePrivacyResponse> updatePrivacySettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdatePrivacyRequest request) {

        try {
            printRequest(request);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdatePrivacyResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdatePrivacyResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 验证隐私设置数据
            if (request.getPrivacyLevel() != null && !isValidPrivacyLevel(request.getPrivacyLevel())) {
                return ResponseEntity.badRequest().body(
                        new UpdatePrivacyResponse(false, "无效的隐私级别", null)
                );
            }

            // 4. 更新隐私设置
            LocalDateTime now = LocalDateTime.now();

            updatePrivacySetting(userId, "privacyLevel", request.getPrivacyLevel(), now);
            updatePrivacySetting(userId, "shareReadingData", request.getShareReadingData() != null ? request.getShareReadingData().toString() : null, now);
            updatePrivacySetting(userId, "shareVocabularyData", request.getShareVocabularyData() != null ? request.getShareVocabularyData().toString() : null, now);
            updatePrivacySetting(userId, "shareAchievements", request.getShareAchievements() != null ? request.getShareAchievements().toString() : null, now);
            updatePrivacySetting(userId, "allowDataCollection", request.getAllowDataCollection() != null ? request.getAllowDataCollection().toString() : null, now);
            updatePrivacySetting(userId, "showOnlineStatus", request.getShowOnlineStatus() != null ? request.getShowOnlineStatus().toString() : null, now);
            updatePrivacySetting(userId, "allowFriendRequests", request.getAllowFriendRequests() != null ? request.getAllowFriendRequests().toString() : null, now);
            updatePrivacySetting(userId, "allowMessages", request.getAllowMessages() != null ? request.getAllowMessages().toString() : null, now);
            updatePrivacySetting(userId, "showLearningStats", request.getShowLearningStats() != null ? request.getShowLearningStats().toString() : null, now);
            updatePrivacySetting(userId, "showRecentActivity", request.getShowRecentActivity() != null ? request.getShowRecentActivity().toString() : null, now);

            // 5. 查询更新后的设置
            String settingsSql = "SELECT setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ? AND setting_type = 'privacy'";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 6. 构建响应数据
            PrivacySettingsData settingsData = new PrivacySettingsData();

            for (Map<String, Object> setting : settingsList) {
                String key = (String) setting.get("setting_key");
                String value = (String) setting.get("setting_value");

                switch (key) {
                    case "privacyLevel":
                        settingsData.setPrivacyLevel(value);
                        break;
                    case "shareReadingData":
                        settingsData.setShareReadingData("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "shareVocabularyData":
                        settingsData.setShareVocabularyData("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "shareAchievements":
                        settingsData.setShareAchievements("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "allowDataCollection":
                        settingsData.setAllowDataCollection("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "showOnlineStatus":
                        settingsData.setShowOnlineStatus("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "allowFriendRequests":
                        settingsData.setAllowFriendRequests("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "allowMessages":
                        settingsData.setAllowMessages("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "showLearningStats":
                        settingsData.setShowLearningStats("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "showRecentActivity":
                        settingsData.setShowRecentActivity("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                }
            }

            // 7. 准备响应
            UpdatePrivacyResponse response = new UpdatePrivacyResponse(true, "隐私设置更新成功", settingsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新隐私设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdatePrivacyResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private void updatePrivacySetting(Integer userId, String key, String value, LocalDateTime now) {
        if (value != null) {
            // 检查设置是否存在
            String checkSql = "SELECT setting_id FROM user_settings WHERE user_id = ? AND setting_type = 'privacy' AND setting_key = ?";
            List<Map<String, Object>> existing = jdbcTemplate.queryForList(checkSql, userId, key);

            if (existing.isEmpty()) {
                // 插入新设置
                String insertSql = "INSERT INTO user_settings (user_id, setting_type, setting_key, setting_value, created_at, updated_at) VALUES (?, 'privacy', ?, ?, ?, ?)";
                jdbcTemplate.update(insertSql, userId, key, value, now, now);
            } else {
                // 更新现有设置
                String updateSql = "UPDATE user_settings SET setting_value = ?, updated_at = ? WHERE user_id = ? AND setting_type = 'privacy' AND setting_key = ?";
                jdbcTemplate.update(updateSql, value, now, userId, key);
            }
        }
    }

    private boolean isValidPrivacyLevel(String privacyLevel) {
        return privacyLevel.equals("minimal") || privacyLevel.equals("standard") || privacyLevel.equals("strict");
    }
}
