package com.vue.readingapp.notifications;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到测试通知发送请求 ===");
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

    // 计算相对时间
    private String calculateRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "未知时间";
        }

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(dateTime, now);

        long seconds = duration.getSeconds();
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;
        long years = days / 365;

        if (seconds < 60) {
            return "刚刚";
        } else if (minutes < 60) {
            return minutes + "分钟前";
        } else if (hours < 24) {
            return hours + "小时前";
        } else if (days < 7) {
            return days + "天前";
        } else if (weeks < 4) {
            return weeks + "周前";
        } else if (months < 12) {
            return months + "个月前";
        } else {
            return years + "年前";
        }
    }

    // 请求DTO
    public static class TestNotificationRequest {
        private String type = "test";
        private String title = "测试通知";
        private String message = "这是一条测试通知";
        private String icon = "/icons/test.png";
        private String targetType = "test";
        private String targetId = "test_123";

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }

        public String getTargetType() { return targetType; }
        public void setTargetType(String targetType) { this.targetType = targetType; }

        public String getTargetId() { return targetId; }
        public void setTargetId(String targetId) { this.targetId = targetId; }
    }

    // 响应DTO
    public static class TestNotificationResponse {
        private boolean success;
        private String message;
        private NotificationData notification;

        public TestNotificationResponse(boolean success, String message, NotificationData notification) {
            this.success = success;
            this.message = message;
            this.notification = notification;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public NotificationData getNotification() { return notification; }
        public void setNotification(NotificationData notification) { this.notification = notification; }
    }

    public static class NotificationData {
        private String id;
        private String type;
        private String title;
        private String message;
        private String icon;
        private String image;
        private boolean read;
        private String readAt;
        private String createdAt;
        private String relativeTime;
        private String actionUrl;
        private String targetType;
        private String targetId;
        private Map<String, Object> metadata;

        public NotificationData() {
            this.metadata = new HashMap<>();
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }

        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }

        public boolean isRead() { return read; }
        public void setRead(boolean read) { this.read = read; }

        public String getReadAt() { return readAt; }
        public void setReadAt(String readAt) { this.readAt = readAt; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getRelativeTime() { return relativeTime; }
        public void setRelativeTime(String relativeTime) { this.relativeTime = relativeTime; }

        public String getActionUrl() { return actionUrl; }
        public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }

        public String getTargetType() { return targetType; }
        public void setTargetType(String targetType) { this.targetType = targetType; }

        public String getTargetId() { return targetId; }
        public void setTargetId(String targetId) { this.targetId = targetId; }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    @PostMapping("/test")
    public ResponseEntity<TestNotificationResponse> testNotification(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody TestNotificationRequest request) {

        try {
            // 打印接收到的请求
            printRequest(request);

            // 1. 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new TestNotificationResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 2. 根据token获取用户ID
            String getUserSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(getUserSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new TestNotificationResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            Integer userId = (Integer) session.get("user_id");

            // 3. 准备测试通知数据
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            String formattedNow = now.format(formatter) + "Z";

            // 创建测试metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("test", true);
            metadata.put("testTime", formattedNow);
            metadata.put("purpose", "测试通知功能是否正常");

            String metadataJson = objectMapper.writeValueAsString(metadata);

            // 4. 插入测试通知到数据库
            String insertSql = "INSERT INTO notifications (user_id, type, title, message, icon_url, action_url, target_type, target_id, metadata, is_read, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            int insertedRows = jdbcTemplate.update(insertSql,
                    userId,
                    request.getType(),
                    request.getTitle(),
                    request.getMessage(),
                    request.getIcon(),
                    "/test",
                    request.getTargetType(),
                    request.getTargetId(),
                    metadataJson,
                    false, // is_read默认为false
                    now
            );

            printQueryResult("插入行数: " + insertedRows);

            if (insertedRows > 0) {
                // 5. 获取刚插入的通知ID
                String getLastIdSql = "SELECT LAST_INSERT_ID() as last_id";
                Map<String, Object> lastIdResult = jdbcTemplate.queryForMap(getLastIdSql);
                Integer notificationId = ((Number) lastIdResult.get("last_id")).intValue();

                // 6. 创建响应数据
                NotificationData notification = new NotificationData();
                notification.setId("notif_" + notificationId);
                notification.setType(request.getType());
                notification.setTitle(request.getTitle());
                notification.setMessage(request.getMessage());
                notification.setIcon(request.getIcon());
                notification.setImage(null);
                notification.setActionUrl("/test");
                notification.setTargetType(request.getTargetType());
                notification.setTargetId(request.getTargetId());
                notification.setMetadata(metadata);
                notification.setRead(false);
                notification.setReadAt(null);
                notification.setCreatedAt(formattedNow);
                notification.setRelativeTime(calculateRelativeTime(now));

                // 7. 创建响应
                TestNotificationResponse response = new TestNotificationResponse(
                        true,
                        "测试通知发送成功",
                        notification
                );

                // 打印返回数据
                printResponse(response);

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new TestNotificationResponse(false, "测试通知发送失败", null)
                );
            }

        } catch (Exception e) {
            System.err.println("测试通知发送过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new TestNotificationResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}