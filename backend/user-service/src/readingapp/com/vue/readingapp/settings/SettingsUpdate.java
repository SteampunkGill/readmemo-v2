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
@RequestMapping("/api/v1/user/settings")
public class SettingsUpdate {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到更新用户设置请求 ===");
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

    public static class UpdateRequest {
        private String language;
        private String theme;
        private Integer fontSize;
        private String timezone;
        private String dateFormat;
        private String timeFormat;
        private Boolean autoSave;
        private Integer autoSaveInterval;
        private Boolean syncEnabled;
        private Integer syncInterval;
        private Integer dataRetentionDays;
        private String privacyLevel;

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }

        public Integer getFontSize() { return fontSize; }
        public void setFontSize(Integer fontSize) { this.fontSize = fontSize; }

        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }

        public String getDateFormat() { return dateFormat; }
        public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }

        public String getTimeFormat() { return timeFormat; }
        public void setTimeFormat(String timeFormat) { this.timeFormat = timeFormat; }

        public Boolean getAutoSave() { return autoSave; }
        public void setAutoSave(Boolean autoSave) { this.autoSave = autoSave; }

        public Integer getAutoSaveInterval() { return autoSaveInterval; }
        public void setAutoSaveInterval(Integer autoSaveInterval) { this.autoSaveInterval = autoSaveInterval; }

        public Boolean getSyncEnabled() { return syncEnabled; }
        public void setSyncEnabled(Boolean syncEnabled) { this.syncEnabled = syncEnabled; }

        public Integer getSyncInterval() { return syncInterval; }
        public void setSyncInterval(Integer syncInterval) { this.syncInterval = syncInterval; }

        public Integer getDataRetentionDays() { return dataRetentionDays; }
        public void setDataRetentionDays(Integer dataRetentionDays) { this.dataRetentionDays = dataRetentionDays; }

        public String getPrivacyLevel() { return privacyLevel; }
        public void setPrivacyLevel(String privacyLevel) { this.privacyLevel = privacyLevel; }
    }

    public static class UpdateResponse {
        private boolean success;
        private String message;
        private SettingsGet.SettingsData data;

        public UpdateResponse(boolean success, String message, SettingsGet.SettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SettingsGet.SettingsData getData() { return data; }
        public void setData(SettingsGet.SettingsData data) { this.data = data; }
    }

    @PutMapping("")
    public ResponseEntity<UpdateResponse> updateSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateRequest request) {

        try {
            printRequest(request);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 验证设置数据
            if (request.getTheme() != null && !isValidTheme(request.getTheme())) {
                return ResponseEntity.badRequest().body(
                        new UpdateResponse(false, "无效的主题设置", null)
                );
            }

            if (request.getTimeFormat() != null && !isValidTimeFormat(request.getTimeFormat())) {
                return ResponseEntity.badRequest().body(
                        new UpdateResponse(false, "无效的时间格式", null)
                );
            }

            if (request.getPrivacyLevel() != null && !isValidPrivacyLevel(request.getPrivacyLevel())) {
                return ResponseEntity.badRequest().body(
                        new UpdateResponse(false, "无效的隐私级别", null)
                );
            }

            if (request.getFontSize() != null && (request.getFontSize() < 8 || request.getFontSize() > 32)) {
                return ResponseEntity.badRequest().body(
                        new UpdateResponse(false, "字体大小必须在8-32之间", null)
                );
            }

            // 4. 更新设置
            LocalDateTime now = LocalDateTime.now();

            // 检查并更新每个设置项
            updateSetting(userId, "language", request.getLanguage(), now);
            updateSetting(userId, "theme", request.getTheme(), now);
            updateSetting(userId, "fontSize", request.getFontSize() != null ? request.getFontSize().toString() : null, now);
            updateSetting(userId, "timezone", request.getTimezone(), now);
            updateSetting(userId, "dateFormat", request.getDateFormat(), now);
            updateSetting(userId, "timeFormat", request.getTimeFormat(), now);
            updateSetting(userId, "autoSave", request.getAutoSave() != null ? request.getAutoSave().toString() : null, now);
            updateSetting(userId, "autoSaveInterval", request.getAutoSaveInterval() != null ? request.getAutoSaveInterval().toString() : null, now);
            updateSetting(userId, "syncEnabled", request.getSyncEnabled() != null ? request.getSyncEnabled().toString() : null, now);
            updateSetting(userId, "syncInterval", request.getSyncInterval() != null ? request.getSyncInterval().toString() : null, now);
            updateSetting(userId, "dataRetentionDays", request.getDataRetentionDays() != null ? request.getDataRetentionDays().toString() : null, now);
            updateSetting(userId, "privacyLevel", request.getPrivacyLevel(), now);

            // 5. 查询更新后的设置
            String settingsSql = "SELECT setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ? AND setting_type = 'general'";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 6. 构建响应数据
            SettingsGet.SettingsData settingsData = new SettingsGet.SettingsData();

            for (Map<String, Object> setting : settingsList) {
                String key = (String) setting.get("setting_key");
                String value = (String) setting.get("setting_value");

                switch (key) {
                    case "language":
                        settingsData.setLanguage(value);
                        break;
                    case "theme":
                        settingsData.setTheme(value);
                        break;
                    case "fontSize":
                        try {
                            settingsData.setFontSize(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            settingsData.setFontSize(14);
                        }
                        break;
                    case "timezone":
                        settingsData.setTimezone(value);
                        break;
                    case "dateFormat":
                        settingsData.setDateFormat(value);
                        break;
                    case "timeFormat":
                        settingsData.setTimeFormat(value);
                        break;
                    case "autoSave":
                        settingsData.setAutoSave("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "autoSaveInterval":
                        try {
                            settingsData.setAutoSaveInterval(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            settingsData.setAutoSaveInterval(30);
                        }
                        break;
                    case "syncEnabled":
                        settingsData.setSyncEnabled("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "syncInterval":
                        try {
                            settingsData.setSyncInterval(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            settingsData.setSyncInterval(5);
                        }
                        break;
                    case "dataRetentionDays":
                        try {
                            settingsData.setDataRetentionDays(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            settingsData.setDataRetentionDays(90);
                        }
                        break;
                    case "privacyLevel":
                        settingsData.setPrivacyLevel(value);
                        break;
                }

                if (settingsData.getCreatedAt() == null && setting.get("created_at") != null) {
                    settingsData.setCreatedAt(setting.get("created_at").toString());
                }
                if (settingsData.getUpdatedAt() == null && setting.get("updated_at") != null) {
                    settingsData.setUpdatedAt(setting.get("updated_at").toString());
                }
            }

            // 7. 准备响应
            UpdateResponse response = new UpdateResponse(true, "用户设置更新成功", settingsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新用户设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private void updateSetting(Integer userId, String key, String value, LocalDateTime now) {
        if (value != null) {
            // 检查设置是否存在
            String checkSql = "SELECT setting_id FROM user_settings WHERE user_id = ? AND setting_type = 'general' AND setting_key = ?";
            List<Map<String, Object>> existing = jdbcTemplate.queryForList(checkSql, userId, key);

            if (existing.isEmpty()) {
                // 插入新设置
                String insertSql = "INSERT INTO user_settings (user_id, setting_type, setting_key, setting_value, created_at, updated_at) VALUES (?, 'general', ?, ?, ?, ?)";
                jdbcTemplate.update(insertSql, userId, key, value, now, now);
            } else {
                // 更新现有设置
                String updateSql = "UPDATE user_settings SET setting_value = ?, updated_at = ? WHERE user_id = ? AND setting_type = 'general' AND setting_key = ?";
                jdbcTemplate.update(updateSql, value, now, userId, key);
            }
        }
    }

    private boolean isValidTheme(String theme) {
        return theme.equals("light") || theme.equals("dark") || theme.equals("auto");
    }

    private boolean isValidTimeFormat(String timeFormat) {
        return timeFormat.equals("12h") || timeFormat.equals("24h");
    }

    private boolean isValidPrivacyLevel(String privacyLevel) {
        return privacyLevel.equals("minimal") || privacyLevel.equals("standard") || privacyLevel.equals("strict");
    }
}
