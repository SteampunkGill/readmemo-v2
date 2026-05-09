package com.vue.readingapp.notifications;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationDelete {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到删除通知请求 ===");
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

    // 请求DTO
    public static class DeleteNotificationRequest {
        private String notificationId;

        public String getNotificationId() { return notificationId; }
        public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    }

    // 响应DTO
    public static class DeleteNotificationResponse {
        private boolean success;
        private String message;
        private String notificationId;

        public DeleteNotificationResponse(boolean success, String message, String notificationId) {
            this.success = success;
            this.message = message;
            this.notificationId = notificationId;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getNotificationId() { return notificationId; }
        public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<DeleteNotificationResponse> deleteNotification(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String notificationId) {

        try {
            // 创建请求对象
            DeleteNotificationRequest request = new DeleteNotificationRequest();
            request.setNotificationId(notificationId);

            // 打印接收到的请求
            printRequest(request);

            // 1. 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteNotificationResponse(false, "请先登录", notificationId)
                );
            }

            String token = authHeader.substring(7);

            // 2. 根据token获取用户ID
            String getUserSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(getUserSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteNotificationResponse(false, "登录已过期，请重新登录", notificationId)
                );
            }

            Map<String, Object> session = sessions.get(0);
            Integer userId = (Integer) session.get("user_id");

            // 3. 提取notificationId的数字部分
            Integer notificationIdInt = null;
            try {
                if (notificationId.startsWith("notif_")) {
                    notificationIdInt = Integer.parseInt(notificationId.substring(6));
                } else {
                    notificationIdInt = Integer.parseInt(notificationId);
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(
                        new DeleteNotificationResponse(false, "通知ID格式错误", notificationId)
                );
            }

            // 4. 验证通知是否存在且属于当前用户
            String checkNotificationSql = "SELECT notification_id FROM notifications WHERE notification_id = ? AND user_id = ?";
            List<Map<String, Object>> notifications = jdbcTemplate.queryForList(checkNotificationSql, notificationIdInt, userId);

            if (notifications.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteNotificationResponse(false, "通知不存在或无权访问", notificationId)
                );
            }

            // 5. 删除通知
            String deleteSql = "DELETE FROM notifications WHERE notification_id = ? AND user_id = ?";
            int deletedRows = jdbcTemplate.update(deleteSql, notificationIdInt, userId);

            printQueryResult("删除行数: " + deletedRows);

            if (deletedRows > 0) {
                DeleteNotificationResponse response = new DeleteNotificationResponse(
                        true,
                        "通知删除成功",
                        notificationId
                );

                // 打印返回数据
                printResponse(response);

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new DeleteNotificationResponse(false, "删除通知失败", notificationId)
                );
            }

        } catch (Exception e) {
            System.err.println("删除通知过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DeleteNotificationResponse(false, "服务器内部错误: " + e.getMessage(), notificationId)
            );
        }
    }
}