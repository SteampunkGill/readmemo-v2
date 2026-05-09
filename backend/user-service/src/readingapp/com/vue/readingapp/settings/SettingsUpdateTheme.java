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
public class SettingsUpdateTheme {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到更新主题设置请求 ===");
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

    public static class UpdateThemeRequest {
        private String theme;
        private String primaryColor;
        private String secondaryColor;
        private String backgroundColor;
        private String textColor;
        private String accentColor;
        private Boolean darkMode;
        private Boolean autoSwitchTheme;
        private String themeSchedule;

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

        public Boolean getDarkMode() { return darkMode; }
        public void setDarkMode(Boolean darkMode) { this.darkMode = darkMode; }

        public Boolean getAutoSwitchTheme() { return autoSwitchTheme; }
        public void setAutoSwitchTheme(Boolean autoSwitchTheme) { this.autoSwitchTheme = autoSwitchTheme; }

        public String getThemeSchedule() { return themeSchedule; }
        public void setThemeSchedule(String themeSchedule) { this.themeSchedule = themeSchedule; }
    }

    public static class UpdateThemeResponse {
        private boolean success;
        private String message;
        private SettingsGetTheme.ThemeSettingsData data;

        public UpdateThemeResponse(boolean success, String message, SettingsGetTheme.ThemeSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SettingsGetTheme.ThemeSettingsData getData() { return data; }
        public void setData(SettingsGetTheme.ThemeSettingsData data) { this.data = data; }
    }

    @PutMapping("")
    public ResponseEntity<UpdateThemeResponse> updateThemeSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateThemeRequest request) {

        try {
            printRequest(request);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateThemeResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateThemeResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 验证主题设置数据
            if (request.getTheme() != null && !isValidTheme(request.getTheme())) {
                return ResponseEntity.badRequest().body(
                        new UpdateThemeResponse(false, "无效的主题设置", null)
                );
            }

            // 验证颜色格式（简单的16进制颜色验证）
            if (request.getPrimaryColor() != null && !isValidColor(request.getPrimaryColor())) {
                return ResponseEntity.badRequest().body(
                        new UpdateThemeResponse(false, "无效的主颜色格式", null)
                );
            }

            if (request.getSecondaryColor() != null && !isValidColor(request.getSecondaryColor())) {
                return ResponseEntity.badRequest().body(
                        new UpdateThemeResponse(false, "无效的次颜色格式", null)
                );
            }

            if (request.getBackgroundColor() != null && !isValidColor(request.getBackgroundColor())) {
                return ResponseEntity.badRequest().body(
                        new UpdateThemeResponse(false, "无效的背景颜色格式", null)
                );
            }

            if (request.getTextColor() != null && !isValidColor(request.getTextColor())) {
                return ResponseEntity.badRequest().body(
                        new UpdateThemeResponse(false, "无效的文本颜色格式", null)
                );
            }

            if (request.getAccentColor() != null && !isValidColor(request.getAccentColor())) {
                return ResponseEntity.badRequest().body(
                        new UpdateThemeResponse(false, "无效的强调颜色格式", null)
                );
            }

            // 4. 更新主题设置
            LocalDateTime now = LocalDateTime.now();

            updateThemeSetting(userId, "theme", request.getTheme(), now);
            updateThemeSetting(userId, "primaryColor", request.getPrimaryColor(), now);
            updateThemeSetting(userId, "secondaryColor", request.getSecondaryColor(), now);
            updateThemeSetting(userId, "backgroundColor", request.getBackgroundColor(), now);
            updateThemeSetting(userId, "textColor", request.getTextColor(), now);
            updateThemeSetting(userId, "accentColor", request.getAccentColor(), now);
            updateThemeSetting(userId, "darkMode", request.getDarkMode() != null ? request.getDarkMode().toString() : null, now);
            updateThemeSetting(userId, "autoSwitchTheme", request.getAutoSwitchTheme() != null ? request.getAutoSwitchTheme().toString() : null, now);
            updateThemeSetting(userId, "themeSchedule", request.getThemeSchedule(), now);

            // 5. 查询更新后的设置
            String settingsSql = "SELECT setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ? AND setting_type = 'theme'";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 6. 构建响应数据
            SettingsGetTheme.ThemeSettingsData settingsData = new SettingsGetTheme.ThemeSettingsData();

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

            // 7. 准备响应
            UpdateThemeResponse response = new UpdateThemeResponse(true, "主题设置更新成功", settingsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新主题设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateThemeResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private void updateThemeSetting(Integer userId, String key, String value, LocalDateTime now) {
        if (value != null) {
            // 检查设置是否存在
            String checkSql = "SELECT setting_id FROM user_settings WHERE user_id = ? AND setting_type = 'theme' AND setting_key = ?";
            List<Map<String, Object>> existing = jdbcTemplate.queryForList(checkSql, userId, key);

            if (existing.isEmpty()) {
                // 插入新设置
                String insertSql = "INSERT INTO user_settings (user_id, setting_type, setting_key, setting_value, created_at, updated_at) VALUES (?, 'theme', ?, ?, ?, ?)";
                jdbcTemplate.update(insertSql, userId, key, value, now, now);
            } else {
                // 更新现有设置
                String updateSql = "UPDATE user_settings SET setting_value = ?, updated_at = ? WHERE user_id = ? AND setting_type = 'theme' AND setting_key = ?";
                jdbcTemplate.update(updateSql, value, now, userId, key);
            }
        }
    }

    private boolean isValidTheme(String theme) {
        return theme.equals("light") || theme.equals("dark") || theme.equals("auto") ||
                theme.equals("sepia") || theme.equals("night");
    }

    private boolean isValidColor(String color) {
        // 简单的16进制颜色验证，支持 #RGB, #RRGGBB, #RRGGBBAA 格式
        return color.matches("^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$");
    }
}
