package com.vue.readingapp.notifications;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationGetUnreadCount {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取未读通知数量请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("===========================");
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
    public static class GetUnreadCountResponse {
        private boolean success;
        private String message;
        private int unreadCount;

        public GetUnreadCountResponse(boolean success, String message, int unreadCount) {
            this.success = success;
            this.message = message;
            this.unreadCount = unreadCount;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public int getUnreadCount() { return unreadCount; }
        public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<GetUnreadCountResponse> getUnreadCount(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest("获取未读通知数量");

            // 1. 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new GetUnreadCountResponse(false, "请先登录", 0)
                );
            }

            String token = authHeader.substring(7);

            // 2. 根据token获取用户ID
            String getUserSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(getUserSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new GetUnreadCountResponse(false, "登录已过期，请重新登录", 0)
                );
            }

            Map<String, Object> session = sessions.get(0);
            Integer userId = (Integer) session.get("user_id");

            // 3. 查询未读通知数量
            String countSql = "SELECT COUNT(*) as unread_count FROM notifications WHERE user_id = ? AND is_read = false";
            Map<String, Object> countResult = jdbcTemplate.queryForMap(countSql, userId);
            long unreadCount = ((Number) countResult.get("unread_count")).longValue();

            printQueryResult("未读通知数量: " + unreadCount);

            // 4. 创建响应
            GetUnreadCountResponse response = new GetUnreadCountResponse(
                    true,
                    "获取未读通知数量成功",
                    (int) unreadCount
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取未读通知数量过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetUnreadCountResponse(false, "服务器内部错误: " + e.getMessage(), 0)
            );
        }
    }
}