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
public class DocumentsUnshare {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Integer documentId, String authHeader) {
        System.out.println("=== 收到取消分享文档请求 ===");
        System.out.println("文档ID: " + documentId);
        System.out.println("认证头: " + authHeader);
        System.out.println("=========================");
    }

    // 打印查询结果
    private void printQueryResult(Map<String, Object> document) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("文档信息: " + document);
        System.out.println("===================");
    }

    // 打印返回数据
    private void printResponse(UnshareResponse response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 响应DTO
    public static class UnshareResponse {
        private boolean success;
        private String message;
        private UnshareData data;

        public UnshareResponse(boolean success, String message, UnshareData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UnshareData getData() { return data; }
        public void setData(UnshareData data) { this.data = data; }
    }

    public static class UnshareData {
        private UnshareDocumentDTO document;

        public UnshareData(UnshareDocumentDTO document) {
            this.document = document;
        }

        public UnshareDocumentDTO getDocument() { return document; }
        public void setDocument(UnshareDocumentDTO document) { this.document = document; }
    }

    public static class UnshareDocumentDTO {
        private Integer id;
        private String title;
        private Boolean isPublic;
        private String updatedAt;

        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public Boolean getIsPublic() { return isPublic; }
        public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    @DeleteMapping("/{documentId}/share")
    public ResponseEntity<UnshareResponse> unshareDocument(
            @PathVariable("documentId") Integer documentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest(documentId, authHeader);

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UnshareResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UnshareResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 2. 检查文档是否存在且属于当前用户
            String checkSql = "SELECT document_id, title FROM documents WHERE document_id = ? AND user_id = ? AND deleted_at IS NULL";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(checkSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UnshareResponse(false, "文档不存在或没有权限", null)
                );
            }

            Map<String, Object> document = documents.get(0);
            String documentTitle = (String) document.get("title");

            // 3. 取消分享（设置is_public为false，清除分享信息）
            LocalDateTime now = LocalDateTime.now();
            Timestamp timestamp = Timestamp.valueOf(now);

            String updateSql = "UPDATE documents SET is_public = FALSE, share_link = NULL, share_password = NULL, " +
                    "share_expiry_date = NULL, updated_at = ? WHERE document_id = ? AND user_id = ?";

            int rowsUpdated = jdbcTemplate.update(updateSql, timestamp, documentId, userId);

            if (rowsUpdated == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UnshareResponse(false, "取消分享失败", null)
                );
            }

            // 4. 清除分享权限记录（如果有相关表）
            // 简化实现：暂时不实现

            // 5. 查询更新后的文档信息
            String querySql = "SELECT document_id, title, is_public, updated_at FROM documents WHERE document_id = ? AND user_id = ?";
            List<Map<String, Object>> updatedDocuments = jdbcTemplate.queryForList(querySql, documentId, userId);
            Map<String, Object> updatedDocument = updatedDocuments.get(0);

            printQueryResult(updatedDocument);

            // 6. 构建响应数据
            UnshareDocumentDTO dto = new UnshareDocumentDTO();
            dto.setId((Integer) updatedDocument.get("document_id"));
            dto.setTitle((String) updatedDocument.get("title"));
            dto.setIsPublic((Boolean) updatedDocument.get("is_public"));
            dto.setUpdatedAt(formatDate((java.sql.Timestamp) updatedDocument.get("updated_at")));

            UnshareData data = new UnshareData(dto);
            UnshareResponse response = new UnshareResponse(true, "文档 '" + documentTitle + "' 取消分享成功", data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("取消分享文档过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UnshareResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 格式化日期
    private String formatDate(java.sql.Timestamp timestamp) {
        if (timestamp == null) return "";
        return timestamp.toLocalDateTime().toString();
    }
}