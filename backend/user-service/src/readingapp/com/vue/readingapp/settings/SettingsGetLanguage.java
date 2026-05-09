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
@RequestMapping("/api/v1/user/settings/language")
public class SettingsGetLanguage {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到获取语言设置请求 ===");
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

    public static class LanguageSettingsResponse {
        private boolean success;
        private String message;
        private LanguageSettingsData data;

        public LanguageSettingsResponse(boolean success, String message, LanguageSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public LanguageSettingsData getData() { return data; }
        public void setData(LanguageSettingsData data) { this.data = data; }
    }

    public static class LanguageSettingsData {
        private String language;
        private String defaultDictionary;
        private boolean autoDetectLanguage;
        private boolean showPhonetic;
        private boolean showDefinition;
        private boolean showExamples;
        private String translationEngine;
        private boolean autoPlayAudio;
        private String audioVoice;
        private double audioSpeed;

        public LanguageSettingsData() {
            // 设置默认值
            this.language = "zh-CN";
            this.defaultDictionary = "en-zh";
            this.autoDetectLanguage = true;
            this.showPhonetic = true;
            this.showDefinition = true;
            this.showExamples = true;
            this.translationEngine = "google";
            this.autoPlayAudio = false;
            this.audioVoice = "en-US-Wavenet-A";
            this.audioSpeed = 1.0;
        }

        // Getters and Setters
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getDefaultDictionary() { return defaultDictionary; }
        public void setDefaultDictionary(String defaultDictionary) { this.defaultDictionary = defaultDictionary; }

        public boolean isAutoDetectLanguage() { return autoDetectLanguage; }
        public void setAutoDetectLanguage(boolean autoDetectLanguage) { this.autoDetectLanguage = autoDetectLanguage; }

        public boolean isShowPhonetic() { return showPhonetic; }
        public void setShowPhonetic(boolean showPhonetic) { this.showPhonetic = showPhonetic; }

        public boolean isShowDefinition() { return showDefinition; }
        public void setShowDefinition(boolean showDefinition) { this.showDefinition = showDefinition; }

        public boolean isShowExamples() { return showExamples; }
        public void setShowExamples(boolean showExamples) { this.showExamples = showExamples; }

        public String getTranslationEngine() { return translationEngine; }
        public void setTranslationEngine(String translationEngine) { this.translationEngine = translationEngine; }

        public boolean isAutoPlayAudio() { return autoPlayAudio; }
        public void setAutoPlayAudio(boolean autoPlayAudio) { this.autoPlayAudio = autoPlayAudio; }

        public String getAudioVoice() { return audioVoice; }
        public void setAudioVoice(String audioVoice) { this.audioVoice = audioVoice; }

        public double getAudioSpeed() { return audioSpeed; }
        public void setAudioSpeed(double audioSpeed) { this.audioSpeed = audioSpeed; }
    }

    @GetMapping("")
    public ResponseEntity<LanguageSettingsResponse> getLanguageSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            printRequest("Authorization: " + authHeader);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new LanguageSettingsResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new LanguageSettingsResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 查询语言设置
            String settingsSql = "SELECT setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ? AND setting_type = 'language'";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 4. 构建响应数据
            LanguageSettingsData settingsData = new LanguageSettingsData();

            for (Map<String, Object> setting : settingsList) {
                String key = (String) setting.get("setting_key");
                String value = (String) setting.get("setting_value");

                switch (key) {
                    case "language":
                        settingsData.setLanguage(value);
                        break;
                    case "defaultDictionary":
                        settingsData.setDefaultDictionary(value);
                        break;
                    case "autoDetectLanguage":
                        settingsData.setAutoDetectLanguage("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "showPhonetic":
                        settingsData.setShowPhonetic("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "showDefinition":
                        settingsData.setShowDefinition("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "showExamples":
                        settingsData.setShowExamples("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "translationEngine":
                        settingsData.setTranslationEngine(value);
                        break;
                    case "autoPlayAudio":
                        settingsData.setAutoPlayAudio("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "audioVoice":
                        settingsData.setAudioVoice(value);
                        break;
                    case "audioSpeed":
                        try {
                            settingsData.setAudioSpeed(Double.parseDouble(value));
                        } catch (NumberFormatException e) {
                            settingsData.setAudioSpeed(1.0);
                        }
                        break;
                }
            }

            // 5. 准备响应
            LanguageSettingsResponse response = new LanguageSettingsResponse(true, "获取语言设置成功", settingsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取语言设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new LanguageSettingsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
