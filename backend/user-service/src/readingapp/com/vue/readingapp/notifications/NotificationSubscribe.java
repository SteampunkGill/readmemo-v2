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
public class NotificationSubscribe {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到订阅通知频道请求 ===");
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

    // 请求DTO
    public static class SubscribeRequest {
        private String channel;
        private String deviceToken;

        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }

        public String getDeviceToken() { return deviceToken; }
        public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
    }

    // 响应DTO
    public static class SubscribeResponse {
        private boolean success;
        private String message;
        private String channel;
        private String subscribedAt;

        public SubscribeResponse(boolean success, String message, String channel, String subscribedAt) {
            this.success = success;
            this.message = message;
            this.channel = channel;
            this.subscribedAt = subscribedAt;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }

        public String getSubscribedAt() { return subscribedAt; }
        public void setSubscribedAt(String subscribedAt) { this.subscribedAt = subscribedAt; }
    }

    @PostMapping("/subscribe")
    public ResponseEntity<SubscribeResponse> subscribe(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SubscribeRequest request) {

        try {
            // 打印接收到的请求
            printRequest(request);

            // 1. 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SubscribeResponse(false, "请先登录", null, null)
                );
            }

            String token = authHeader.substring(7);

            // 2. 根据token获取用户ID
            String getUserSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(getUserSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SubscribeResponse(false, "登录已过期，请重新登录", null, null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            Integer userId = (Integer) session.get("user_id");

            // 3. 验证请求数据
            if (request.getChannel() == null || request.getChannel().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SubscribeResponse(false, "频道不能为空", null, null)
                );
            }

            // 验证频道类型
            String channel = request.getChannel().toLowerCase();
            if (!channel.equals("push") && !channel.equals("email") && !channel.equals("desktop")) {
                return ResponseEntity.badRequest().body(
                        new SubscribeResponse(false, "不支持的频道类型，支持的频道：push, email, desktop", null, null)
                );
            }

            // 如果是push频道，需要设备令牌
            if (channel.equals("push") && (request.getDeviceToken() == null || request.getDeviceToken().trim().isEmpty())) {
                return ResponseEntity.badRequest().body(
                        new SubscribeResponse(false, "push频道需要设备令牌", null, null)
                );
            }

            // 4. 检查是否已订阅
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            String formattedNow = now.format(formatter) + "Z";

            String checkSql = "SELECT COUNT(*) as count FROM notification_subscriptions WHERE user_id = ? AND channel = ?";
            Map<String, Object> checkResult = jdbcTemplate.queryForMap(checkSql, userId, channel);
            long count = ((Number) checkResult.get("count")).longValue();

            int updatedRows;

            if (count > 0) {
                // 更新现有订阅
                String updateSql = "UPDATE notification_subscriptions SET device_token = ?, is_active = true, updated_at = ? WHERE user_id = ? AND channel = ?";
                updatedRows = jdbcTemplate.update(updateSql,
                        request.getDeviceToken(),
                        now,
                        userId,
                        channel
                );
            } else {
                // 插入新订阅
                String insertSql = "INSERT INTO notification_subscriptions (user_id, channel, device_token, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
                updatedRows = jdbcTemplate.update(insertSql,
                        userId,
                        channel,
                        request.getDeviceToken(),
                        true,
                        now,
                        now
                );
            }

            printQueryResult("更新行数: " + updatedRows);

            if (updatedRows > 0) {
                // 5. 更新通知设置中的对应频道
                String updateSettingsSql = "UPDATE notification_settings SET ";
                if (channel.equals("push")) {
                    updateSettingsSql += "push_notifications = true";
                } else if (channel.equals("email")) {
                    updateSettingsSql += "email_notifications = true";
                } else if (channel.equals("desktop")) {
                    updateSettingsSql += "desktop_notifications = true";
                }
                updateSettingsSql += ", updated_at = ? WHERE user_id = ?";

                jdbcTemplate.update(updateSettingsSql, now, userId);

                // 6. 创建响应
                SubscribeResponse response = new SubscribeResponse(
                        true,
                        "订阅通知频道成功",
                        channel,
                        formattedNow
                );

                // 打印返回数据
                printResponse(response);

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new SubscribeResponse(false, "订阅通知频道失败", null, null)
                );
            }

        } catch (Exception e) {
            System.err.println("订阅通知频道过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SubscribeResponse(false, "服务器内部错误: " + e.getMessage(), null, null)
            );
        }
    }
}