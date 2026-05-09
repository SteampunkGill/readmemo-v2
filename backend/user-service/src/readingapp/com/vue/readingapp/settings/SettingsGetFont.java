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
@RequestMapping("/api/v1/user/settings/font")
public class SettingsGetFont {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到获取字体设置请求 ===");
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

    public static class FontSettingsResponse {
        private boolean success;
        private String message;
        private FontSettingsData data;

        public FontSettingsResponse(boolean success, String message, FontSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public FontSettingsData getData() { return data; }
        public void setData(FontSettingsData data) { this.data = data; }
    }

    public static class FontSettingsData {
        private String fontFamily;
        private int fontSize;
        private double lineHeight;
        private double letterSpacing;
        private String fontWeight;
        private String fontStyle;
        private boolean useCustomFont;
        private String customFontUrl;
        private String fontDisplay;
        private boolean optimizeForReadability;

        public FontSettingsData() {
            // 设置默认值
            this.fontFamily = "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif";
            this.fontSize = 16;
            this.lineHeight = 1.6;
            this.letterSpacing = 0;
            this.fontWeight = "normal";
            this.fontStyle = "normal";
            this.useCustomFont = false;
            this.customFontUrl = "";
            this.fontDisplay = "swap";
            this.optimizeForReadability = true;
        }

        // Getters and Setters
        public String getFontFamily() { return fontFamily; }
        public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }

        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }

        public double getLineHeight() { return lineHeight; }
        public void setLineHeight(double lineHeight) { this.lineHeight = lineHeight; }

        public double getLetterSpacing() { return letterSpacing; }
        public void setLetterSpacing(double letterSpacing) { this.letterSpacing = letterSpacing; }

        public String getFontWeight() { return fontWeight; }
        public void setFontWeight(String fontWeight) { this.fontWeight = fontWeight; }

        public String getFontStyle() { return fontStyle; }
        public void setFontStyle(String fontStyle) { this.fontStyle = fontStyle; }

        public boolean isUseCustomFont() { return useCustomFont; }
        public void setUseCustomFont(boolean useCustomFont) { this.useCustomFont = useCustomFont; }

        public String getCustomFontUrl() { return customFontUrl; }
        public void setCustomFontUrl(String customFontUrl) { this.customFontUrl = customFontUrl; }

        public String getFontDisplay() { return fontDisplay; }
        public void setFontDisplay(String fontDisplay) { this.fontDisplay = fontDisplay; }

        public boolean isOptimizeForReadability() { return optimizeForReadability; }
        public void setOptimizeForReadability(boolean optimizeForReadability) { this.optimizeForReadability = optimizeForReadability; }
    }

    @GetMapping("")
    public ResponseEntity<FontSettingsResponse> getFontSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            printRequest("Authorization: " + authHeader);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new FontSettingsResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id, expires_at FROM user_sessions WHERE access_token = ?";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new FontSettingsResponse(false, "会话已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            Integer userId = (Integer) session.get("user_id");
            LocalDateTime expiresAt = (LocalDateTime) session.get("expires_at");

            if (expiresAt.isBefore(LocalDateTime.now())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new FontSettingsResponse(false, "会话已过期，请重新登录", null)
                );
            }

            // 3. 查询字体设置
            String settingsSql = "SELECT setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ? AND setting_type = 'font'";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 4. 构建响应数据
            FontSettingsData settingsData = new FontSettingsData();

            for (Map<String, Object> setting : settingsList) {
                String key = (String) setting.get("setting_key");
                String value = (String) setting.get("setting_value");

                switch (key) {
                    case "fontFamily":
                        settingsData.setFontFamily(value);
                        break;
                    case "fontSize":
                        try {
                            settingsData.setFontSize(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            settingsData.setFontSize(16);
                        }
                        break;
                    case "lineHeight":
                        try {
                            settingsData.setLineHeight(Double.parseDouble(value));
                        } catch (NumberFormatException e) {
                            settingsData.setLineHeight(1.6);
                        }
                        break;
                    case "letterSpacing":
                        try {
                            settingsData.setLetterSpacing(Double.parseDouble(value));
                        } catch (NumberFormatException e) {
                            settingsData.setLetterSpacing(0);
                        }
                        break;
                    case "fontWeight":
                        settingsData.setFontWeight(value);
                        break;
                    case "fontStyle":
                        settingsData.setFontStyle(value);
                        break;
                    case "useCustomFont":
                        settingsData.setUseCustomFont("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "customFontUrl":
                        settingsData.setCustomFontUrl(value);
                        break;
                    case "fontDisplay":
                        settingsData.setFontDisplay(value);
                        break;
                    case "optimizeForReadability":
                        settingsData.setOptimizeForReadability("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                }
            }

            // 5. 准备响应
            FontSettingsResponse response = new FontSettingsResponse(true, "获取字体设置成功", settingsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取字体设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new FontSettingsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
