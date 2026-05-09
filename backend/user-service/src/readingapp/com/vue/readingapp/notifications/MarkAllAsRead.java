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
public class MarkAllAsRead {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到标记所有通知为已读请求 (MarkAllAsRead) ===");
        System.out.println("请求数据: " + request);
        System.out.println("==============================================");
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
    public static class MarkAllAsReadResponse {
        private boolean success;
        private String message;
        private int markedCount;
        private String timestamp;

        public MarkAllAsReadResponse(boolean success, String message, int markedCount, String timestamp) {
            this.success = success;
            this.message = message;
            this.markedCount = markedCount;
            this.timestamp = timestamp;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public int getMarkedCount() { return markedCount; }
        public void setMarkedCount(int markedCount) { this.markedCount = markedCount; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    @PutMapping("/mark-all-as-read")
    public ResponseEntity<MarkAllAsReadResponse> markAllAsRead(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest("标记所有通知为已读");

            // 1. 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new MarkAllAsReadResponse(false, "请先登录", 0, null)
                );
            }

            String token = authHeader.substring(7);

            // 2. 根据token获取用户ID
            String getUserSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(getUserSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new MarkAllAsReadResponse(false, "登录已过期，请重新登录", 0, null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            Integer userId = (Integer) session.get("user_id");

            // 3. 查询当前用户有多少未读通知
            String countUnreadSql = "SELECT COUNT(*) as unread_count FROM notifications WHERE user_id = ? AND is_read = false";
            Map<String, Object> countResult = jdbcTemplate.queryForMap(countUnreadSql, userId);
            long unreadCount = ((Number) countResult.get("unread_count")).longValue();

            printQueryResult("未读通知数量: " + unreadCount);

            if (unreadCount == 0) {
                return ResponseEntity.ok(
                        new MarkAllAsReadResponse(true, "没有未读通知", 0, null)
                );
            }

            // 4. 更新所有未读通知为已读状态
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            String formattedNow = now.format(formatter) + "Z";

            String updateSql = "UPDATE notifications SET is_read = true, read_at = ? WHERE user_id = ? AND is_read = false";
            int updatedRows = jdbcTemplate.update(updateSql, now, userId);

            printQueryResult("更新行数: " + updatedRows);

            if (updatedRows > 0) {
                MarkAllAsReadResponse response = new MarkAllAsReadResponse(
                        true,
                        "所有通知已标记为已读",
                        updatedRows,
                        formattedNow
                );

                // 打印返回数据
                printResponse(response);

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new MarkAllAsReadResponse(false, "更新通知状态失败", 0, null)
                );
            }

        } catch (Exception e) {
            System.err.println("标记所有通知为已读过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new MarkAllAsReadResponse(false, "服务器内部错误: " + e.getMessage(), 0, null)
            );
        }
    }
}