package com.vue.readingapp.reader;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderDeleteBookmark {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到删除书签请求 ===");
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
    public static class DeleteBookmarkResponse {
        private boolean success;
        private String message;
        private DeleteData data;

        public DeleteBookmarkResponse(boolean success, String message, DeleteData data) {
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

    @DeleteMapping("/documents/{documentId}/bookmarks/{bookmarkId}")
    public ResponseEntity<DeleteBookmarkResponse> deleteBookmark(
            @PathVariable("documentId") int documentId,
            @PathVariable("bookmarkId") int bookmarkId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("bookmarkId", bookmarkId);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteBookmarkResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteBookmarkResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 验证书签是否存在且属于当前用户
            String bookmarkSql = "SELECT * FROM reading_history WHERE history_id = ? AND document_id = ? AND user_id = ? AND is_bookmark = true";
            List<Map<String, Object>> bookmarks = jdbcTemplate.queryForList(bookmarkSql, bookmarkId, documentId, userId);

            if (bookmarks.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteBookmarkResponse(false, "书签不存在或没有权限删除", null)
                );
            }

            // 3. 获取书签信息（用于返回）
            Map<String, Object> bookmark = bookmarks.get(0);

            // 4. 删除书签
            String deleteSql = "DELETE FROM reading_history WHERE history_id = ? AND document_id = ? AND user_id = ?";
            int deletedRows = jdbcTemplate.update(deleteSql, bookmarkId, documentId, userId);

            if (deletedRows == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new DeleteBookmarkResponse(false, "删除书签失败", null)
                );
            }

            // 5. 构建响应数据
            DeleteData deleteData = new DeleteData();
            deleteData.setDeletedId(bookmarkId);
            deleteData.setDeletedAt(new Date().toString());

            DeleteBookmarkResponse response = new DeleteBookmarkResponse(true, "书签已删除", deleteData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("删除书签过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DeleteBookmarkResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}