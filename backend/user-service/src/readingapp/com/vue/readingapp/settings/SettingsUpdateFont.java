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
public class SettingsUpdateFont {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到更新字体设置请求 ===");
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

    public static class UpdateFontRequest {
        private String fontFamily;
        private Integer fontSize;
        private Double lineHeight;
        private Double letterSpacing;
        private String fontWeight;
        private String fontStyle;
        private Boolean useCustomFont;
        private String customFontUrl;
        private String fontDisplay;
        private Boolean optimizeForReadability;

        // Getters and Setters
        public String getFontFamily() { return fontFamily; }
        public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }

        public Integer getFontSize() { return fontSize; }
        public void setFontSize(Integer fontSize) { this.fontSize = fontSize; }

        public Double getLineHeight() { return lineHeight; }
        public void setLineHeight(Double lineHeight) { this.lineHeight = lineHeight; }

        public Double getLetterSpacing() { return letterSpacing; }
        public void setLetterSpacing(Double letterSpacing) { this.letterSpacing = letterSpacing; }

        public String getFontWeight() { return fontWeight; }
        public void setFontWeight(String fontWeight) { this.fontWeight = fontWeight; }

        public String getFontStyle() { return fontStyle; }
        public void setFontStyle(String fontStyle) { this.fontStyle = fontStyle; }

        public Boolean getUseCustomFont() { return useCustomFont; }
        public void setUseCustomFont(Boolean useCustomFont) { this.useCustomFont = useCustomFont; }

        public String getCustomFontUrl() { return customFontUrl; }
        public void setCustomFontUrl(String customFontUrl) { this.customFontUrl = customFontUrl; }

        public String getFontDisplay() { return fontDisplay; }
        public void setFontDisplay(String fontDisplay) { this.fontDisplay = fontDisplay; }

