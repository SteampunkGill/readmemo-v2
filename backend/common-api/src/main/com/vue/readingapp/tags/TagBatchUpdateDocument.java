package com.vue.readingapp.tags;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents/tags")
public class TagBatchUpdateDocument {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到批量更新文档标签请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=============================");
    }

    // 打印返回数据
    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 请求DTO
    public static class BatchUpdateDocumentTagsRequest {
        private List<String> documentIds;
        private List<String> tagIds;
        private String operation; // add, remove, replace

        // Getters and Setters
        public List<String> getDocumentIds() { return documentIds; }
        public void setDocumentIds(List<String> documentIds) { this.documentIds = documentIds; }

        public List<String> getTagIds() { return tagIds; }
        public void setTagIds(List<String> tagIds) { this.tagIds = tagIds; }

        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
    }

    // 响应类
    public static class BatchUpdateDocumentTagsResponse {
        private boolean success;
        private String message;
        private int updatedCount;

        public BatchUpdateDocumentTagsResponse(boolean success, String message, int updatedCount) {
            this.success = success;
            this.message = message;
            this.updatedCount = updatedCount;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public int getUpdatedCount() { return updatedCount; }
        public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchUpdateDocumentTagsResponse> batchUpdateDocumentTags(
            @RequestBody BatchUpdateDocumentTagsRequest request) {

        printRequest(request);

        try {
            // 验证请求数据
            if (request.getDocumentIds() == null || request.getDocumentIds().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchUpdateDocumentTagsResponse(false, "文档ID列表不能为空", 0)
                );
            }

            if (request.getTagIds() == null || request.getTagIds().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchUpdateDocumentTagsResponse(false, "标签ID列表不能为空", 0)
                );
            }

            if (request.getOperation() == null || request.getOperation().trim().isEmpty()) {
                request.setOperation("replace"); // 默认操作
            }

            int updatedCount = 0;

            // 根据操作类型处理
            switch (request.getOperation().toLowerCase()) {
                case "add":
                    // 为多个文档添加多个标签
                    for (String documentId : request.getDocumentIds()) {
                        for (String tagId : request.getTagIds()) {
                            try {
                                // 检查文档是否存在
                                String checkDocumentSql = "SELECT COUNT(*) FROM documents WHERE document_id = ?";
                                int docCount = jdbcTemplate.queryForObject(checkDocumentSql, Integer.class, Integer.parseInt(documentId));

                                // 检查标签是否存在
                                String checkTagSql = "SELECT COUNT(*) FROM document_tags WHERE tag_id = ?";
                                int tagCount = jdbcTemplate.queryForObject(checkTagSql, Integer.class, Integer.parseInt(tagId));

                                if (docCount > 0 && tagCount > 0) {
                                    // 检查是否已经存在该关系
                                    String checkRelationSql = "SELECT COUNT(*) FROM document_tag_relations WHERE document_id = ? AND tag_id = ?";
                                    int relationCount = jdbcTemplate.queryForObject(checkRelationSql, Integer.class,
                                            Integer.parseInt(documentId), Integer.parseInt(tagId));

                                    if (relationCount == 0) {
                                        // 添加关系
                                        String insertSql = "INSERT INTO document_tag_relations (document_id, tag_id, created_at) VALUES (?, ?, NOW())";
                                        jdbcTemplate.update(insertSql, Integer.parseInt(documentId), Integer.parseInt(tagId));
                                        updatedCount++;
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("为文档 " + documentId + " 添加标签 " + tagId + " 时发生错误: " + e.getMessage());
                            }
                        }
                    }
                    break;

                case "remove":
                    // 从多个文档移除多个标签
                    for (String documentId : request.getDocumentIds()) {
                        for (String tagId : request.getTagIds()) {
                            try {
                                // 检查是否存在该关系
                                String checkRelationSql = "SELECT COUNT(*) FROM document_tag_relations WHERE document_id = ? AND tag_id = ?";
                                int relationCount = jdbcTemplate.queryForObject(checkRelationSql, Integer.class,
                                        Integer.parseInt(documentId), Integer.parseInt(tagId));

                                if (relationCount > 0) {
                                    // 移除关系
                                    String deleteSql = "DELETE FROM document_tag_relations WHERE document_id = ? AND tag_id = ?";
                                    jdbcTemplate.update(deleteSql, Integer.parseInt(documentId), Integer.parseInt(tagId));
                                    updatedCount++;
                                }
                            } catch (Exception e) {
                                System.err.println("从文档 " + documentId + " 移除标签 " + tagId + " 时发生错误: " + e.getMessage());
                            }
                        }
                    }
                    break;

                case "replace":
                    // 替换多个文档的标签（先删除所有旧标签，再添加新标签）
                    for (String documentId : request.getDocumentIds()) {
                        try {
                            // 先删除文档的所有标签
                            String deleteAllSql = "DELETE FROM document_tag_relations WHERE document_id = ?";
                            jdbcTemplate.update(deleteAllSql, Integer.parseInt(documentId));

                            // 然后添加新标签
                            for (String tagId : request.getTagIds()) {
                                // 检查文档和标签是否存在
                                String checkDocumentSql = "SELECT COUNT(*) FROM documents WHERE document_id = ?";
                                int docCount = jdbcTemplate.queryForObject(checkDocumentSql, Integer.class, Integer.parseInt(documentId));

                                String checkTagSql = "SELECT COUNT(*) FROM document_tags WHERE tag_id = ?";
                                int tagCount = jdbcTemplate.queryForObject(checkTagSql, Integer.class, Integer.parseInt(tagId));

                                if (docCount > 0 && tagCount > 0) {
                                    String insertSql = "INSERT INTO document_tag_relations (document_id, tag_id, created_at) VALUES (?, ?, NOW())";
                                    jdbcTemplate.update(insertSql, Integer.parseInt(documentId), Integer.parseInt(tagId));
                                    updatedCount++;
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("替换文档 " + documentId + " 的标签时发生错误: " + e.getMessage());
                        }
                    }
                    break;

                default:
                    return ResponseEntity.badRequest().body(
                            new BatchUpdateDocumentTagsResponse(false, "不支持的操作类型: " + request.getOperation(), 0)
                    );
            }

            // 创建响应
            String message = String.format("批量操作完成，成功更新 %d 个文档标签关系", updatedCount);
            BatchUpdateDocumentTagsResponse response = new BatchUpdateDocumentTagsResponse(
                    true, message, updatedCount);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("批量更新文档标签过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BatchUpdateDocumentTagsResponse(false, "服务器内部错误: " + e.getMessage(), 0)
            );
        }
    }
}
