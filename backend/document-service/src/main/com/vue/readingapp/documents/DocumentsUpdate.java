package com.vue.readingapp.documents;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.time.LocalDateTime;
import java.sql.Timestamp;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentsUpdate {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Integer documentId, UpdateRequest request) {
        System.out.println("=== 收到更新文档请求 ===");
        System.out.println("文档ID: " + documentId);
        System.out.println("更新数据: " + request);
        System.out.println("=====================");
    }

    // 打印查询结果
    private void printQueryResult(Map<String, Object> document) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("文档信息: " + document);
        System.out.println("===================");
    }

    // 打印返回数据
    private void printResponse(UpdateResponse response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 请求DTO
    public static class UpdateRequest {
        private String title;
        private String description;
        private List<String> tags;
        private String language;
        private Boolean isPublic;
        private Boolean isFavorite;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public Boolean getIsPublic() { return isPublic; }
        public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

        public Boolean getIsFavorite() { return isFavorite; }
        public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }
    }

    // 响应DTO
    public static class UpdateResponse {
        private boolean success;
        private String message;
        private UpdateData data;

        public UpdateResponse(boolean success, String message, UpdateData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UpdateData getData() { return data; }
        public void setData(UpdateData data) { this.data = data; }
    }

    public static class UpdateData {
        private UpdateDocumentDTO document;

        public UpdateData(UpdateDocumentDTO document) {
            this.document = document;
        }

        public UpdateDocumentDTO getDocument() { return document; }
        public void setDocument(UpdateDocumentDTO document) { this.document = document; }
    }

    public static class UpdateDocumentDTO {
        private Integer id;
        private String title;
        private String description;
        private String language;
        private List<String> tags;
        private Boolean isPublic;
        private Boolean isFavorite;
        private LocalDateTime updatedAt;

        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public Boolean getIsPublic() { return isPublic; }
        public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

        public Boolean getIsFavorite() { return isFavorite; }
        public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    @PutMapping("/{documentId}")
    public ResponseEntity<UpdateResponse> updateDocument(
            @PathVariable("documentId") Integer documentId,
            @RequestBody UpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest(documentId, request);

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 2. 验证请求数据
            if (request == null || (request.getTitle() == null && request.getDescription() == null &&
                    request.getTags() == null && request.getLanguage() == null &&
                    request.getIsPublic() == null && request.getIsFavorite() == null)) {
                return ResponseEntity.badRequest().body(
                        new UpdateResponse(false, "更新数据不能为空", null)
                );
            }

            // 3. 检查文档是否存在且属于当前用户
            String checkSql = "SELECT document_id FROM documents WHERE document_id = ? AND user_id = ? AND deleted_at IS NULL";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(checkSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdateResponse(false, "文档不存在或没有权限", null)
                );
            }

            // 4. 构建更新SQL
            StringBuilder updateSql = new StringBuilder("UPDATE documents SET ");
            List<Object> params = new ArrayList<>();

            if (request.getTitle() != null) {
                updateSql.append("title = ?, ");
                params.add(request.getTitle());
            }

            if (request.getDescription() != null) {
                updateSql.append("description = ?, ");
                params.add(request.getDescription());
            }

            if (request.getLanguage() != null) {
                updateSql.append("language = ?, ");
                params.add(request.getLanguage());
            }

            if (request.getIsPublic() != null) {
                updateSql.append("is_public = ?, ");
                params.add(request.getIsPublic());
            }

            if (request.getIsFavorite() != null) {
                updateSql.append("is_favorite = ?, ");
                params.add(request.getIsFavorite());
            }

            // 添加更新时间
            LocalDateTime now = LocalDateTime.now();
            Timestamp timestamp = Timestamp.valueOf(now);
            updateSql.append("updated_at = ? ");
            params.add(timestamp);

            updateSql.append("WHERE document_id = ? AND user_id = ?");
            params.add(documentId);
            params.add(userId);

            // 5. 执行更新
            int rowsUpdated = jdbcTemplate.update(updateSql.toString(), params.toArray());

            if (rowsUpdated == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdateResponse(false, "文档更新失败", null)
                );
            }

            // 6. 处理标签更新
            if (request.getTags() != null) {
                // 删除旧的标签关系
                String deleteRelationsSql = "DELETE dtr FROM document_tag_relations dtr " +
                        "JOIN document_tags dt ON dtr.tag_id = dt.tag_id " +
                        "WHERE dtr.document_id = ?";
                jdbcTemplate.update(deleteRelationsSql, documentId);

                // 添加新的标签关系
                for (String tagName : request.getTags()) {
                    if (tagName == null || tagName.trim().isEmpty()) continue;

                    // 检查标签是否已存在
                    String checkTagSql = "SELECT tag_id FROM document_tags WHERE tag_name = ?";
                    List<Map<String, Object>> existingTags = jdbcTemplate.queryForList(checkTagSql, tagName);

                    Integer tagId;
                    if (existingTags.isEmpty()) {
                        // 创建新标签
                        String insertTagSql = "INSERT INTO document_tags (tag_name, created_at) VALUES (?, ?)";
                        jdbcTemplate.update(insertTagSql, tagName, timestamp);

                        String lastTagIdSql = "SELECT LAST_INSERT_ID() as id";
                        tagId = jdbcTemplate.queryForObject(lastTagIdSql, Integer.class);
                    } else {
                        tagId = (Integer) existingTags.get(0).get("tag_id");
                    }

                    // 建立文档-标签关系
                    String insertRelationSql = "INSERT INTO document_tag_relations (document_id, tag_id, created_at) VALUES (?, ?, ?)";
                    jdbcTemplate.update(insertRelationSql, documentId, tagId, timestamp);
                }
            }

            // 7. 查询更新后的文档信息
            String querySql = "SELECT d.document_id, d.title, d.description, d.language, d.is_public, d.is_favorite, d.updated_at " +
                    "FROM documents d WHERE d.document_id = ? AND d.user_id = ?";

            List<Map<String, Object>> updatedDocuments = jdbcTemplate.queryForList(querySql, documentId, userId);
            Map<String, Object> updatedDocument = updatedDocuments.get(0);
            printQueryResult(updatedDocument);

            // 8. 查询文档标签
            String tagSql = "SELECT dt.tag_name FROM document_tag_relations dtr " +
                    "JOIN document_tags dt ON dtr.tag_id = dt.tag_id " +
                    "WHERE dtr.document_id = ?";
            List<Map<String, Object>> tagResults = jdbcTemplate.queryForList(tagSql, documentId);

            List<String> tags = new ArrayList<>();
            for (Map<String, Object> tag : tagResults) {
                tags.add((String) tag.get("tag_name"));
            }

            // 9. 构建响应数据
            UpdateDocumentDTO dto = new UpdateDocumentDTO();
            dto.setId((Integer) updatedDocument.get("document_id"));
            dto.setTitle((String) updatedDocument.get("title"));
            dto.setDescription((String) updatedDocument.get("description"));
            dto.setLanguage((String) updatedDocument.get("language"));
            dto.setTags(tags);
            dto.setIsPublic((Boolean) updatedDocument.get("is_public"));
            dto.setIsFavorite((Boolean) updatedDocument.get("is_favorite"));
            dto.setUpdatedAt(formatDate((java.sql.Timestamp) updatedDocument.get("updated_at")));

            UpdateData data = new UpdateData(dto);
            UpdateResponse response = new UpdateResponse(true, "文档更新成功", data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新文档过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 格式化日期（保持LocalDateTime类型）
    private LocalDateTime formatDate(java.sql.Timestamp timestamp) {
        if (timestamp == null) return null;
        return timestamp.toLocalDateTime();
    }
}