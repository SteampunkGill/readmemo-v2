package com.vue.readingapp.reader;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderDeleteNote {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到删除笔记请求 ===");
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
    public static class DeleteNoteResponse {
        private boolean success;
        private String message;
        private DeleteData data;

        public DeleteNoteResponse(boolean success, String message, DeleteData data) {
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

    @DeleteMapping("/documents/{documentId}/notes/{noteId}")
    public ResponseEntity<DeleteNoteResponse> deleteNote(
            @PathVariable("documentId") int documentId,
            @PathVariable("noteId") int noteId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("noteId", noteId);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteNoteResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteNoteResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 验证笔记是否存在且属于当前用户
            String noteSql = "SELECT * FROM document_notes WHERE note_id = ? AND document_id = ? AND user_id = ?";
            List<Map<String, Object>> notes = jdbcTemplate.queryForList(noteSql, noteId, documentId, userId);

            if (notes.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteNoteResponse(false, "笔记不存在或没有权限删除", null)
                );
            }

            // 3. 获取笔记信息（用于返回）
            Map<String, Object> note = notes.get(0);

            // 4. 删除笔记
            String deleteSql = "DELETE FROM document_notes WHERE note_id = ? AND document_id = ? AND user_id = ?";
            int deletedRows = jdbcTemplate.update(deleteSql, noteId, documentId, userId);

            if (deletedRows == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new DeleteNoteResponse(false, "删除笔记失败", null)
                );
            }

            // 5. 构建响应数据
            DeleteData deleteData = new DeleteData();
            deleteData.setDeletedId(noteId);
            deleteData.setDeletedAt(new Date().toString());

            DeleteNoteResponse response = new DeleteNoteResponse(true, "笔记已删除", deleteData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("删除笔记过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DeleteNoteResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}