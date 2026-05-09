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
@RequestMapping("/api/v1/user/settings/reading")
public class SettingsUpdateReading {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到更新阅读设置请求 ===");
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

    public static class UpdateReadingRequest {
        private String fontFamily;
        private Integer fontSize;
        private Double lineHeight;
        private Double paragraphSpacing;
        private String theme;
        private String contrast;
        private String highlightColor;
        private String noteColor;
        private Boolean autoScroll;
        private Double scrollSpeed;
        private Boolean showTranslation;
        private String translationPosition;
        private Boolean showPhonetic;
        private Boolean showDefinition;
        private Boolean autoPlayAudio;
        private Double audioVolume;
        private Boolean textSelection;
        private Boolean doubleClickTranslate;
        private String wordBreak;
        private String textAlign;
        private Integer maxWidth;
        private Integer margin;

        // Getters and Setters
        public String getFontFamily() { return fontFamily; }
        public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }

        public Integer getFontSize() { return fontSize; }
        public void setFontSize(Integer fontSize) { this.fontSize = fontSize; }

        public Double getLineHeight() { return lineHeight; }
        public void setLineHeight(Double lineHeight) { this.lineHeight = lineHeight; }

        public Double getParagraphSpacing() { return paragraphSpacing; }
        public void setParagraphSpacing(Double paragraphSpacing) { this.paragraphSpacing = paragraphSpacing; }

        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }

        public String getContrast() { return contrast; }
        public void setContrast(String contrast) { this.contrast = contrast; }

        public String getHighlightColor() { return highlightColor; }
        public void setHighlightColor(String highlightColor) { this.highlightColor = highlightColor; }

        public String getNoteColor() { return noteColor; }
        public void setNoteColor(String noteColor) { this.noteColor = noteColor; }

        public Boolean getAutoScroll() { return autoScroll; }
        public void setAutoScroll(Boolean autoScroll) { this.autoScroll = autoScroll; }

        public Double getScrollSpeed() { return scrollSpeed; }
        public void setScrollSpeed(Double scrollSpeed) { this.scrollSpeed = scrollSpeed; }

        public Boolean getShowTranslation() { return showTranslation; }
        public void setShowTranslation(Boolean showTranslation) { this.showTranslation = showTranslation; }

        public String getTranslationPosition() { return translationPosition; }
        public void setTranslationPosition(String translationPosition) { this.translationPosition = translationPosition; }

        public Boolean getShowPhonetic() { return showPhonetic; }
        public void setShowPhonetic(Boolean showPhonetic) { this.showPhonetic = showPhonetic; }

        public Boolean getShowDefinition() { return showDefinition; }
        public void setShowDefinition(Boolean showDefinition) { this.showDefinition = showDefinition; }

        public Boolean getAutoPlayAudio() { return autoPlayAudio; }
        public void setAutoPlayAudio(Boolean autoPlayAudio) { this.autoPlayAudio = autoPlayAudio; }

        public Double getAudioVolume() { return audioVolume; }
        public void setAudioVolume(Double audioVolume) { this.audioVolume = audioVolume; }

        public Boolean getTextSelection() { return textSelection; }
        public void setTextSelection(Boolean textSelection) { this.textSelection = textSelection; }

        public Boolean getDoubleClickTranslate() { return doubleClickTranslate; }
        public void setDoubleClickTranslate(Boolean doubleClickTranslate) { this.doubleClickTranslate = doubleClickTranslate; }

        public String getWordBreak() { return wordBreak; }
        public void setWordBreak(String wordBreak) { this.wordBreak = wordBreak; }

        public String getTextAlign() { return textAlign; }
        public void setTextAlign(String textAlign) { this.textAlign = textAlign; }

        public Integer getMaxWidth() { return maxWidth; }
        public void setMaxWidth(Integer maxWidth) { this.maxWidth = maxWidth; }

        public Integer getMargin() { return margin; }
        public void setMargin(Integer margin) { this.margin = margin; }
    }

    public static class UpdateReadingResponse {
        private boolean success;
        private String message;
        private SettingsGetReading.ReadingSettingsData data;

        public UpdateReadingResponse(boolean success, String message, SettingsGetReading.ReadingSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SettingsGetReading.ReadingSettingsData getData() { return data; }
        public void setData(SettingsGetReading.ReadingSettingsData data) { this.data = data; }
    }

    @PutMapping("")
    public ResponseEntity<UpdateReadingResponse> updateReadingSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateReadingRequest request) {

        try {
            printRequest(request);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateReadingResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateReadingResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 验证阅读设置数据
            if (request.getTheme() != null && !isValidReadingTheme(request.getTheme())) {
                return ResponseEntity.badRequest().body(
                        new UpdateReadingResponse(false, "无效的阅读主题", null)
                );
            }

            if (request.getContrast() != null && !isValidContrast(request.getContrast())) {
                return ResponseEntity.badRequest().body(
                        new UpdateReadingResponse(false, "无效的对比度设置", null)
                );
            }

            if (request.getTranslationPosition() != null && !isValidTranslationPosition(request.getTranslationPosition())) {
                return ResponseEntity.badRequest().body(
                        new UpdateReadingResponse(false, "无效的翻译位置", null)
                );
            }

            if (request.getTextAlign() != null && !isValidTextAlign(request.getTextAlign())) {
                return ResponseEntity.badRequest().body(
                        new UpdateReadingResponse(false, "无效的文本对齐方式", null)
                );
            }

            if (request.getFontSize() != null && (request.getFontSize() < 10 || request.getFontSize() > 32)) {
                return ResponseEntity.badRequest().body(
                        new UpdateReadingResponse(false, "阅读字体大小必须在10-32之间", null)
                );
            }

            if (request.getAudioVolume() != null && (request.getAudioVolume() < 0 || request.getAudioVolume() > 1)) {
                return ResponseEntity.badRequest().body(
                        new UpdateReadingResponse(false, "音量必须在0-1之间", null)
                );
            }

            // 4. 更新阅读设置
            LocalDateTime now = LocalDateTime.now();

            // 检查并更新每个设置项
            updateReadingSetting(userId, "fontFamily", request.getFontFamily(), now);
            updateReadingSetting(userId, "fontSize", request.getFontSize() != null ? request.getFontSize().toString() : null, now);
            updateReadingSetting(userId, "lineHeight", request.getLineHeight() != null ? request.getLineHeight().toString() : null, now);
            updateReadingSetting(userId, "paragraphSpacing", request.getParagraphSpacing() != null ? request.getParagraphSpacing().toString() : null, now);
            updateReadingSetting(userId, "theme", request.getTheme(), now);
            updateReadingSetting(userId, "contrast", request.getContrast(), now);
            updateReadingSetting(userId, "highlightColor", request.getHighlightColor(), now);
            updateReadingSetting(userId, "noteColor", request.getNoteColor(), now);
            updateReadingSetting(userId, "autoScroll", request.getAutoScroll() != null ? request.getAutoScroll().toString() : null, now);
            updateReadingSetting(userId, "scrollSpeed", request.getScrollSpeed() != null ? request.getScrollSpeed().toString() : null, now);
            updateReadingSetting(userId, "showTranslation", request.getShowTranslation() != null ? request.getShowTranslation().toString() : null, now);
            updateReadingSetting(userId, "translationPosition", request.getTranslationPosition(), now);
            updateReadingSetting(userId, "showPhonetic", request.getShowPhonetic() != null ? request.getShowPhonetic().toString() : null, now);
            updateReadingSetting(userId, "showDefinition", request.getShowDefinition() != null ? request.getShowDefinition().toString() : null, now);
            updateReadingSetting(userId, "autoPlayAudio", request.getAutoPlayAudio() != null ? request.getAutoPlayAudio().toString() : null, now);
            updateReadingSetting(userId, "audioVolume", request.getAudioVolume() != null ? request.getAudioVolume().toString() : null, now);
            updateReadingSetting(userId, "textSelection", request.getTextSelection() != null ? request.getTextSelection().toString() : null, now);
            updateReadingSetting(userId, "doubleClickTranslate", request.getDoubleClickTranslate() != null ? request.getDoubleClickTranslate().toString() : null, now);
            updateReadingSetting(userId, "wordBreak", request.getWordBreak(), now);
            updateReadingSetting(userId, "textAlign", request.getTextAlign(), now);
            updateReadingSetting(userId, "maxWidth", request.getMaxWidth() != null ? request.getMaxWidth().toString() : null, now);
            updateReadingSetting(userId, "margin", request.getMargin() != null ? request.getMargin().toString() : null, now);

            // 5. 查询更新后的设置
            String settingsSql = "SELECT setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ? AND setting_type = 'reading'";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 6. 构建响应数据
            SettingsGetReading.ReadingSettingsData settingsData = new SettingsGetReading.ReadingSettingsData();

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
                    case "paragraphSpacing":
                        try {
                            settingsData.setParagraphSpacing(Double.parseDouble(value));
                        } catch (NumberFormatException e) {
                            settingsData.setParagraphSpacing(1.2);
                        }
                        break;
                    case "theme":
                        settingsData.setTheme(value);
                        break;
                    case "contrast":
                        settingsData.setContrast(value);
                        break;
                    case "highlightColor":
                        settingsData.setHighlightColor(value);
                        break;
                    case "noteColor":
                        settingsData.setNoteColor(value);
                        break;
                    case "autoScroll":
                        settingsData.setAutoScroll("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "scrollSpeed":
                        try {
                            settingsData.setScrollSpeed(Double.parseDouble(value));
                        } catch (NumberFormatException e) {
                            settingsData.setScrollSpeed(1.0);
                        }
                        break;
                    case "showTranslation":
                        settingsData.setShowTranslation("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "translationPosition":
                        settingsData.setTranslationPosition(value);
                        break;
                    case "showPhonetic":
                        settingsData.setShowPhonetic("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "showDefinition":
                        settingsData.setShowDefinition("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "autoPlayAudio":
                        settingsData.setAutoPlayAudio("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "audioVolume":
                        try {
                            settingsData.setAudioVolume(Double.parseDouble(value));
                        } catch (NumberFormatException e) {
                            settingsData.setAudioVolume(0.7);
                        }
                        break;
                    case "textSelection":
                        settingsData.setTextSelection("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "doubleClickTranslate":
                        settingsData.setDoubleClickTranslate("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "wordBreak":
                        settingsData.setWordBreak(value);
                        break;
                    case "textAlign":
                        settingsData.setTextAlign(value);
                        break;
                    case "maxWidth":
                        try {
                            settingsData.setMaxWidth(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            settingsData.setMaxWidth(800);
                        }
                        break;
                    case "margin":
                        try {
                            settingsData.setMargin(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            settingsData.setMargin(20);
                        }
                        break;
                }
            }

            // 7. 准备响应
            UpdateReadingResponse response = new UpdateReadingResponse(true, "阅读设置更新成功", settingsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新阅读设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateReadingResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private void updateReadingSetting(Integer userId, String key, String value, LocalDateTime now) {
        if (value != null) {
            // 检查设置是否存在
            String checkSql = "SELECT setting_id FROM user_settings WHERE user_id = ? AND setting_type = 'reading' AND setting_key = ?";
            List<Map<String, Object>> existing = jdbcTemplate.queryForList(checkSql, userId, key);

            if (existing.isEmpty()) {
                // 插入新设置
                String insertSql = "INSERT INTO user_settings (user_id, setting_type, setting_key, setting_value, created_at, updated_at) VALUES (?, 'reading', ?, ?, ?, ?)";
                jdbcTemplate.update(insertSql, userId, key, value, now, now);
            } else {
                // 更新现有设置
                String updateSql = "UPDATE user_settings SET setting_value = ?, updated_at = ? WHERE user_id = ? AND setting_type = 'reading' AND setting_key = ?";
                jdbcTemplate.update(updateSql, value, now, userId, key);
            }
        }
    }

    private boolean isValidReadingTheme(String theme) {
        return theme.equals("light") || theme.equals("dark") || theme.equals("sepia") || theme.equals("night");
    }

    private boolean isValidContrast(String contrast) {
        return contrast.equals("normal") || contrast.equals("high") || contrast.equals("low");
    }

    private boolean isValidTranslationPosition(String position) {
        return position.equals("inline") || position.equals("sidebar") || position.equals("popup");
    }

    private boolean isValidTextAlign(String align) {
        return align.equals("left") || align.equals("right") || align.equals("center") || align.equals("justify");
    }
}
