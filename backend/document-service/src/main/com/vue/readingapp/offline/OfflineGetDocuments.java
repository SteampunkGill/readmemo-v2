package com.vue.readingapp.offline;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@RestController
@RequestMapping("/api/v1/offline")
public class OfflineGetDocuments {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取离线文档列表请求 ===");
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

    // 响应DTO
    public static class OfflineDocumentsResponse {
        private boolean success;
        private String message;
        private OfflineDocumentsData data;

        public OfflineDocumentsResponse(boolean success, String message, OfflineDocumentsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public OfflineDocumentsData getData() { return data; }
        public void setData(OfflineDocumentsData data) { this.data = data; }
    }

    public static class OfflineDocumentsData {
        private List<OfflineDocument> documents;
        private Pagination pagination;

        public OfflineDocumentsData(List<OfflineDocument> documents, Pagination pagination) {
            this.documents = documents;
            this.pagination = pagination;
        }

        public List<OfflineDocument> getDocuments() { return documents; }
        public void setDocuments(List<OfflineDocument> documents) { this.documents = documents; }

        public Pagination getPagination() { return pagination; }
        public void setPagination(Pagination pagination) { this.pagination = pagination; }
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

    public static class Pagination {
        private int page;
        private int pageSize;
        private int total;
        private int totalPages;

        public Pagination(int page, int pageSize, int total, int totalPages) {
            this.page = page;
            this.pageSize = pageSize;
            this.total = total;
            this.totalPages = totalPages;
        }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }

    // 创建 offline_documents 表（如果不存在）
    private void createOfflineDocumentsTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS offline_documents (" +
                "offline_doc_id VARCHAR(50) PRIMARY KEY, " +
                "document_id VARCHAR(50) NOT NULL, " +
                "user_id INT NOT NULL, " +
                "title VARCHAR(500) NOT NULL, " +
                "content LONGTEXT, " +
                "description TEXT, " +
                "tags JSON, " +
                "file_size BIGINT DEFAULT 0, " +
                "file_type VARCHAR(50), " +
                "download_url VARCHAR(500), " +
                "is_synced BOOLEAN DEFAULT FALSE, " +
                "offline_version INT DEFAULT 1, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE, " +
                "FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE" +
                ")";

        try {
            jdbcTemplate.execute(sql);
            System.out.println("offline_documents 表已创建或已存在");
        } catch (Exception e) {
            System.err.println("创建 offline_documents 表失败: " + e.getMessage());
        }
    }

    // 验证用户token
    private Integer validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            // 从 user_sessions 表中验证token
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

    @GetMapping("/documents")
    public ResponseEntity<OfflineDocumentsResponse> getOfflineDocuments(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("page", page);
        requestInfo.put("pageSize", pageSize);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new OfflineDocumentsResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new OfflineDocumentsResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 创建表（如果不存在）
            createOfflineDocumentsTableIfNotExists();

            // 3. 计算分页参数
            int offset = (page - 1) * pageSize;

            // 4. 查询总记录数
            String countSql = "SELECT COUNT(*) as total FROM offline_documents WHERE user_id = ?";
            int total = jdbcTemplate.queryForObject(countSql, Integer.class, userId);

            // 5. 查询离线文档列表
            String querySql = "SELECT * FROM offline_documents WHERE user_id = ? ORDER BY updated_at DESC LIMIT ? OFFSET ?";
            List<Map<String, Object>> offlineDocs = jdbcTemplate.queryForList(querySql, userId, pageSize, offset);
            printQueryResult(offlineDocs);

            // 6. 转换为DTO对象
            List<OfflineDocument> documents = new ArrayList<>();

            for (Map<String, Object> row : offlineDocs) {
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

                documents.add(doc);
            }

            // 7. 计算分页信息
            int totalPages = (int) Math.ceil((double) total / pageSize);
            Pagination pagination = new Pagination(page, pageSize, total, totalPages);

            // 8. 准备响应数据
            OfflineDocumentsData data = new OfflineDocumentsData(documents, pagination);
            OfflineDocumentsResponse response = new OfflineDocumentsResponse(true, "获取离线文档列表成功", data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取离线文档列表过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new OfflineDocumentsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
