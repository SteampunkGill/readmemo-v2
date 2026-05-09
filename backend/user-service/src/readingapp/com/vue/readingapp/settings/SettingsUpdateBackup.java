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
public class SettingsUpdateBackup {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到更新备份设置请求 ===");
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

    public static class UpdateBackupRequest {
        private Boolean autoBackup;
        private String backupFrequency;
        private String backupTime;
        private Integer backupRetentionDays;
        private Boolean backupToCloud;
        private String cloudService;
        private Boolean backupEncryption;
        private Boolean includeDocuments;
        private Boolean includeVocabulary;
        private Boolean includeNotes;
        private Boolean includeHighlights;

        // Getters and Setters
        public Boolean getAutoBackup() { return autoBackup; }
        public void setAutoBackup(Boolean autoBackup) { this.autoBackup = autoBackup; }

        public String getBackupFrequency() { return backupFrequency; }
        public void setBackupFrequency(String backupFrequency) { this.backupFrequency = backupFrequency; }

        public String getBackupTime() { return backupTime; }
        public void setBackupTime(String backupTime) { this.backupTime = backupTime; }

        public Integer getBackupRetentionDays() { return backupRetentionDays; }
        public void setBackupRetentionDays(Integer backupRetentionDays) { this.backupRetentionDays = backupRetentionDays; }

        public Boolean getBackupToCloud() { return backupToCloud; }
        public void setBackupToCloud(Boolean backupToCloud) { this.backupToCloud = backupToCloud; }

        public String getCloudService() { return cloudService; }
        public void setCloudService(String cloudService) { this.cloudService = cloudService; }

        public Boolean getBackupEncryption() { return backupEncryption; }
        public void setBackupEncryption(Boolean backupEncryption) { this.backupEncryption = backupEncryption; }

        public Boolean getIncludeDocuments() { return includeDocuments; }
        public void setIncludeDocuments(Boolean includeDocuments) { this.includeDocuments = includeDocuments; }

        public Boolean getIncludeVocabulary() { return includeVocabulary; }
        public void setIncludeVocabulary(Boolean includeVocabulary) { this.includeVocabulary = includeVocabulary; }

        public Boolean getIncludeNotes() { return includeNotes; }
        public void setIncludeNotes(Boolean includeNotes) { this.includeNotes = includeNotes; }

        public Boolean getIncludeHighlights() { return includeHighlights; }
        public void setIncludeHighlights(Boolean includeHighlights) { this.includeHighlights = includeHighlights; }
    }

    public static class UpdateBackupResponse {
        private boolean success;
        private String message;
        private SettingsGetBackup.BackupSettingsData data;

        public UpdateBackupResponse(boolean success, String message, SettingsGetBackup.BackupSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SettingsGetBackup.BackupSettingsData getData() { return data; }
        public void setData(SettingsGetBackup.BackupSettingsData data) { this.data = data; }
    }

    @PutMapping("")
    public ResponseEntity<UpdateBackupResponse> updateBackupSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateBackupRequest request) {

        try {
            printRequest(request);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateBackupResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateBackupResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 验证备份设置数据
            if (request.getBackupRetentionDays() != null && (request.getBackupRetentionDays() < 1 || request.getBackupRetentionDays() > 365)) {
                return ResponseEntity.badRequest().body(
                        new UpdateBackupResponse(false, "备份保留天数必须在1-365之间", null)
                );
            }

            // 验证备份频率
            if (request.getBackupFrequency() != null && !isValidBackupFrequency(request.getBackupFrequency())) {
                return ResponseEntity.badRequest().body(
                        new UpdateBackupResponse(false, "无效的备份频率", null)
                );
            }

            // 验证云服务
            if (request.getCloudService() != null && !isValidCloudService(request.getCloudService())) {
                return ResponseEntity.badRequest().body(
                        new UpdateBackupResponse(false, "无效的云服务", null)
                );
            }

            // 验证备份时间格式
            if (request.getBackupTime() != null && !isValidTimeFormat(request.getBackupTime())) {
                return ResponseEntity.badRequest().body(
                        new UpdateBackupResponse(false, "无效的备份时间格式", null)
                );
            }

            // 4. 更新备份设置
            LocalDateTime now = LocalDateTime.now();

            updateBackupSetting(userId, "autoBackup", request.getAutoBackup() != null ? request.getAutoBackup().toString() : null, now);
            updateBackupSetting(userId, "backupFrequency", request.getBackupFrequency(), now);
            updateBackupSetting(userId, "backupTime", request.getBackupTime(), now);
            updateBackupSetting(userId, "backupRetentionDays", request.getBackupRetentionDays() != null ? request.getBackupRetentionDays().toString() : null, now);
            updateBackupSetting(userId, "backupToCloud", request.getBackupToCloud() != null ? request.getBackupToCloud().toString() : null, now);
            updateBackupSetting(userId, "cloudService", request.getCloudService(), now);
            updateBackupSetting(userId, "backupEncryption", request.getBackupEncryption() != null ? request.getBackupEncryption().toString() : null, now);
            updateBackupSetting(userId, "includeDocuments", request.getIncludeDocuments() != null ? request.getIncludeDocuments().toString() : null, now);
            updateBackupSetting(userId, "includeVocabulary", request.getIncludeVocabulary() != null ? request.getIncludeVocabulary().toString() : null, now);
            updateBackupSetting(userId, "includeNotes", request.getIncludeNotes() != null ? request.getIncludeNotes().toString() : null, now);
            updateBackupSetting(userId, "includeHighlights", request.getIncludeHighlights() != null ? request.getIncludeHighlights().toString() : null, now);

            // 5. 查询更新后的设置
            String settingsSql = "SELECT setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ? AND setting_type = 'backup'";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 6. 构建响应数据
            SettingsGetBackup.BackupSettingsData settingsData = new SettingsGetBackup.BackupSettingsData();

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

            // 7. 准备响应
            UpdateBackupResponse response = new UpdateBackupResponse(true, "备份设置更新成功", settingsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新备份设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateBackupResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private void updateBackupSetting(Integer userId, String key, String value, LocalDateTime now) {
        if (value != null) {
            // 检查设置是否存在
            String checkSql = "SELECT setting_id FROM user_settings WHERE user_id = ? AND setting_type = 'backup' AND setting_key = ?";
            List<Map<String, Object>> existing = jdbcTemplate.queryForList(checkSql, userId, key);

            if (existing.isEmpty()) {
                // 插入新设置
                String insertSql = "INSERT INTO user_settings (user_id, setting_type, setting_key, setting_value, created_at, updated_at) VALUES (?, 'backup', ?, ?, ?, ?)";
                jdbcTemplate.update(insertSql, userId, key, value, now, now);
            } else {
                // 更新现有设置
                String updateSql = "UPDATE user_settings SET setting_value = ?, updated_at = ? WHERE user_id = ? AND setting_type = 'backup' AND setting_key = ?";
                jdbcTemplate.update(updateSql, value, now, userId, key);
            }
        }
    }

    private boolean isValidBackupFrequency(String frequency) {
        return frequency.equals("daily") || frequency.equals("weekly") ||
                frequency.equals("monthly") || frequency.equals("never");
    }

    private boolean isValidCloudService(String service) {
        return service.equals("google_drive") || service.equals("dropbox") ||
                service.equals("onedrive") || service.equals("icloud");
    }

    private boolean isValidTimeFormat(String time) {
        return time.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$");
    }
}
