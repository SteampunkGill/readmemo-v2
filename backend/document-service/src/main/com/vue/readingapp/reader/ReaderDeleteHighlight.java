package com.vue.readingapp.reader;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderDeleteHighlight {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到删除高亮请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=====================");
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
    public static class DeleteHighlightResponse {
        private boolean success;
        private String message;
        private DeleteData data;

        public DeleteHighlightResponse(boolean success, String message, DeleteData data) {
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
        private int deletedId;
        private String deletedAt;

        public int getDeletedId() { return deletedId; }
        public void setDeletedId(int deletedId) { this.deletedId = deletedId; }

        public String getDeletedAt() { return deletedAt; }
        public void setDeletedAt(String deletedAt) { this.deletedAt = deletedAt; }
    }

    @DeleteMapping("/documents/{documentId}/highlights/{highlightId}")
    public ResponseEntity<DeleteHighlightResponse> deleteHighlight(
            @PathVariable("documentId") int documentId,
            @PathVariable("highlightId") int highlightId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("highlightId", highlightId);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteHighlightResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteHighlightResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 验证高亮是否存在且属于当前用户
            String highlightSql = "SELECT * FROM document_highlights WHERE highlight_id = ? AND document_id = ? AND user_id = ?";
            List<Map<String, Object>> highlights = jdbcTemplate.queryForList(highlightSql, highlightId, documentId, userId);

            if (highlights.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteHighlightResponse(false, "高亮不存在或没有权限删除", null)
                );
            }

            // 3. 先获取高亮信息（用于返回）
            Map<String, Object> highlight = highlights.get(0);

            // 4. 删除关联的笔记（如果有外键约束会自动级联删除，这里显式删除）
            String deleteNotesSql = "DELETE FROM document_notes WHERE highlight_id = ? AND user_id = ?";
            jdbcTemplate.update(deleteNotesSql, highlightId, userId);

            // 5. 删除高亮
            String deleteSql = "DELETE FROM document_highlights WHERE highlight_id = ? AND document_id = ? AND user_id = ?";
            int deletedRows = jdbcTemplate.update(deleteSql, highlightId, documentId, userId);

            if (deletedRows == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new DeleteHighlightResponse(false, "删除高亮失败", null)
                );
            }

            // 6. 构建响应数据
            DeleteData deleteData = new DeleteData();
            deleteData.setDeletedId(highlightId);
            deleteData.setDeletedAt(new Date().toString());

            DeleteHighlightResponse response = new DeleteHighlightResponse(true, "高亮已删除", deleteData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("删除高亮过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DeleteHighlightResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}