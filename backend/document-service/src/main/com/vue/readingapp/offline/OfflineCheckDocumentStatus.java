
package com.vue.readingapp.offline;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/offline")
public class OfflineCheckDocumentStatus {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到检查文档是否已离线请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("================================");
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

    // 响应DTO
    public static class DocumentStatusResponse {
        private boolean success;
        private String message;
        private DocumentStatusData data;

        public DocumentStatusResponse(boolean success, String message, DocumentStatusData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public DocumentStatusData getData() { return data; }
        public void setData(DocumentStatusData data) { this.data = data; }
    }

    public static class DocumentStatusData {
        private String documentId;
        private boolean isOffline;
        private String offlineDocId;
        private String downloadUrl;
        private boolean isSynced;
        private int offlineVersion;
        private String updatedAt;
        private String createdAt;

        public DocumentStatusData(String documentId, boolean isOffline, String offlineDocId,
                                  String downloadUrl, boolean isSynced, int offlineVersion,
                                  String updatedAt, String createdAt) {
            this.documentId = documentId;
            this.isOffline = isOffline;
            this.offlineDocId = offlineDocId;
            this.downloadUrl = downloadUrl;
            this.isSynced = isSynced;
            this.offlineVersion = offlineVersion;
            this.updatedAt = updatedAt;
            this.createdAt = createdAt;
        }

        // Getters and Setters
        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public boolean isOffline() { return isOffline; }
        public void setOffline(boolean offline) { isOffline = offline; }

        public String getOfflineDocId() { return offlineDocId; }
        public void setOfflineDocId(String offlineDocId) { this.offlineDocId = offlineDocId; }

        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

        public boolean isSynced() { return isSynced; }
        public void setSynced(boolean synced) { isSynced = synced; }

        public int getOfflineVersion() { return offlineVersion; }
        public void setOfflineVersion(int offlineVersion) { this.offlineVersion = offlineVersion; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
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

    @GetMapping("/documents/{documentId}/status")
    public ResponseEntity<DocumentStatusResponse> checkDocumentOfflineStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String documentId) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("documentId", documentId);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DocumentStatusResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DocumentStatusResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 检查文档是否存在
            String checkDocSql = "SELECT document_id, title FROM documents WHERE document_id = ?";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(checkDocSql, documentId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DocumentStatusResponse(false, "文档不存在", null)
                );
            }

            Map<String, Object> document = documents.get(0);
            String title = (String) document.get("title");

            // 3. 检查是否已离线下载
            String checkOfflineSql = "SELECT * FROM offline_documents WHERE document_id = ? AND user_id = ?";
            List<Map<String, Object>> offlineDocs = jdbcTemplate.queryForList(checkOfflineSql, documentId, userId);
            printQueryResult(offlineDocs);

            if (offlineDocs.isEmpty()) {
                // 文档未离线下载
                DocumentStatusData data = new DocumentStatusData(
                        documentId,
                        false,
                        null,
                        null,
                        false,
                        0,
                        null,
                        null
                );

                DocumentStatusResponse response = new DocumentStatusResponse(true, "文档未离线下载", data);
                printResponse(response);

                return ResponseEntity.ok(response);
            } else {
                // 文档已离线下载
                Map<String, Object> offlineDoc = offlineDocs.get(0);

                DocumentStatusData data = new DocumentStatusData(
                        documentId,
                        true,
                        (String) offlineDoc.get("offline_doc_id"),
                        (String) offlineDoc.get("download_url"),
                        offlineDoc.get("is_synced") != null && (Boolean) offlineDoc.get("is_synced"),
                        offlineDoc.get("offline_version") != null ? ((Number) offlineDoc.get("offline_version")).intValue() : 1,
                        offlineDoc.get("updated_at") != null ? offlineDoc.get("updated_at").toString() : null,
                        offlineDoc.get("created_at") != null ? offlineDoc.get("created_at").toString() : null
                );

                DocumentStatusResponse response = new DocumentStatusResponse(true, "文档已离线下载", data);
                printResponse(response);

                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            System.err.println("检查文档是否已离线过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DocumentStatusResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}