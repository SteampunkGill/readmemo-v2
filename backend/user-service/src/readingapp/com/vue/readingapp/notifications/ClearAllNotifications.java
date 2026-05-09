package com.vue.readingapp.notifications;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class ClearAllNotifications {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到清除所有通知请求 ===");
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
    public static class ClearAllNotificationsResponse {
        private boolean success;
        private String message;
        private int clearedCount;
        private String timestamp;

        public ClearAllNotificationsResponse(boolean success, String message, int clearedCount, String timestamp) {
            this.success = success;
            this.message = message;
            this.clearedCount = clearedCount;
            this.timestamp = timestamp;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public int getClearedCount() { return clearedCount; }
        public void setClearedCount(int clearedCount) { this.clearedCount = clearedCount; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<ClearAllNotificationsResponse> clearAllNotifications(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest("清除所有通知");

            // 1. 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ClearAllNotificationsResponse(false, "请先登录", 0, null)
                );
            }

            String token = authHeader.substring(7);

            // 2. 根据token获取用户ID
            String getUserSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(getUserSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ClearAllNotificationsResponse(false, "登录已过期，请重新登录", 0, null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            Integer userId = (Integer) session.get("user_id");

            // 3. 查询要删除的通知总数
            String countSql = "SELECT COUNT(*) as total_count FROM notifications WHERE user_id = ?";
            Map<String, Object> countResult = jdbcTemplate.queryForMap(countSql, userId);
            long totalCount = ((Number) countResult.get("total_count")).longValue();

            printQueryResult("要删除的通知总数: " + totalCount);

            if (totalCount == 0) {
                return ResponseEntity.ok(
                        new ClearAllNotificationsResponse(true, "没有通知可清除", 0, null)
                );
            }

            // 4. 删除所有通知
            String deleteSql = "DELETE FROM notifications WHERE user_id = ?";
            int deletedRows = jdbcTemplate.update(deleteSql, userId);

            printQueryResult("删除行数: " + deletedRows);

            if (deletedRows > 0) {
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                String formattedNow = now.format(formatter) + "Z";

                ClearAllNotificationsResponse response = new ClearAllNotificationsResponse(
                        true,
                        "所有通知已清除",
                        deletedRows,
                        formattedNow
                );

                // 打印返回数据
                printResponse(response);

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new ClearAllNotificationsResponse(false, "清除通知失败", 0, null)
                );
            }

        } catch (Exception e) {
            System.err.println("清除所有通知过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ClearAllNotificationsResponse(false, "服务器内部错误: " + e.getMessage(), 0, null)
            );
        }
    }
}