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
public class ReviewClearHistory {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到清空复习历史请求 ===");
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

    public static class ClearHistoryResponse {
        private boolean success;
        private String message;
        private ClearHistoryData data;

        public ClearHistoryResponse(boolean success, String message, ClearHistoryData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ClearHistoryData getData() { return data; }
        public void setData(ClearHistoryData data) { this.data = data; }
    }

    public static class ClearHistoryData {
        private int deleted_count;

        public ClearHistoryData(int deleted_count) {
            this.deleted_count = deleted_count;
        }

        public int getDeleted_count() { return deleted_count; }
        public void setDeleted_count(int deleted_count) { this.deleted_count = deleted_count; }
    }

    @DeleteMapping("/history")
    public ResponseEntity<ClearHistoryResponse> clearAllReviewHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        printRequest("清空所有复习历史");

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ClearHistoryResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ClearHistoryResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) users.get(0).get("user_id");
            System.out.println("当前用户ID: " + userId);

            // 2. 获取删除前的记录数
            String countSql = "SELECT COUNT(*) FROM review_sessions WHERE user_id = ?";
            int beforeCount = jdbcTemplate.queryForObject(countSql, Integer.class, userId);

            // 3. 删除所有复习历史
            String deleteSql = "DELETE FROM review_sessions WHERE user_id = ?";
            int deleted = jdbcTemplate.update(deleteSql, userId);

            printQueryResult("删除前记录数: " + beforeCount + ", 删除记录数: " + deleted);

            if (deleted > 0) {
                ClearHistoryData data = new ClearHistoryData(deleted);
                ClearHistoryResponse response = new ClearHistoryResponse(true, "清空复习历史成功", data);
                printResponse(response);
                return ResponseEntity.ok(response);
            } else {
                ClearHistoryData data = new ClearHistoryData(0);
                ClearHistoryResponse response = new ClearHistoryResponse(true, "没有可删除的复习历史", data);
                printResponse(response);
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            System.err.println("清空复习历史过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ClearHistoryResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
