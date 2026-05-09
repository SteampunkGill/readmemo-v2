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
@RequestMapping("/api/v1/user/settings/backup")
public class SettingsGetBackup {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到获取备份设置请求 ===");
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

    public static class BackupSettingsResponse {
        private boolean success;
        private String message;
        private BackupSettingsData data;

        public BackupSettingsResponse(boolean success, String message, BackupSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public BackupSettingsData getData() { return data; }
        public void setData(BackupSettingsData data) { this.data = data; }
    }

    public static class BackupSettingsData {
        private boolean autoBackup;
        private String backupFrequency;
        private String backupTime;
        private int backupRetentionDays;
        private boolean backupToCloud;
        private String cloudService;
        private boolean backupEncryption;
        private boolean includeDocuments;
        private boolean includeVocabulary;
        private boolean includeNotes;
        private boolean includeHighlights;
        private String lastBackupDate;
        private String nextBackupDate;

        public BackupSettingsData() {
            // 设置默认值
            this.autoBackup = true;
            this.backupFrequency = "weekly";
            this.backupTime = "02:00";
            this.backupRetentionDays = 30;
            this.backupToCloud = false;
            this.cloudService = "google_drive";
            this.backupEncryption = true;
            this.includeDocuments = true;
            this.includeVocabulary = true;
            this.includeNotes = true;
            this.includeHighlights = true;
            this.lastBackupDate = null;
            this.nextBackupDate = null;
        }

        // Getters and Setters
        public boolean isAutoBackup() { return autoBackup; }
        public void setAutoBackup(boolean autoBackup) { this.autoBackup = autoBackup; }

        public String getBackupFrequency() { return backupFrequency; }
        public void setBackupFrequency(String backupFrequency) { this.backupFrequency = backupFrequency; }

        public String getBackupTime() { return backupTime; }
        public void setBackupTime(String backupTime) { this.backupTime = backupTime; }

        public int getBackupRetentionDays() { return backupRetentionDays; }
        public void setBackupRetentionDays(int backupRetentionDays) { this.backupRetentionDays = backupRetentionDays; }

        public boolean isBackupToCloud() { return backupToCloud; }
        public void setBackupToCloud(boolean backupToCloud) { this.backupToCloud = backupToCloud; }

        public String getCloudService() { return cloudService; }
        public void setCloudService(String cloudService) { this.cloudService = cloudService; }

        public boolean isBackupEncryption() { return backupEncryption; }
        public void setBackupEncryption(boolean backupEncryption) { this.backupEncryption = backupEncryption; }

        public boolean isIncludeDocuments() { return includeDocuments; }
        public void setIncludeDocuments(boolean includeDocuments) { this.includeDocuments = includeDocuments; }

        public boolean isIncludeVocabulary() { return includeVocabulary; }
        public void setIncludeVocabulary(boolean includeVocabulary) { this.includeVocabulary = includeVocabulary; }

        public boolean isIncludeNotes() { return includeNotes; }
        public void setIncludeNotes(boolean includeNotes) { this.includeNotes = includeNotes; }

        public boolean isIncludeHighlights() { return includeHighlights; }
        public void setIncludeHighlights(boolean includeHighlights) { this.includeHighlights = includeHighlights; }

        public String getLastBackupDate() { return lastBackupDate; }
        public void setLastBackupDate(String lastBackupDate) { this.lastBackupDate = lastBackupDate; }

        public String getNextBackupDate() { return nextBackupDate; }
        public void setNextBackupDate(String nextBackupDate) { this.nextBackupDate = nextBackupDate; }
    }

    @GetMapping("")
    public ResponseEntity<BackupSettingsResponse> getBackupSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            printRequest("Authorization: " + authHeader);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BackupSettingsResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BackupSettingsResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 查询备份设置
            String settingsSql = "SELECT setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ? AND setting_type = 'backup'";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 4. 构建响应数据
            BackupSettingsData settingsData = new BackupSettingsData();

            for (Map<String, Object> setting : settingsList) {
                String key = (String) setting.get("setting_key");
                String value = (String) setting.get("setting_value");

                switch (key) {
                    case "autoBackup":
                        settingsData.setAutoBackup("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "backupFrequency":
                        settingsData.setBackupFrequency(value);
                        break;
                    case "backupTime":
                        settingsData.setBackupTime(value);
                        break;
                    case "backupRetentionDays":
                        try {
                            settingsData.setBackupRetentionDays(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            settingsData.setBackupRetentionDays(30);
                        }
                        break;
                    case "backupToCloud":
                        settingsData.setBackupToCloud("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "cloudService":
                        settingsData.setCloudService(value);
                        break;
                    case "backupEncryption":
                        settingsData.setBackupEncryption("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "includeDocuments":
                        settingsData.setIncludeDocuments("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "includeVocabulary":
                        settingsData.setIncludeVocabulary("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "includeNotes":
                        settingsData.setIncludeNotes("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "includeHighlights":
                        settingsData.setIncludeHighlights("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "lastBackupDate":
                        settingsData.setLastBackupDate(value);
                        break;
                    case "nextBackupDate":
                        settingsData.setNextBackupDate(value);
                        break;
                }
            }

            // 5. 准备响应
            BackupSettingsResponse response = new BackupSettingsResponse(true, "获取备份设置成功", settingsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取备份设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BackupSettingsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
