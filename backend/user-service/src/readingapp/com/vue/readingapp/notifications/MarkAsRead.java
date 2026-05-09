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
public class MarkAsRead {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到标记通知为已读请求 (MarkAsRead) ===");
        System.out.println("请求数据: " + request);
        System.out.println("========================================");
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

    // 请求DTO
    public static class MarkAsReadRequest {
        private String notificationId;

        public String getNotificationId() { return notificationId; }
        public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    }

    // 响应DTO
    public static class MarkAsReadResponse {
        private boolean success;
        private String message;
        private String notificationId;
        private String readAt;

        public MarkAsReadResponse(boolean success, String message, String notificationId, String readAt) {
            this.success = success;
            this.message = message;
            this.notificationId = notificationId;
            this.readAt = readAt;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getNotificationId() { return notificationId; }
        public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

        public String getReadAt() { return readAt; }
        public void setReadAt(String readAt) { this.readAt = readAt; }
    }

    @PutMapping("/mark-as-read")
    public ResponseEntity<MarkAsReadResponse> markAsRead(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody MarkAsReadRequest request) {

        try {
            // 打印接收到的请求
            printRequest(request);

            // 1. 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new MarkAsReadResponse(false, "请先登录", request.getNotificationId(), null)
                );
            }

            String token = authHeader.substring(7);

            // 2. 根据token获取用户ID
            String getUserSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(getUserSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new MarkAsReadResponse(false, "登录已过期，请重新登录", request.getNotificationId(), null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            Integer userId = (Integer) session.get("user_id");

            // 3. 验证请求数据
            if (request.getNotificationId() == null || request.getNotificationId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new MarkAsReadResponse(false, "通知ID不能为空", null, null)
                );
            }

            // 4. 提取notificationId的数字部分
            Integer notificationIdInt = null;
            try {
                if (request.getNotificationId().startsWith("notif_")) {
                    notificationIdInt = Integer.parseInt(request.getNotificationId().substring(6));
                } else {
                    notificationIdInt = Integer.parseInt(request.getNotificationId());
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(
                        new MarkAsReadResponse(false, "通知ID格式错误", request.getNotificationId(), null)
                );
            }

            // 5. 验证通知是否存在且属于当前用户
            String checkNotificationSql = "SELECT notification_id, is_read FROM notifications WHERE notification_id = ? AND user_id = ?";
            List<Map<String, Object>> notifications = jdbcTemplate.queryForList(checkNotificationSql, notificationIdInt, userId);

            if (notifications.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new MarkAsReadResponse(false, "通知不存在或无权访问", request.getNotificationId(), null)
                );
            }

            // 6. 检查通知是否已经是已读状态
            Map<String, Object> notification = notifications.get(0);
            Boolean isRead = (Boolean) notification.get("is_read");
            if (isRead != null && isRead) {
                return ResponseEntity.ok(
                        new MarkAsReadResponse(true, "通知已经是已读状态", request.getNotificationId(), null)
                );
            }

            // 7. 更新通知为已读状态
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            String formattedNow = now.format(formatter) + "Z";

            String updateSql = "UPDATE notifications SET is_read = true, read_at = ? WHERE notification_id = ? AND user_id = ?";
            int updatedRows = jdbcTemplate.update(updateSql, now, notificationIdInt, userId);

            printQueryResult("更新行数: " + updatedRows);

            if (updatedRows > 0) {
                MarkAsReadResponse response = new MarkAsReadResponse(
                        true,
                        "通知已标记为已读",
                        request.getNotificationId(),
                        formattedNow
                );

                // 打印返回数据
                printResponse(response);

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new MarkAsReadResponse(false, "更新通知状态失败", request.getNotificationId(), null)
                );
            }

        } catch (Exception e) {
            System.err.println("标记通知为已读过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new MarkAsReadResponse(false, "服务器内部错误: " + e.getMessage(), request.getNotificationId(), null)
            );
        }
    }
}