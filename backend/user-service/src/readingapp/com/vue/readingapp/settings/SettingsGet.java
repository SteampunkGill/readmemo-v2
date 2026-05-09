
package com.vue.readingapp.settings;

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
@RequestMapping("/api/v1/user/settings")
public class SettingsGet {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取用户设置请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("========================");
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
    public static class SettingsResponse {
        private boolean success;
        private String message;
        private SettingsData data;

        public SettingsResponse(boolean success, String message, SettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SettingsData getData() { return data; }
        public void setData(SettingsData data) { this.data = data; }
    }

    public static class SettingsData {
        private String language;
        private String theme;
        private int fontSize;
        private String timezone;
        private String dateFormat;
        private String timeFormat;
        private boolean autoSave;
        private int autoSaveInterval;
        private boolean syncEnabled;
        private int syncInterval;
        private int dataRetentionDays;
        private String privacyLevel;
        private String createdAt;
        private String updatedAt;

        // 默认构造函数
        public SettingsData() {
            // 设置默认值
            this.language = "zh-CN";
            this.theme = "light";
            this.fontSize = 14;
            this.timezone = "Asia/Shanghai";
            this.dateFormat = "YYYY-MM-DD";
            this.timeFormat = "24h";
            this.autoSave = true;
            this.autoSaveInterval = 30;
            this.syncEnabled = true;
            this.syncInterval = 5;
            this.dataRetentionDays = 90;
            this.privacyLevel = "standard";
        }

        // Getters and Setters
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }

        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }

        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }

        public String getDateFormat() { return dateFormat; }
        public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }

        public String getTimeFormat() { return timeFormat; }
        public void setTimeFormat(String timeFormat) { this.timeFormat = timeFormat; }

        public boolean isAutoSave() { return autoSave; }
        public void setAutoSave(boolean autoSave) { this.autoSave = autoSave; }

        public int getAutoSaveInterval() { return autoSaveInterval; }
        public void setAutoSaveInterval(int autoSaveInterval) { this.autoSaveInterval = autoSaveInterval; }

        public boolean isSyncEnabled() { return syncEnabled; }
        public void setSyncEnabled(boolean syncEnabled) { this.syncEnabled = syncEnabled; }

        public int getSyncInterval() { return syncInterval; }
        public void setSyncInterval(int syncInterval) { this.syncInterval = syncInterval; }

        public int getDataRetentionDays() { return dataRetentionDays; }
        public void setDataRetentionDays(int dataRetentionDays) { this.dataRetentionDays = dataRetentionDays; }

        public String getPrivacyLevel() { return privacyLevel; }
        public void setPrivacyLevel(String privacyLevel) { this.privacyLevel = privacyLevel; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    @GetMapping("")
    public ResponseEntity<SettingsResponse> getSettings(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // 打印接收到的请求
            printRequest("Authorization: " + authHeader);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SettingsResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SettingsResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 查询用户设置
            String settingsSql = "SELECT setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ? AND setting_type = 'general'";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 4. 构建响应数据
            SettingsData settingsData = new SettingsData();

            // 如果有设置，覆盖默认值
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

                // 设置创建和更新时间（取第一条记录的）
                if (settingsData.getCreatedAt() == null && setting.get("created_at") != null) {
                    settingsData.setCreatedAt(setting.get("created_at").toString());
                }
                if (settingsData.getUpdatedAt() == null && setting.get("updated_at") != null) {
                    settingsData.setUpdatedAt(setting.get("updated_at").toString());
                }
            }

            // 5. 准备响应
            SettingsResponse response = new SettingsResponse(true, "获取用户设置成功", settingsData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取用户设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SettingsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}