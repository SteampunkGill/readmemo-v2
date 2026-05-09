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
public class NotificationUnsubscribe {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到取消订阅通知频道请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=============================");
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
    public static class UnsubscribeRequest {
        private String channel;

        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
    }

    // 响应DTO
    public static class UnsubscribeResponse {
        private boolean success;
        private String message;
        private String channel;
        private String unsubscribedAt;

        public UnsubscribeResponse(boolean success, String message, String channel, String unsubscribedAt) {
            this.success = success;
            this.message = message;
            this.channel = channel;
            this.unsubscribedAt = unsubscribedAt;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }

        public String getUnsubscribedAt() { return unsubscribedAt; }
        public void setUnsubscribedAt(String unsubscribedAt) { this.unsubscribedAt = unsubscribedAt; }
    }

    @DeleteMapping("/unsubscribe")
    public ResponseEntity<UnsubscribeResponse> unsubscribe(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UnsubscribeRequest request) {

        try {
            // 打印接收到的请求
            printRequest(request);

            // 1. 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UnsubscribeResponse(false, "请先登录", null, null)
                );
            }

            String token = authHeader.substring(7);

            // 2. 根据token获取用户ID
            String getUserSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(getUserSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UnsubscribeResponse(false, "登录已过期，请重新登录", null, null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            Integer userId = (Integer) session.get("user_id");

            // 3. 验证请求数据
            if (request.getChannel() == null || request.getChannel().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UnsubscribeResponse(false, "频道不能为空", null, null)
                );
            }

            // 验证频道类型
            String channel = request.getChannel().toLowerCase();
            if (!channel.equals("push") && !channel.equals("email") && !channel.equals("desktop")) {
                return ResponseEntity.badRequest().body(
                        new UnsubscribeResponse(false, "不支持的频道类型，支持的频道：push, email, desktop", null, null)
                );
            }

            // 4. 检查是否已订阅
            String checkSql = "SELECT COUNT(*) as count FROM notification_subscriptions WHERE user_id = ? AND channel = ? AND is_active = true";
            Map<String, Object> checkResult = jdbcTemplate.queryForMap(checkSql, userId, channel);
            long count = ((Number) checkResult.get("count")).longValue();

            if (count == 0) {
                return ResponseEntity.ok(
                        new UnsubscribeResponse(true, "您尚未订阅该频道", channel, null)
                );
            }

            // 5. 取消订阅
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            String formattedNow = now.format(formatter) + "Z";

            String updateSql = "UPDATE notification_subscriptions SET is_active = false, updated_at = ? WHERE user_id = ? AND channel = ?";
            int updatedRows = jdbcTemplate.update(updateSql, now, userId, channel);

            printQueryResult("更新行数: " + updatedRows);

            if (updatedRows > 0) {
                // 6. 更新通知设置中的对应频道
                String updateSettingsSql = "UPDATE notification_settings SET ";
                if (channel.equals("push")) {
                    updateSettingsSql += "push_notifications = false";
                } else if (channel.equals("email")) {
                    updateSettingsSql += "email_notifications = false";
                } else if (channel.equals("desktop")) {
                    updateSettingsSql += "desktop_notifications = false";
                }
                updateSettingsSql += ", updated_at = ? WHERE user_id = ?";

                jdbcTemplate.update(updateSettingsSql, now, userId);

                // 7. 创建响应
                UnsubscribeResponse response = new UnsubscribeResponse(
                        true,
                        "取消订阅通知频道成功",
                        channel,
                        formattedNow
                );

                // 打印返回数据
                printResponse(response);

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new UnsubscribeResponse(false, "取消订阅通知频道失败", null, null)
                );
            }

        } catch (Exception e) {
            System.err.println("取消订阅通知频道过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UnsubscribeResponse(false, "服务器内部错误: " + e.getMessage(), null, null)
            );
        }
    }
}