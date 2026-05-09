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
public class DocumentsMove {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Integer documentId, MoveRequest request, String authHeader) {
        System.out.println("=== 收到移动文档请求 ===");
        System.out.println("文档ID: " + documentId);
        System.out.println("目标文件夹ID: " + request.getFolderId());
        System.out.println("认证头: " + authHeader);
        System.out.println("=====================");
    }

    // 打印查询结果
    private void printQueryResult(Map<String, Object> document) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("文档信息: " + document);
        System.out.println("===================");
    }

    // 打印返回数据
    private void printResponse(MoveResponse response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 请求DTO
    public static class MoveRequest {
        private Integer folderId;

        public Integer getFolderId() { return folderId; }
        public void setFolderId(Integer folderId) { this.folderId = folderId; }
    }

    // 响应DTO
    public static class MoveResponse {
        private boolean success;
        private String message;
        private MoveData data;

        public MoveResponse(boolean success, String message, MoveData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public MoveData getData() { return data; }
        public void setData(MoveData data) { this.data = data; }
    }

    public static class MoveData {
        private MoveDocumentDTO document;

        public MoveData(MoveDocumentDTO document) {
            this.document = document;
        }

        public MoveDocumentDTO getDocument() { return document; }
        public void setDocument(MoveDocumentDTO document) { this.document = document; }
    }

    public static class MoveDocumentDTO {
        private Integer id;
        private String title;
        private Integer folderId;
        private String updatedAt;

        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public Integer getFolderId() { return folderId; }
        public void setFolderId(Integer folderId) { this.folderId = folderId; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    @PutMapping("/{documentId}/move")
    public ResponseEntity<MoveResponse> moveDocument(
            @PathVariable("documentId") Integer documentId,
            @RequestBody MoveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest(documentId, request, authHeader);

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new MoveResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new MoveResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 2. 验证请求数据
            if (request == null || request.getFolderId() == null) {
                return ResponseEntity.badRequest().body(
                        new MoveResponse(false, "目标文件夹ID不能为空", null)
                );
            }

            // 3. 检查文档是否存在且属于当前用户
            String checkSql = "SELECT document_id, title FROM documents WHERE document_id = ? AND user_id = ? AND deleted_at IS NULL";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(checkSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new MoveResponse(false, "文档不存在或没有权限", null)
                );
            }

            Map<String, Object> document = documents.get(0);
            String documentTitle = (String) document.get("title");

            // 4. 检查目标文件夹是否存在（如果提供了文件夹ID）
            // 注意：根据数据库设计，没有文件夹表，这里简化处理
            // 实际项目中应该有文件夹表，并检查文件夹是否存在且属于当前用户
            if (request.getFolderId() > 0) {
                // 这里应该查询文件夹表，但数据库设计中没有
                // 简化处理：假设文件夹存在
                System.out.println("移动文档到文件夹: " + request.getFolderId());
            }

            // 5. 更新文档的文件夹ID
            // 注意：根据提供的数据库表结构，documents表中没有folder_id字段
            // 这里假设有folder_id字段，或者我们使用其他方式存储文件夹关系
            // 简化实现：假设有folder_id字段

            LocalDateTime now = LocalDateTime.now();
            Timestamp timestamp = Timestamp.valueOf(now);

            String updateSql = "UPDATE documents SET folder_id = ?, updated_at = ? WHERE document_id = ? AND user_id = ?";
            int rowsUpdated = jdbcTemplate.update(updateSql, request.getFolderId(), timestamp, documentId, userId);

            if (rowsUpdated == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new MoveResponse(false, "文档移动失败", null)
                );
            }

            // 6. 查询更新后的文档信息
            String querySql = "SELECT document_id, title, folder_id, updated_at FROM documents WHERE document_id = ? AND user_id = ?";
            List<Map<String, Object>> updatedDocuments = jdbcTemplate.queryForList(querySql, documentId, userId);
            Map<String, Object> updatedDocument = updatedDocuments.get(0);

            printQueryResult(updatedDocument);

            // 7. 构建响应数据
            MoveDocumentDTO dto = new MoveDocumentDTO();
            dto.setId((Integer) updatedDocument.get("document_id"));
            dto.setTitle((String) updatedDocument.get("title"));
            dto.setFolderId((Integer) updatedDocument.get("folder_id"));
            dto.setUpdatedAt(formatDate((java.sql.Timestamp) updatedDocument.get("updated_at")));

            MoveData data = new MoveData(dto);
            MoveResponse response = new MoveResponse(true, "文档 '" + documentTitle + "' 移动成功", data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("移动文档过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new MoveResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 格式化日期
    private String formatDate(java.sql.Timestamp timestamp) {
        if (timestamp == null) return "";
        return timestamp.toLocalDateTime().toString();
    }
}