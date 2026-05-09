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
public class OfflineSyncDocument {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到同步文档数据请求 ===");
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
    public static class SyncDocumentRequest {
        private DocumentData document;

        public DocumentData getDocument() { return document; }
        public void setDocument(DocumentData document) { this.document = document; }
    }

    public static class DocumentData {
        private String id;
        private String title;
        private String content;
        private String updatedAt;
        private int offlineVersion;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public int getOfflineVersion() { return offlineVersion; }
        public void setOfflineVersion(int offlineVersion) { this.offlineVersion = offlineVersion; }
    }

    // 响应DTO
    public static class SyncDocumentResponse {
        private boolean success;
        private String message;
        private SyncDocumentData data;

        public SyncDocumentResponse(boolean success, String message, SyncDocumentData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SyncDocumentData getData() { return data; }
        public void setData(SyncDocumentData data) { this.data = data; }
    }

    public static class SyncDocumentData {
        private String documentId;
        private boolean synced;
        private String syncedAt;
        private int offlineVersion;

        public SyncDocumentData(String documentId, boolean synced, String syncedAt, int offlineVersion) {
            this.documentId = documentId;
            this.synced = synced;
            this.syncedAt = syncedAt;
            this.offlineVersion = offlineVersion;
        }

        // Getters and Setters
        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public boolean isSynced() { return synced; }
        public void setSynced(boolean synced) { this.synced = synced; }

        public String getSyncedAt() { return syncedAt; }
        public void setSyncedAt(String syncedAt) { this.syncedAt = syncedAt; }

        public int getOfflineVersion() { return offlineVersion; }
        public void setOfflineVersion(int offlineVersion) { this.offlineVersion = offlineVersion; }
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

    @PostMapping("/sync/document")
    public ResponseEntity<SyncDocumentResponse> syncDocument(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SyncDocumentRequest request) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("requestBody", request);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SyncDocumentResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SyncDocumentResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 验证请求数据
            if (request == null || request.getDocument() == null) {
                return ResponseEntity.badRequest().body(
                        new SyncDocumentResponse(false, "文档数据不能为空", null)
                );
            }

            DocumentData documentData = request.getDocument();
            if (documentData.getId() == null || documentData.getId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SyncDocumentResponse(false, "文档ID不能为空", null)
                );
            }

            // 3. 检查文档是否存在
            String checkDocSql = "SELECT document_id FROM documents WHERE document_id = ?";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(checkDocSql, documentData.getId());

            if (documents.isEmpty()) {
                // 文档不存在，创建新文档
                String insertDocSql = "INSERT INTO documents (document_id, user_id, title, content, status, created_at) " +
                        "VALUES (?, ?, ?, ?, 'active', NOW())";
                jdbcTemplate.update(insertDocSql, documentData.getId(), userId,
                        documentData.getTitle(), documentData.getContent());
            } else {
                // 文档存在，更新文档
                String updateDocSql = "UPDATE documents SET title = ?, content = ?, updated_at = NOW() " +
                        "WHERE document_id = ? AND user_id = ?";
                jdbcTemplate.update(updateDocSql, documentData.getTitle(),
                        documentData.getContent(), documentData.getId(), userId);
            }

            // 4. 检查是否已有离线文档记录
            String checkOfflineSql = "SELECT offline_doc_id, offline_version FROM offline_documents " +
                    "WHERE document_id = ? AND user_id = ?";
            List<Map<String, Object>> offlineDocs = jdbcTemplate.queryForList(checkOfflineSql,
                    documentData.getId(), userId);

            if (offlineDocs.isEmpty()) {
                // 创建离线文档记录
                String offlineDocId = "offline_doc_" + UUID.randomUUID().toString();
                String insertOfflineSql = "INSERT INTO offline_documents (offline_doc_id, document_id, user_id, " +
                        "title, content, is_synced, offline_version, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, TRUE, ?, NOW())";
                jdbcTemplate.update(insertOfflineSql, offlineDocId, documentData.getId(), userId,
                        documentData.getTitle(), documentData.getContent(),
                        documentData.getOfflineVersion());
            } else {
                // 更新离线文档记录
                Map<String, Object> offlineDoc = offlineDocs.get(0);
                int currentVersion = offlineDoc.get("offline_version") != null ?
                        ((Number) offlineDoc.get("offline_version")).intValue() : 0;

                if (documentData.getOfflineVersion() > currentVersion) {
                    // 离线版本较新，更新服务器数据
                    String updateOfflineSql = "UPDATE offline_documents SET title = ?, content = ?, " +
                            "is_synced = TRUE, offline_version = ?, updated_at = NOW() " +
                            "WHERE document_id = ? AND user_id = ?";
                    jdbcTemplate.update(updateOfflineSql, documentData.getTitle(),
                            documentData.getContent(), documentData.getOfflineVersion(),
                            documentData.getId(), userId);
                } else {
                    // 服务器版本较新或相同，标记为已同步
                    String updateSyncSql = "UPDATE offline_documents SET is_synced = TRUE, updated_at = NOW() " +
                            "WHERE document_id = ? AND user_id = ?";
                    jdbcTemplate.update(updateSyncSql, documentData.getId(), userId);
                }
            }

            // 5. 准备响应数据
            SyncDocumentData data = new SyncDocumentData(
                    documentData.getId(),
                    true,
                    "刚刚同步",
                    documentData.getOfflineVersion()
            );

            SyncDocumentResponse response = new SyncDocumentResponse(true, "文档同步成功", data);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("同步文档数据过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SyncDocumentResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
