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
public class DocumentsBatchUpdate {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(BatchUpdateRequest request) {
        System.out.println("=== 收到批量更新文档请求 ===");
        System.out.println("文档ID列表: " + request.getDocumentIds());
        System.out.println("更新数据: " + request);
        System.out.println("=========================");
    }

    // 打印查询结果
    private void printQueryResult(int updatedCount, List<Integer> failedIds) {
        System.out.println("=== 批量更新结果 ===");
        System.out.println("成功更新数量: " + updatedCount);
        System.out.println("失败文档ID: " + failedIds);
        System.out.println("==================");
    }

    // 打印返回数据
    private void printResponse(BatchUpdateResponse response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 请求DTO
    public static class BatchUpdateRequest {
        private List<Integer> documentIds;
        private String title;
        private String description;
        private List<String> tags;
        private String language;
        private Boolean isPublic;
        private Boolean isFavorite;

        public List<Integer> getDocumentIds() { return documentIds; }
        public void setDocumentIds(List<Integer> documentIds) { this.documentIds = documentIds; }

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
    public static class BatchUpdateResponse {
        private boolean success;
        private String message;
        private BatchUpdateData data;

        public BatchUpdateResponse(boolean success, String message, BatchUpdateData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public BatchUpdateData getData() { return data; }
        public void setData(BatchUpdateData data) { this.data = data; }
    }

    public static class BatchUpdateData {
        private Integer processedCount;
        private Integer successCount;
        private Integer failedCount;
        private List<Integer> failedIds;

        public BatchUpdateData(Integer processedCount, Integer successCount, Integer failedCount, List<Integer> failedIds) {
            this.processedCount = processedCount;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.failedIds = failedIds;
        }

        public Integer getProcessedCount() { return processedCount; }
        public void setProcessedCount(Integer processedCount) { this.processedCount = processedCount; }

        public Integer getSuccessCount() { return successCount; }
        public void setSuccessCount(Integer successCount) { this.successCount = successCount; }

        public Integer getFailedCount() { return failedCount; }
        public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }

        public List<Integer> getFailedIds() { return failedIds; }
        public void setFailedIds(List<Integer> failedIds) { this.failedIds = failedIds; }
    }

    @PutMapping("/batch")
    public ResponseEntity<BatchUpdateResponse> batchUpdateDocuments(
            @RequestBody BatchUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest(request);

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchUpdateResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchUpdateResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 2. 验证请求数据
            if (request == null || request.getDocumentIds() == null || request.getDocumentIds().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchUpdateResponse(false, "文档ID列表不能为空", null)
                );
            }

            if (request.getTitle() == null && request.getDescription() == null &&
                    request.getTags() == null && request.getLanguage() == null &&
                    request.getIsPublic() == null && request.getIsFavorite() == null) {
                return ResponseEntity.badRequest().body(
                        new BatchUpdateResponse(false, "至少提供一个更新字段", null)
                );
            }

            List<Integer> documentIds = request.getDocumentIds();
            List<Integer> failedIds = new ArrayList<>();
            int successCount = 0;

            LocalDateTime now = LocalDateTime.now();
            Timestamp timestamp = Timestamp.valueOf(now);

            // 3. 批量更新每个文档
            for (Integer documentId : documentIds) {
                try {
                    // 检查文档是否存在且属于当前用户
                    String checkSql = "SELECT document_id FROM documents WHERE document_id = ? AND user_id = ? AND deleted_at IS NULL";
                    List<Map<String, Object>> documents = jdbcTemplate.queryForList(checkSql, documentId, userId);

                    if (documents.isEmpty()) {
                        failedIds.add(documentId);
                        continue;
                    }

                    // 构建更新SQL
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
                    updateSql.append("updated_at = ? ");
                    params.add(timestamp);

                    updateSql.append("WHERE document_id = ? AND user_id = ?");
                    params.add(documentId);
                    params.add(userId);

                    // 执行更新
                    int rowsUpdated = jdbcTemplate.update(updateSql.toString(), params.toArray());

                    if (rowsUpdated > 0) {
                        successCount++;

                        // 处理标签更新
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
                    } else {
                        failedIds.add(documentId);
                    }

                } catch (Exception e) {
                    System.err.println("更新文档 " + documentId + " 时发生错误: " + e.getMessage());
                    failedIds.add(documentId);
                }
            }

            // 4. 打印查询结果
            printQueryResult(successCount, failedIds);

            // 5. 构建响应数据
            BatchUpdateData data = new BatchUpdateData(
                    documentIds.size(),
                    successCount,
                    failedIds.size(),
                    failedIds
            );

            String message;
            if (failedIds.isEmpty()) {
                message = "成功更新 " + successCount + " 个文档";
            } else {
                message = "成功更新 " + successCount + " 个文档，" + failedIds.size() + " 个文档更新失败";
            }

            BatchUpdateResponse response = new BatchUpdateResponse(true, message, data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("批量更新文档过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BatchUpdateResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}