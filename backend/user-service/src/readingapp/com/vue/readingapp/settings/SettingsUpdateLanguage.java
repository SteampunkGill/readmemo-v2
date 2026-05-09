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
public class SettingsUpdateLanguage {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到更新语言设置请求 ===");
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

    public static class UpdateLanguageRequest {
        private String language;
        private String defaultDictionary;
        private Boolean autoDetectLanguage;
        private Boolean showPhonetic;
        private Boolean showDefinition;
        private Boolean showExamples;
        private String translationEngine;
        private Boolean autoPlayAudio;
        private String audioVoice;
        private Double audioSpeed;

        // Getters and Setters
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getDefaultDictionary() { return defaultDictionary; }
        public void setDefaultDictionary(String defaultDictionary) { this.defaultDictionary = defaultDictionary; }

        public Boolean getAutoDetectLanguage() { return autoDetectLanguage; }
        public void setAutoDetectLanguage(Boolean autoDetectLanguage) { this.autoDetectLanguage = autoDetectLanguage; }

        public Boolean getShowPhonetic() { return showPhonetic; }
        public void setShowPhonetic(Boolean showPhonetic) { this.showPhonetic = showPhonetic; }

        public Boolean getShowDefinition() { return showDefinition; }
        public void setShowDefinition(Boolean showDefinition) { this.showDefinition = showDefinition; }

        public Boolean getShowExamples() { return showExamples; }
        public void setShowExamples(Boolean showExamples) { this.showExamples = showExamples; }

        public String getTranslationEngine() { return translationEngine; }
        public void setTranslationEngine(String translationEngine) { this.translationEngine = translationEngine; }

        public Boolean getAutoPlayAudio() { return autoPlayAudio; }
        public void setAutoPlayAudio(Boolean autoPlayAudio) { this.autoPlayAudio = autoPlayAudio; }

        public String getAudioVoice() { return audioVoice; }
        public void setAudioVoice(String audioVoice) { this.audioVoice = audioVoice; }

        public Double getAudioSpeed() { return audioSpeed; }
        public void setAudioSpeed(Double audioSpeed) { this.audioSpeed = audioSpeed; }
    }

    public static class UpdateLanguageResponse {
        private boolean success;
        private String message;
        private SettingsGetLanguage.LanguageSettingsData data;

        public UpdateLanguageResponse(boolean success, String message, SettingsGetLanguage.LanguageSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SettingsGetLanguage.LanguageSettingsData getData() { return data; }
        public void setData(SettingsGetLanguage.LanguageSettingsData data) { this.data = data; }
    }

    @PutMapping("")
    public ResponseEntity<UpdateLanguageResponse> updateLanguageSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateLanguageRequest request) {

        try {
            printRequest(request);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateLanguageResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateLanguageResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 验证语言设置数据
            if (request.getAudioSpeed() != null && (request.getAudioSpeed() < 0.5 || request.getAudioSpeed() > 2.0)) {
                return ResponseEntity.badRequest().body(
                        new UpdateLanguageResponse(false, "音频速度必须在0.5-2.0之间", null)
                );
            }

            // 验证翻译引擎
            if (request.getTranslationEngine() != null && !isValidTranslationEngine(request.getTranslationEngine())) {
                return ResponseEntity.badRequest().body(
                        new UpdateLanguageResponse(false, "无效的翻译引擎", null)
                );
            }

            // 4. 更新语言设置
            LocalDateTime now = LocalDateTime.now();

            updateLanguageSetting(userId, "language", request.getLanguage(), now);
            updateLanguageSetting(userId, "defaultDictionary", request.getDefaultDictionary(), now);
            updateLanguageSetting(userId, "autoDetectLanguage", request.getAutoDetectLanguage() != null ? request.getAutoDetectLanguage().toString() : null, now);
            updateLanguageSetting(userId, "showPhonetic", request.getShowPhonetic() != null ? request.getShowPhonetic().toString() : null, now);
            updateLanguageSetting(userId, "showDefinition", request.getShowDefinition() != null ? request.getShowDefinition().toString() : null, now);
            updateLanguageSetting(userId, "showExamples", request.getShowExamples() != null ? request.getShowExamples().toString() : null, now);
            updateLanguageSetting(userId, "translationEngine", request.getTranslationEngine(), now);
            updateLanguageSetting(userId, "autoPlayAudio", request.getAutoPlayAudio() != null ? request.getAutoPlayAudio().toString() : null, now);
            updateLanguageSetting(userId, "audioVoice", request.getAudioVoice(), now);
            updateLanguageSetting(userId, "audioSpeed", request.getAudioSpeed() != null ? request.getAudioSpeed().toString() : null, now);

            // 5. 查询更新后的设置
            String settingsSql = "SELECT setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ? AND setting_type = 'language'";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 6. 构建响应数据
            SettingsGetLanguage.LanguageSettingsData settingsData = new SettingsGetLanguage.LanguageSettingsData();

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

            // 7. 准备响应
            UpdateLanguageResponse response = new UpdateLanguageResponse(true, "语言设置更新成功", settingsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新语言设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateLanguageResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private void updateLanguageSetting(Integer userId, String key, String value, LocalDateTime now) {
        if (value != null) {
            // 检查设置是否存在
            String checkSql = "SELECT setting_id FROM user_settings WHERE user_id = ? AND setting_type = 'language' AND setting_key = ?";
            List<Map<String, Object>> existing = jdbcTemplate.queryForList(checkSql, userId, key);

            if (existing.isEmpty()) {
                // 插入新设置
                String insertSql = "INSERT INTO user_settings (user_id, setting_type, setting_key, setting_value, created_at, updated_at) VALUES (?, 'language', ?, ?, ?, ?)";
                jdbcTemplate.update(insertSql, userId, key, value, now, now);
            } else {
                // 更新现有设置
                String updateSql = "UPDATE user_settings SET setting_value = ?, updated_at = ? WHERE user_id = ? AND setting_type = 'language' AND setting_key = ?";
                jdbcTemplate.update(updateSql, value, now, userId, key);
            }
        }
    }

    private boolean isValidTranslationEngine(String engine) {
        return engine.equals("google") || engine.equals("bing") ||
                engine.equals("youdao") || engine.equals("baidu");
    }
}
