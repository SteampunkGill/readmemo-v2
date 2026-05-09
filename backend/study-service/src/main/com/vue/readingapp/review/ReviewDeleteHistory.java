package com.vue.readingapp.review;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/review")
public class ReviewDeleteHistory {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到删除复习历史请求 ===");
        System.out.println("请求参数: " + request);
        System.out.println("时间: " + LocalDateTime.now());
        System.out.println("========================");
    }

    private void printQueryResult(Object result) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("查询结果: " + result);
        System.out.println("===================");
    }

    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    public static class DeleteHistoryResponse {
        private boolean success;
        private String message;

        public DeleteHistoryResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<DeleteHistoryResponse> deleteReviewHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String sessionId) {

        printRequest("sessionId: " + sessionId);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteHistoryResponse(false, "请先登录")
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteHistoryResponse(false, "登录已过期，请重新登录")
                );
            }

            int userId = (int) users.get(0).get("user_id");
            System.out.println("当前用户ID: " + userId);

            // 2. 检查会话是否存在
            String checkSql = "SELECT COUNT(*) FROM review_sessions " +
                    "WHERE session_id = ? AND user_id = ?";

            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, sessionId, userId);

            if (count == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteHistoryResponse(false, "复习会话不存在")
                );
            }

            // 3. 删除会话
            String deleteSql = "DELETE FROM review_sessions WHERE session_id = ? AND user_id = ?";
            int deleted = jdbcTemplate.update(deleteSql, sessionId, userId);

            printQueryResult("删除记录数: " + deleted);

            if (deleted > 0) {
                DeleteHistoryResponse response = new DeleteHistoryResponse(true, "删除复习历史成功");
                printResponse(response);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new DeleteHistoryResponse(false, "删除复习历史失败")
                );
            }

        } catch (Exception e) {
            System.err.println("删除复习历史过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DeleteHistoryResponse(false, "服务器内部错误: " + e.getMessage())
            );
        }
    }
}
