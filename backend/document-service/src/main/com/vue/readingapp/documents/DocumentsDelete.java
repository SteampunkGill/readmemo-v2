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
public class DocumentsDelete {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Integer documentId, String authHeader) {
        System.out.println("=== 收到删除文档请求 ===");
        System.out.println("文档ID: " + documentId);
        System.out.println("认证头: " + authHeader);
        System.out.println("=====================");
    }

    // 打印返回数据
    private void printResponse(DeleteResponse response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 响应DTO
    public static class DeleteResponse {
        private boolean success;
        private String message;
        private DeleteData data;

        public DeleteResponse(boolean success, String message, DeleteData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public DeleteData getData() { return data; }
        public void setData(DeleteData data) { this.data = data; }
    }

    public static class DeleteData {
        private Integer deletedId;

        public DeleteData(Integer deletedId) {
            this.deletedId = deletedId;
        }

        public Integer getDeletedId() { return deletedId; }
        public void setDeletedId(Integer deletedId) { this.deletedId = deletedId; }
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<DeleteResponse> deleteDocument(
            @PathVariable("documentId") Integer documentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest(documentId, authHeader);

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 2. 检查文档是否存在且属于当前用户
            // 修改：移除 deleted_at IS NULL 条件，因为表中没有这个字段
            String checkSql = "SELECT document_id, title FROM documents WHERE document_id = ? AND user_id = ?";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(checkSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteResponse(false, "文档不存在或没有权限", null)
                );
            }

            Map<String, Object> document = documents.get(0);
            String documentTitle = (String) document.get("title");

            // 3. 执行硬删除（直接删除记录）
            // 修改：使用 DELETE 语句而不是 UPDATE
            String deleteSql = "DELETE FROM documents WHERE document_id = ? AND user_id = ?";
            int rowsDeleted = jdbcTemplate.update(deleteSql, documentId, userId);

            if (rowsDeleted == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteResponse(false, "文档删除失败", null)
                );
            }

            // 4. 从处理队列中移除（如果存在）
            String deleteQueueSql = "DELETE FROM document_processing_queue WHERE document_id = ?";
            jdbcTemplate.update(deleteQueueSql, documentId);

            // 5. 构建响应数据
            DeleteData data = new DeleteData(documentId);
            DeleteResponse response = new DeleteResponse(true, "文档 '" + documentTitle + "' 删除成功", data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("删除文档过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DeleteResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}