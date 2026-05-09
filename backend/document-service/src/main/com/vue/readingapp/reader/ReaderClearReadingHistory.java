
package com.vue.readingapp.reader;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderClearReadingHistory {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到清空阅读历史请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=========================");
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
    public static class ClearHistoryResponse {
        private boolean success;
        private String message;
        private ClearData data;

        public ClearHistoryResponse(boolean success, String message, ClearData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ClearData getData() { return data; }
        public void setData(ClearData data) { this.data = data; }
    }

    public static class ClearData {
        private int clearedCount;
        private String clearedAt;

        public int getClearedCount() { return clearedCount; }
        public void setClearedCount(int clearedCount) { this.clearedCount = clearedCount; }

        public String getClearedAt() { return clearedAt; }
        public void setClearedAt(String clearedAt) { this.clearedAt = clearedAt; }
    }

    @DeleteMapping("/documents/{documentId}/reading-history")
    public ResponseEntity<ClearHistoryResponse> clearReadingHistory(
            @PathVariable("documentId") int documentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ClearHistoryResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ClearHistoryResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 验证文档权限
            String docSql = "SELECT * FROM documents WHERE document_id = ? AND user_id = ?";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(docSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new ClearHistoryResponse(false, "没有权限清空此文档的阅读历史", null)
                );
            }

            // 3. 获取要删除的记录数
            String countSql = "SELECT COUNT(*) as total FROM reading_history WHERE user_id = ? AND document_id = ?";
            List<Map<String, Object>> counts = jdbcTemplate.queryForList(countSql, userId, documentId);
            int total = counts.isEmpty() ? 0 : ((Number) counts.get(0).get("total")).intValue();

            // 4. 删除阅读历史
            String deleteSql = "DELETE FROM reading_history WHERE user_id = ? AND document_id = ?";
            int deletedRows = jdbcTemplate.update(deleteSql, userId, documentId);

            if (deletedRows == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ClearHistoryResponse(false, "没有找到阅读历史记录", null)
                );
            }

            // 5. 构建响应数据
            ClearData clearData = new ClearData();
            clearData.setClearedCount(deletedRows);
            clearData.setClearedAt(new Date().toString());

            ClearHistoryResponse response = new ClearHistoryResponse(true, "阅读历史已清空", clearData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("清空阅读历史过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ClearHistoryResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}