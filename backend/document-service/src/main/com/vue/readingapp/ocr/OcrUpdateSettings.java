package com.vue.readingapp.ocr;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/documents")
public class OcrUpdateSettings {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到更新OCR设置请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=======================");
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

    // 请求DTO
    public static class UpdateOcrSettingsRequest {
        private String documentId;
        private OcrSettings settings;

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public OcrSettings getSettings() { return settings; }
        public void setSettings(OcrSettings settings) { this.settings = settings; }
    }

    public static class OcrSettings {
        private String defaultLanguage;
        private Integer defaultConfidence;
        private Boolean autoPreprocess;
        private String supportedLanguages; // JSON数组字符串
        private String engine;

        public String getDefaultLanguage() { return defaultLanguage; }
        public void setDefaultLanguage(String defaultLanguage) { this.defaultLanguage = defaultLanguage; }

        public Integer getDefaultConfidence() { return defaultConfidence; }
        public void setDefaultConfidence(Integer defaultConfidence) { this.defaultConfidence = defaultConfidence; }

        public Boolean getAutoPreprocess() { return autoPreprocess; }
        public void setAutoPreprocess(Boolean autoPreprocess) { this.autoPreprocess = autoPreprocess; }

        public String getSupportedLanguages() { return supportedLanguages; }
        public void setSupportedLanguages(String supportedLanguages) { this.supportedLanguages = supportedLanguages; }

        public String getEngine() { return engine; }
        public void setEngine(String engine) { this.engine = engine; }
    }

    // 响应DTO
    public static class UpdateOcrSettingsResponse {
        private boolean success;
        private String message;
        private UpdatedSettingsData data;

        public UpdateOcrSettingsResponse(boolean success, String message, UpdatedSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UpdatedSettingsData getData() { return data; }
        public void setData(UpdatedSettingsData data) { this.data = data; }
    }

    public static class UpdatedSettingsData {
        private String documentId;
        private OcrSettings settings;
        private String updatedAt;

        public UpdatedSettingsData(String documentId, OcrSettings settings, String updatedAt) {
            this.documentId = documentId;
            this.settings = settings;
            this.updatedAt = updatedAt;
        }

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public OcrSettings getSettings() { return settings; }
        public void setSettings(OcrSettings settings) { this.settings = settings; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    @PutMapping("/{documentId}/ocr/options")
    public ResponseEntity<UpdateOcrSettingsResponse> updateOcrSettings(
            @PathVariable String documentId,
            @RequestBody UpdateOcrSettingsRequest request) {

        // 设置文档ID
        request.setDocumentId(documentId);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证请求数据
            if (documentId == null || documentId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UpdateOcrSettingsResponse(false, "文档ID不能为空", null)
                );
            }

            if (request.getSettings() == null) {
                return ResponseEntity.badRequest().body(
                        new UpdateOcrSettingsResponse(false, "OCR设置不能为空", null)
                );
            }

            // 2. 检查文档是否存在
            String checkDocumentSql = "SELECT COUNT(*) FROM documents WHERE document_id = ?";
            Integer documentCount = jdbcTemplate.queryForObject(checkDocumentSql, Integer.class, documentId);

            if (documentCount == null || documentCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdateOcrSettingsResponse(false, "文档不存在", null)
                );
            }

            // 3. 检查OCR设置表是否存在
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'document_ocr_settings'";
            Integer tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class);

            if (tableCount == null || tableCount == 0) {
                // 创建OCR设置表
                String createTableSql = "CREATE TABLE document_ocr_settings (" +
                        "setting_id VARCHAR(50) PRIMARY KEY," +
                        "document_id VARCHAR(50) NOT NULL," +
                        "default_language VARCHAR(20)," +
                        "default_confidence INT," +
                        "auto_preprocess BOOLEAN," +
                        "supported_languages TEXT," +
                        "engine VARCHAR(50)," +
                        "created_at TIMESTAMP," +
                        "updated_at TIMESTAMP," +
                        "FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE" +
                        ")";
                jdbcTemplate.execute(createTableSql);
                System.out.println("已创建 document_ocr_settings 表");
            }

            // 4. 检查是否已存在设置
            String checkSettingsSql = "SELECT setting_id FROM document_ocr_settings WHERE document_id = ?";
            List<Map<String, Object>> existingSettings = jdbcTemplate.queryForList(checkSettingsSql, documentId);

            LocalDateTime now = LocalDateTime.now();
            String settingId;

            if (existingSettings.isEmpty()) {
                // 创建新的设置
                settingId = "setting_" + UUID.randomUUID().toString().substring(0, 8);
                String insertSettingsSql = "INSERT INTO document_ocr_settings (setting_id, document_id, default_language, default_confidence, auto_preprocess, supported_languages, engine, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                jdbcTemplate.update(insertSettingsSql,
                        settingId,
                        documentId,
                        request.getSettings().getDefaultLanguage(),
                        request.getSettings().getDefaultConfidence(),
                        request.getSettings().getAutoPreprocess(),
                        request.getSettings().getSupportedLanguages(),
                        request.getSettings().getEngine(),
                        now,
                        now
                );

                System.out.println("创建了新的OCR设置: " + settingId);
            } else {
                // 更新现有的设置
                settingId = (String) existingSettings.get(0).get("setting_id");
                String updateSettingsSql = "UPDATE document_ocr_settings SET default_language = ?, default_confidence = ?, auto_preprocess = ?, supported_languages = ?, engine = ?, updated_at = ? WHERE setting_id = ?";

                jdbcTemplate.update(updateSettingsSql,
                        request.getSettings().getDefaultLanguage(),
                        request.getSettings().getDefaultConfidence(),
                        request.getSettings().getAutoPreprocess(),
                        request.getSettings().getSupportedLanguages(),
                        request.getSettings().getEngine(),
                        now,
                        settingId
                );

                System.out.println("更新了OCR设置: " + settingId);
            }

            // 5. 查询更新后的设置
            String querySettingsSql = "SELECT document_id, default_language, default_confidence, auto_preprocess, supported_languages, engine, updated_at FROM document_ocr_settings WHERE setting_id = ?";
            List<Map<String, Object>> updatedSettings = jdbcTemplate.queryForList(querySettingsSql, settingId);
            printQueryResult(updatedSettings);

            if (updatedSettings.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new UpdateOcrSettingsResponse(false, "更新OCR设置失败", null)
                );
            }

            Map<String, Object> settings = updatedSettings.get(0);

            // 6. 准备响应数据
            OcrSettings ocrSettings = new OcrSettings();
            ocrSettings.setDefaultLanguage((String) settings.get("default_language"));
            ocrSettings.setDefaultConfidence(settings.get("default_confidence") != null ? ((Number) settings.get("default_confidence")).intValue() : 70);
            ocrSettings.setAutoPreprocess(settings.get("auto_preprocess") != null ? (Boolean) settings.get("auto_preprocess") : true);
            ocrSettings.setSupportedLanguages((String) settings.get("supported_languages"));
            ocrSettings.setEngine((String) settings.get("engine"));

            UpdatedSettingsData responseData = new UpdatedSettingsData(
                    (String) settings.get("document_id"),
                    ocrSettings,
                    settings.get("updated_at") != null ? ((LocalDateTime) settings.get("updated_at")).format(formatter) : ""
            );

            UpdateOcrSettingsResponse response = new UpdateOcrSettingsResponse(true, "OCR设置更新成功", responseData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新OCR设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateOcrSettingsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}