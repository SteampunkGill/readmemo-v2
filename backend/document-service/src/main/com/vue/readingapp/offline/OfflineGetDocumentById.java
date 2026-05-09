
package com.vue.readingapp.offline;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@RestController
@RequestMapping("/api/v1/offline")
public class OfflineGetDocumentById {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取单个离线文档详情请求 ===");
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
    public static class OfflineDocumentResponse {
        private boolean success;
        private String message;
        private OfflineDocumentData data;

        public OfflineDocumentResponse(boolean success, String message, OfflineDocumentData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public OfflineDocumentData getData() { return data; }
        public void setData(OfflineDocumentData data) { this.data = data; }
    }

    public static class OfflineDocumentData {
        private OfflineDocument document;

        public OfflineDocumentData(OfflineDocument document) {
            this.document = document;
        }

        public OfflineDocument getDocument() { return document; }
        public void setDocument(OfflineDocument document) { this.document = document; }
    }

    public static class OfflineDocument {
        private String id;
        private String documentId;
        private String title;
        private String content;
        private String description;
        private List<String> tags;
        private long fileSize;
        private String fileType;
        private String downloadUrl;
        private boolean isSynced;
        private int offlineVersion;
        private String updatedAt;
        private String createdAt;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }

        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }

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

    @GetMapping("/documents/{offlineDocId}")
    public ResponseEntity<OfflineDocumentResponse> getOfflineDocumentById(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String offlineDocId) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("offlineDocId", offlineDocId);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new OfflineDocumentResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new OfflineDocumentResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 查询离线文档详情
            String querySql = "SELECT * FROM offline_documents WHERE offline_doc_id = ? AND user_id = ?";
            List<Map<String, Object>> offlineDocs = jdbcTemplate.queryForList(querySql, offlineDocId, userId);
            printQueryResult(offlineDocs);

            if (offlineDocs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new OfflineDocumentResponse(false, "离线文档不存在", null)
                );
            }

            Map<String, Object> row = offlineDocs.get(0);

            // 3. 转换为DTO对象
            OfflineDocument doc = new OfflineDocument();
            doc.setId((String) row.get("offline_doc_id"));
            doc.setDocumentId((String) row.get("document_id"));
            doc.setTitle((String) row.get("title"));
            doc.setContent((String) row.get("content"));
            doc.setDescription((String) row.get("description"));

            // 解析tags JSON
            String tagsJson = (String) row.get("tags");
            if (tagsJson != null && !tagsJson.isEmpty()) {
                try {
                    List<String> tags = objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
                    doc.setTags(tags);
                } catch (Exception e) {
                    doc.setTags(new ArrayList<>());
                }
            } else {
                doc.setTags(new ArrayList<>());
            }

            doc.setFileSize(row.get("file_size") != null ? ((Number) row.get("file_size")).longValue() : 0);
            doc.setFileType((String) row.get("file_type"));
            doc.setDownloadUrl((String) row.get("download_url"));
            doc.setSynced(row.get("is_synced") != null && (Boolean) row.get("is_synced"));
            doc.setOfflineVersion(row.get("offline_version") != null ? ((Number) row.get("offline_version")).intValue() : 1);
            doc.setUpdatedAt(row.get("updated_at") != null ? row.get("updated_at").toString() : null);
            doc.setCreatedAt(row.get("created_at") != null ? row.get("created_at").toString() : null);

            // 4. 准备响应数据
            OfflineDocumentData data = new OfflineDocumentData(doc);
            OfflineDocumentResponse response = new OfflineDocumentResponse(true, "获取离线文档详情成功", data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取离线文档详情过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new OfflineDocumentResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}