        public Boolean getOptimizeForReadability() { return optimizeForReadability; }
        public void setOptimizeForReadability(Boolean optimizeForReadability) { this.optimizeForReadability = optimizeForReadability; }
    }

    public static class UpdateFontResponse {
        private boolean success;
        private String message;
        private SettingsGetFont.FontSettingsData data;

        public UpdateFontResponse(boolean success, String message, SettingsGetFont.FontSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SettingsGetFont.FontSettingsData getData() { return data; }
        public void setData(SettingsGetFont.FontSettingsData data) { this.data = data; }
    }

    @PutMapping("")
    public ResponseEntity<UpdateFontResponse> updateFontSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateFontRequest request) {

        try {
            printRequest(request);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateFontResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id, expires_at FROM user_sessions WHERE access_token = ?";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateFontResponse(false, "会话已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            Integer userId = (Integer) session.get("user_id");
            LocalDateTime expiresAt = (LocalDateTime) session.get("expires_at");

            if (expiresAt.isBefore(LocalDateTime.now())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateFontResponse(false, "会话已过期，请重新登录", null)
                );
            }

            // 3. 验证字体设置数据
            if (request.getFontSize() != null && (request.getFontSize() < 8 || request.getFontSize() > 48)) {
                return ResponseEntity.badRequest().body(
                        new UpdateFontResponse(false, "字体大小必须在8-48之间", null)
                );
            }

            if (request.getLineHeight() != null && (request.getLineHeight() < 1.0 || request.getLineHeight() > 3.0)) {
                return ResponseEntity.badRequest().body(
                        new UpdateFontResponse(false, "行高必须在1.0-3.0之间", null)
                );
            }

            if (request.getLetterSpacing() != null && (request.getLetterSpacing() < -0.1 || request.getLetterSpacing() > 0.5)) {
                return ResponseEntity.badRequest().body(
                        new UpdateFontResponse(false, "字间距必须在-0.1到0.5之间", null)
                );
            }

            // 验证字体粗细
            if (request.getFontWeight() != null && !isValidFontWeight(request.getFontWeight())) {
                return ResponseEntity.badRequest().body(
                        new UpdateFontResponse(false, "无效的字体粗细", null)
                );
            }

            // 验证字体样式
            if (request.getFontStyle() != null && !isValidFontStyle(request.getFontStyle())) {
                return ResponseEntity.badRequest().body(
                        new UpdateFontResponse(false, "无效的字体样式", null)
                );
            }

            // 验证字体显示方式
            if (request.getFontDisplay() != null && !isValidFontDisplay(request.getFontDisplay())) {
                return ResponseEntity.badRequest().body(
                        new UpdateFontResponse(false, "无效的字体显示方式", null)
                );
            }

            // 4. 更新字体设置
            LocalDateTime now = LocalDateTime.now();

            updateFontSetting(userId, "fontFamily", request.getFontFamily(), now);
            updateFontSetting(userId, "fontSize", request.getFontSize() != null ? request.getFontSize().toString() : null, now);
            updateFontSetting(userId, "lineHeight", request.getLineHeight() != null ? request.getLineHeight().toString() : null, now);
            updateFontSetting(userId, "letterSpacing", request.getLetterSpacing() != null ? request.getLetterSpacing().toString() : null, now);
            updateFontSetting(userId, "fontWeight", request.getFontWeight(), now);
            updateFontSetting(userId, "fontStyle", request.getFontStyle(), now);
            updateFontSetting(userId, "useCustomFont", request.getUseCustomFont() != null ? request.getUseCustomFont().toString() : null, now);
            updateFontSetting(userId, "customFontUrl", request.getCustomFontUrl(), now);
            updateFontSetting(userId, "fontDisplay", request.getFontDisplay(), now);
            updateFontSetting(userId, "optimizeForReadability", request.getOptimizeForReadability() != null ? request.getOptimizeForReadability().toString() : null, now);

            // 5. 查询更新后的设置
            String settingsSql = "SELECT setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ? AND setting_type = 'font'";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 6. 构建响应数据
            SettingsGetFont.FontSettingsData settingsData = new SettingsGetFont.FontSettingsData();

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

            // 7. 准备响应
            UpdateFontResponse response = new UpdateFontResponse(true, "字体设置更新成功", settingsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新字体设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateFontResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private void updateFontSetting(Integer userId, String key, String value, LocalDateTime now) {
        if (value != null) {
            // 检查设置是否存在
            String checkSql = "SELECT setting_id FROM user_settings WHERE user_id = ? AND setting_type = 'font' AND setting_key = ?";
            List<Map<String, Object>> existing = jdbcTemplate.queryForList(checkSql, userId, key);

            if (existing.isEmpty()) {
                // 插入新设置
                String insertSql = "INSERT INTO user_settings (user_id, setting_type, setting_key, setting_value, created_at, updated_at) VALUES (?, 'font', ?, ?, ?, ?)";
                jdbcTemplate.update(insertSql, userId, key, value, now, now);
            } else {
                // 更新现有设置
                String updateSql = "UPDATE user_settings SET setting_value = ?, updated_at = ? WHERE user_id = ? AND setting_type = 'font' AND setting_key = ?";
                jdbcTemplate.update(updateSql, value, now, userId, key);
            }
        }
    }

    private boolean isValidFontWeight(String fontWeight) {
        return fontWeight.equals("normal") || fontWeight.equals("bold") ||
                fontWeight.equals("lighter") || fontWeight.equals("bolder") ||
                fontWeight.matches("^[1-9]00$"); // 100, 200, ..., 900
    }

    private boolean isValidFontStyle(String fontStyle) {
        return fontStyle.equals("normal") || fontStyle.equals("italic") ||
                fontStyle.equals("oblique");
    }

    private boolean isValidFontDisplay(String fontDisplay) {
        return fontDisplay.equals("auto") || fontDisplay.equals("block") ||
                fontDisplay.equals("swap") || fontDisplay.equals("fallback") ||
                fontDisplay.equals("optional");
    }
}
