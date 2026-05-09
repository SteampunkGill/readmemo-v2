package com.vue.readingapp.tags;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
public class TagRemoveFromDocument {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到从文档移除标签请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("===========================");
    }

    // 打印返回数据
    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 响应类
    public static class RemoveTagFromDocumentResponse {
        private boolean success;
        private String message;

        public RemoveTagFromDocumentResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    @DeleteMapping("/{documentId}/tags/{tagId}")
    public ResponseEntity<RemoveTagFromDocumentResponse> removeTagFromDocument(
            @PathVariable("documentId") String documentId,
            @PathVariable("tagId") String tagId) {

        Map<String, Object> requestData = new java.util.HashMap<>();
        requestData.put("documentId", documentId);
        requestData.put("tagId", tagId);
        printRequest(requestData);

        try {
            // 验证文档是否存在
            String checkDocumentSql = "SELECT COUNT(*) FROM documents WHERE document_id = ?";
            int documentCount = jdbcTemplate.queryForObject(checkDocumentSql, Integer.class, Integer.parseInt(documentId));
            if (documentCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new RemoveTagFromDocumentResponse(false, "文档不存在")
                );
            }

            // 验证标签是否存在（在document_tags中）
            String checkTagSql = "SELECT COUNT(*) FROM document_tags WHERE tag_id = ?";
            int tagCount = jdbcTemplate.queryForObject(checkTagSql, Integer.class, Integer.parseInt(tagId));
            if (tagCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new RemoveTagFromDocumentResponse(false, "标签不存在")
                );
            }

            // 检查是否存在该关系
            String checkRelationSql = "SELECT COUNT(*) FROM document_tag_relations WHERE document_id = ? AND tag_id = ?";
            int relationCount = jdbcTemplate.queryForObject(checkRelationSql, Integer.class,
                    Integer.parseInt(documentId), Integer.parseInt(tagId));

            if (relationCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new RemoveTagFromDocumentResponse(false, "该标签未添加到文档")
                );
            }

            // 从文档移除标签
            String deleteSql = "DELETE FROM document_tag_relations WHERE document_id = ? AND tag_id = ?";
            int deleted = jdbcTemplate.update(deleteSql,
                    Integer.parseInt(documentId), Integer.parseInt(tagId));

            if (deleted == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new RemoveTagFromDocumentResponse(false, "标签移除失败")
                );
            }

            // 创建响应
            RemoveTagFromDocumentResponse response = new RemoveTagFromDocumentResponse(true, "标签移除成功");
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            System.err.println("ID格式错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(
                    new RemoveTagFromDocumentResponse(false, "ID格式错误")
            );
        } catch (Exception e) {
            System.err.println("从文档移除标签过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new RemoveTagFromDocumentResponse(false, "服务器内部错误: " + e.getMessage())
            );
        }
    }
}
