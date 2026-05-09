
package com.vue.readingapp.offline;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/offline")
public class OfflineSyncVocabulary {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到同步词汇数据请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("==========================");
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
    public static class SyncVocabularyRequest {
        private VocabularyData vocabulary;

        public VocabularyData getVocabulary() { return vocabulary; }
        public void setVocabulary(VocabularyData vocabulary) { this.vocabulary = vocabulary; }
    }

    public static class VocabularyData {
        private String id;
        private String word;
        private String translation;
        private String updatedAt;
        private int offlineVersion;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getTranslation() { return translation; }
        public void setTranslation(String translation) { this.translation = translation; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public int getOfflineVersion() { return offlineVersion; }
        public void setOfflineVersion(int offlineVersion) { this.offlineVersion = offlineVersion; }
    }

    // 响应DTO
    public static class SyncVocabularyResponse {
        private boolean success;
        private String message;
        private SyncVocabularyData data;

        public SyncVocabularyResponse(boolean success, String message, SyncVocabularyData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SyncVocabularyData getData() { return data; }
        public void setData(SyncVocabularyData data) { this.data = data; }
    }

    public static class SyncVocabularyData {
        private String vocabularyId;
        private boolean synced;
        private String syncedAt;
        private int offlineVersion;

        public SyncVocabularyData(String vocabularyId, boolean synced, String syncedAt, int offlineVersion) {
            this.vocabularyId = vocabularyId;
            this.synced = synced;
            this.syncedAt = syncedAt;
            this.offlineVersion = offlineVersion;
        }

        // Getters and Setters
        public String getVocabularyId() { return vocabularyId; }
        public void setVocabularyId(String vocabularyId) { this.vocabularyId = vocabularyId; }

        public boolean isSynced() { return synced; }
        public void setSynced(boolean synced) { this.synced = synced; }

        public String getSyncedAt() { return syncedAt; }
        public void setSyncedAt(String syncedAt) { this.syncedAt = syncedAt; }

        public int getOfflineVersion() { return offlineVersion; }
        public void setOfflineVersion(int offlineVersion) { this.offlineVersion = offlineVersion; }
    }

    // 创建 offline_vocabulary 表（如果不存在）
    private void createOfflineVocabularyTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS offline_vocabulary (" +
                "offline_voc_id VARCHAR(50) PRIMARY KEY, " +
                "vocabulary_id VARCHAR(50) NOT NULL, " +
                "user_id INT NOT NULL, " +
                "word VARCHAR(200) NOT NULL, " +
                "translation TEXT, " +
                "is_synced BOOLEAN DEFAULT FALSE, " +
                "offline_version INT DEFAULT 1, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                ")";

        try {
            jdbcTemplate.execute(sql);
            System.out.println("offline_vocabulary 表已创建或已存在");
        } catch (Exception e) {
            System.err.println("创建 offline_vocabulary 表失败: " + e.getMessage());
        }
    }

    // 验证用户token
    private Integer validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            String sql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sql, token);

            if (sessions.isEmpty()) {
                return null;
            }

            return (Integer) sessions.get(0).get("user_id");
        } catch (Exception e) {
            System.err.println("验证token失败: " + e.getMessage());
            return null;
        }
    }

    @PostMapping("/sync/vocabulary")
    public ResponseEntity<SyncVocabularyResponse> syncVocabulary(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SyncVocabularyRequest request) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("requestBody", request);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SyncVocabularyResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SyncVocabularyResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 验证请求数据
            if (request == null || request.getVocabulary() == null) {
                return ResponseEntity.badRequest().body(
                        new SyncVocabularyResponse(false, "词汇数据不能为空", null)
                );
            }

            VocabularyData vocabularyData = request.getVocabulary();
            if (vocabularyData.getId() == null || vocabularyData.getId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SyncVocabularyResponse(false, "词汇ID不能为空", null)
                );
            }

            // 3. 创建相关表
            createOfflineVocabularyTableIfNotExists();

            // 4. 检查词汇是否存在（假设有vocabulary表）
            String checkVocabSql = "SELECT vocabulary_id FROM vocabulary WHERE vocabulary_id = ?";
            List<Map<String, Object>> vocabularies = jdbcTemplate.queryForList(checkVocabSql, vocabularyData.getId());

            if (vocabularies.isEmpty()) {
                // 词汇不存在，创建新词汇
                String insertVocabSql = "INSERT INTO vocabulary (vocabulary_id, user_id, word, translation, created_at) " +
                        "VALUES (?, ?, ?, ?, NOW())";
                jdbcTemplate.update(insertVocabSql, vocabularyData.getId(), userId,
                        vocabularyData.getWord(), vocabularyData.getTranslation());
            } else {
                // 词汇存在，更新词汇
                String updateVocabSql = "UPDATE vocabulary SET word = ?, translation = ?, updated_at = NOW() " +
                        "WHERE vocabulary_id = ? AND user_id = ?";
                jdbcTemplate.update(updateVocabSql, vocabularyData.getWord(),
                        vocabularyData.getTranslation(), vocabularyData.getId(), userId);
            }

            // 5. 检查是否已有离线词汇记录
            String checkOfflineSql = "SELECT offline_voc_id, offline_version FROM offline_vocabulary " +
                    "WHERE vocabulary_id = ? AND user_id = ?";
            List<Map<String, Object>> offlineVocabs = jdbcTemplate.queryForList(checkOfflineSql,
                    vocabularyData.getId(), userId);

            if (offlineVocabs.isEmpty()) {
                // 创建离线词汇记录
                String offlineVocId = "offline_voc_" + UUID.randomUUID().toString();
                String insertOfflineSql = "INSERT INTO offline_vocabulary (offline_voc_id, vocabulary_id, user_id, " +
                        "word, translation, is_synced, offline_version, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, TRUE, ?, NOW())";
                jdbcTemplate.update(insertOfflineSql, offlineVocId, vocabularyData.getId(), userId,
                        vocabularyData.getWord(), vocabularyData.getTranslation(),
                        vocabularyData.getOfflineVersion());
            } else {
                // 更新离线词汇记录
                Map<String, Object> offlineVoc = offlineVocabs.get(0);
                int currentVersion = offlineVoc.get("offline_version") != null ?
                        ((Number) offlineVoc.get("offline_version")).intValue() : 0;

                if (vocabularyData.getOfflineVersion() > currentVersion) {
                    // 离线版本较新，更新服务器数据
                    String updateOfflineSql = "UPDATE offline_vocabulary SET word = ?, translation = ?, " +
                            "is_synced = TRUE, offline_version = ?, updated_at = NOW() " +
                            "WHERE vocabulary_id = ? AND user_id = ?";
                    jdbcTemplate.update(updateOfflineSql, vocabularyData.getWord(),
                            vocabularyData.getTranslation(), vocabularyData.getOfflineVersion(),
                            vocabularyData.getId(), userId);
                } else {
                    // 服务器版本较新或相同，标记为已同步
                    String updateSyncSql = "UPDATE offline_vocabulary SET is_synced = TRUE, updated_at = NOW() " +
                            "WHERE vocabulary_id = ? AND user_id = ?";
                    jdbcTemplate.update(updateSyncSql, vocabularyData.getId(), userId);
                }
            }

            // 6. 准备响应数据
            SyncVocabularyData data = new SyncVocabularyData(
                    vocabularyData.getId(),
                    true,
                    "刚刚同步",
                    vocabularyData.getOfflineVersion()
            );

            SyncVocabularyResponse response = new SyncVocabularyResponse(true, "词汇同步成功", data);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("同步词汇数据过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SyncVocabularyResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}