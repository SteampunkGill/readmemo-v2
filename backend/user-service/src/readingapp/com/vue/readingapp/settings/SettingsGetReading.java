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
public class SettingsGetReading {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到获取阅读设置请求 ===");
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

    public static class ReadingSettingsResponse {
        private boolean success;
        private String message;
        private ReadingSettingsData data;

        public ReadingSettingsResponse(boolean success, String message, ReadingSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ReadingSettingsData getData() { return data; }
        public void setData(ReadingSettingsData data) { this.data = data; }
    }

    public static class ReadingSettingsData {
        private String fontFamily;
        private int fontSize;
        private double lineHeight;
        private double paragraphSpacing;
        private String theme;
        private String contrast;
        private String highlightColor;
        private String noteColor;
        private boolean autoScroll;
        private double scrollSpeed;
        private boolean showTranslation;
        private String translationPosition;
        private boolean showPhonetic;
        private boolean showDefinition;
        private boolean autoPlayAudio;
        private double audioVolume;
        private boolean textSelection;
        private boolean doubleClickTranslate;
        private String wordBreak;
        private String textAlign;
        private int maxWidth;
        private int margin;

        public ReadingSettingsData() {
            // 设置默认值
            this.fontFamily = "system-ui";
            this.fontSize = 16;
            this.lineHeight = 1.6;
            this.paragraphSpacing = 1.2;
            this.theme = "light";
            this.contrast = "normal";
            this.highlightColor = "#FFEB3B";
            this.noteColor = "#4CAF50";
            this.autoScroll = false;
            this.scrollSpeed = 1.0;
            this.showTranslation = true;
            this.translationPosition = "inline";
            this.showPhonetic = true;
            this.showDefinition = true;
            this.autoPlayAudio = false;
            this.audioVolume = 0.7;
            this.textSelection = true;
            this.doubleClickTranslate = true;
            this.wordBreak = "normal";
            this.textAlign = "left";
            this.maxWidth = 800;
            this.margin = 20;
        }

        // Getters and Setters
        public String getFontFamily() { return fontFamily; }
        public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }

        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }

        public double getLineHeight() { return lineHeight; }
        public void setLineHeight(double lineHeight) { this.lineHeight = lineHeight; }

        public double getParagraphSpacing() { return paragraphSpacing; }
        public void setParagraphSpacing(double paragraphSpacing) { this.paragraphSpacing = paragraphSpacing; }

        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }

        public String getContrast() { return contrast; }
        public void setContrast(String contrast) { this.contrast = contrast; }

        public String getHighlightColor() { return highlightColor; }
        public void setHighlightColor(String highlightColor) { this.highlightColor = highlightColor; }

        public String getNoteColor() { return noteColor; }
        public void setNoteColor(String noteColor) { this.noteColor = noteColor; }

        public boolean isAutoScroll() { return autoScroll; }
        public void setAutoScroll(boolean autoScroll) { this.autoScroll = autoScroll; }

        public double getScrollSpeed() { return scrollSpeed; }
        public void setScrollSpeed(double scrollSpeed) { this.scrollSpeed = scrollSpeed; }

        public boolean isShowTranslation() { return showTranslation; }
        public void setShowTranslation(boolean showTranslation) { this.showTranslation = showTranslation; }

        public String getTranslationPosition() { return translationPosition; }
        public void setTranslationPosition(String translationPosition) { this.translationPosition = translationPosition; }

        public boolean isShowPhonetic() { return showPhonetic; }
        public void setShowPhonetic(boolean showPhonetic) { this.showPhonetic = showPhonetic; }

        public boolean isShowDefinition() { return showDefinition; }
        public void setShowDefinition(boolean showDefinition) { this.showDefinition = showDefinition; }

        public boolean isAutoPlayAudio() { return autoPlayAudio; }
        public void setAutoPlayAudio(boolean autoPlayAudio) { this.autoPlayAudio = autoPlayAudio; }

        public double getAudioVolume() { return audioVolume; }
        public void setAudioVolume(double audioVolume) { this.audioVolume = audioVolume; }

        public boolean isTextSelection() { return textSelection; }
        public void setTextSelection(boolean textSelection) { this.textSelection = textSelection; }

        public boolean isDoubleClickTranslate() { return doubleClickTranslate; }
        public void setDoubleClickTranslate(boolean doubleClickTranslate) { this.doubleClickTranslate = doubleClickTranslate; }

        public String getWordBreak() { return wordBreak; }
        public void setWordBreak(String wordBreak) { this.wordBreak = wordBreak; }

        public String getTextAlign() { return textAlign; }
        public void setTextAlign(String textAlign) { this.textAlign = textAlign; }

        public int getMaxWidth() { return maxWidth; }
        public void setMaxWidth(int maxWidth) { this.maxWidth = maxWidth; }

        public int getMargin() { return margin; }
        public void setMargin(int margin) { this.margin = margin; }
    }

    @GetMapping("")
    public ResponseEntity<ReadingSettingsResponse> getReadingSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            printRequest("Authorization: " + authHeader);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReadingSettingsResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReadingSettingsResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 查询阅读设置
            String settingsSql = "SELECT setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ? AND setting_type = 'reading'";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 4. 构建响应数据
            ReadingSettingsData settingsData = new ReadingSettingsData();

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

            // 5. 准备响应
            ReadingSettingsResponse response = new ReadingSettingsResponse(true, "获取阅读设置成功", settingsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取阅读设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReadingSettingsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
