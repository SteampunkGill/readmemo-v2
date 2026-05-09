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
@RequestMapping("/api/v1/user/settings/theme")
public class SettingsGetTheme {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到获取主题设置请求 ===");
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

    public static class ThemeSettingsResponse {
        private boolean success;
        private String message;
        private ThemeSettingsData data;

        public ThemeSettingsResponse(boolean success, String message, ThemeSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ThemeSettingsData getData() { return data; }
        public void setData(ThemeSettingsData data) { this.data = data; }
    }

    public static class ThemeSettingsData {
        private String theme;
        private String primaryColor;
        private String secondaryColor;
        private String backgroundColor;
        private String textColor;
        private String accentColor;
        private boolean darkMode;
        private boolean autoSwitchTheme;
        private String themeSchedule;

        public ThemeSettingsData() {
            // 设置默认值
            this.theme = "light";
            this.primaryColor = "#1976D2";
            this.secondaryColor = "#424242";
            this.backgroundColor = "#FFFFFF";
            this.textColor = "#000000";
            this.accentColor = "#FF4081";
            this.darkMode = false;
            this.autoSwitchTheme = false;
            this.themeSchedule = "sunset";
        }

        // Getters and Setters
        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }

        public String getPrimaryColor() { return primaryColor; }
        public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }

        public String getSecondaryColor() { return secondaryColor; }
        public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }

        public String getBackgroundColor() { return backgroundColor; }
        public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }

        public String getTextColor() { return textColor; }
        public void setTextColor(String textColor) { this.textColor = textColor; }

        public String getAccentColor() { return accentColor; }
        public void setAccentColor(String accentColor) { this.accentColor = accentColor; }

        public boolean isDarkMode() { return darkMode; }
        public void setDarkMode(boolean darkMode) { this.darkMode = darkMode; }

        public boolean isAutoSwitchTheme() { return autoSwitchTheme; }
        public void setAutoSwitchTheme(boolean autoSwitchTheme) { this.autoSwitchTheme = autoSwitchTheme; }

        public String getThemeSchedule() { return themeSchedule; }
        public void setThemeSchedule(String themeSchedule) { this.themeSchedule = themeSchedule; }
    }

    @GetMapping("")
    public ResponseEntity<ThemeSettingsResponse> getThemeSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            printRequest("Authorization: " + authHeader);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ThemeSettingsResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ThemeSettingsResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 查询主题设置
            String settingsSql = "SELECT setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ? AND setting_type = 'theme'";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 4. 构建响应数据
            ThemeSettingsData settingsData = new ThemeSettingsData();

            for (Map<String, Object> setting : settingsList) {
                String key = (String) setting.get("setting_key");
                String value = (String) setting.get("setting_value");

                switch (key) {
                    case "theme":
                        settingsData.setTheme(value);
                        break;
                    case "primaryColor":
                        settingsData.setPrimaryColor(value);
                        break;
                    case "secondaryColor":
                        settingsData.setSecondaryColor(value);
                        break;
                    case "backgroundColor":
                        settingsData.setBackgroundColor(value);
                        break;
                    case "textColor":
                        settingsData.setTextColor(value);
                        break;
                    case "accentColor":
                        settingsData.setAccentColor(value);
                        break;
                    case "darkMode":
                        settingsData.setDarkMode("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "autoSwitchTheme":
                        settingsData.setAutoSwitchTheme("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "themeSchedule":
                        settingsData.setThemeSchedule(value);
                        break;
                }
            }

            // 5. 准备响应
            ThemeSettingsResponse response = new ThemeSettingsResponse(true, "获取主题设置成功", settingsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取主题设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ThemeSettingsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
