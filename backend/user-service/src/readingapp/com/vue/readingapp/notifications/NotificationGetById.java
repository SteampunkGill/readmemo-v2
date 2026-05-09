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
public class NotificationGetById {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取单个通知详情请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("============================");
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
    public static class GetNotificationByIdRequest {
        private String notificationId;

        public String getNotificationId() { return notificationId; }
        public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    }

    // 响应DTO
    public static class GetNotificationByIdResponse {
        private boolean success;
        private String message;
        private NotificationData notification;

        public GetNotificationByIdResponse(boolean success, String message, NotificationData notification) {
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

    @GetMapping("/{notificationId}")
    public ResponseEntity<GetNotificationByIdResponse> getNotificationById(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String notificationId) {

        try {
            // 创建请求对象
            GetNotificationByIdRequest request = new GetNotificationByIdRequest();
            request.setNotificationId(notificationId);

            // 打印接收到的请求
            printRequest(request);

            // 1. 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new GetNotificationByIdResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 2. 根据token获取用户ID
            String getUserSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(getUserSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new GetNotificationByIdResponse(false, "登录已过期，请重新登录", null)
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
                        new GetNotificationByIdResponse(false, "通知ID格式错误", null)
                );
            }

            // 4. 查询通知详情
            String querySql = "SELECT notification_id, type, title, message, icon_url, image_url, action_url, target_type, target_id, metadata, is_read, read_at, created_at FROM notifications WHERE notification_id = ? AND user_id = ?";
            List<Map<String, Object>> notifications = jdbcTemplate.queryForList(querySql, notificationIdInt, userId);

            printQueryResult(notifications);

            if (notifications.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new GetNotificationByIdResponse(false, "通知不存在或无权访问", null)
                );
            }

            Map<String, Object> row = notifications.get(0);

            // 5. 转换结果
            NotificationData notification = new NotificationData();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

            // 设置基本字段
            notification.setId("notif_" + row.get("notification_id"));
            notification.setType((String) row.get("type"));
            notification.setTitle((String) row.get("title"));
            notification.setMessage((String) row.get("message"));
            notification.setIcon((String) row.get("icon_url"));
            notification.setImage((String) row.get("image_url"));
            notification.setActionUrl((String) row.get("action_url"));
            notification.setTargetType((String) row.get("target_type"));

            // 处理targetId
            Object targetIdObj = row.get("target_id");
            if (targetIdObj != null) {
                if (targetIdObj instanceof Number) {
                    notification.setTargetId(String.valueOf(((Number) targetIdObj).intValue()));
                } else {
                    notification.setTargetId(targetIdObj.toString());
                }
            }

            // 设置阅读状态
            Boolean isRead = (Boolean) row.get("is_read");
            notification.setRead(isRead != null && isRead);

            // 处理时间字段
            LocalDateTime createdAt = null;
            if (row.get("created_at") != null) {
                if (row.get("created_at") instanceof java.sql.Timestamp) {
                    createdAt = ((java.sql.Timestamp) row.get("created_at")).toLocalDateTime();
                } else if (row.get("created_at") instanceof LocalDateTime) {
                    createdAt = (LocalDateTime) row.get("created_at");
                }

                if (createdAt != null) {
                    notification.setCreatedAt(createdAt.format(formatter) + "Z");
                    notification.setRelativeTime(calculateRelativeTime(createdAt));
                }
            }

            // 处理阅读时间
            LocalDateTime readAt = null;
            if (row.get("read_at") != null) {
                if (row.get("read_at") instanceof java.sql.Timestamp) {
                    readAt = ((java.sql.Timestamp) row.get("read_at")).toLocalDateTime();
                } else if (row.get("read_at") instanceof LocalDateTime) {
                    readAt = (LocalDateTime) row.get("read_at");
                }

                if (readAt != null) {
                    notification.setReadAt(readAt.format(formatter) + "Z");
                }
            }

            // 处理metadata字段
            Object metadataObj = row.get("metadata");
            if (metadataObj != null) {
                try {
                    if (metadataObj instanceof String) {
                        Map<String, Object> metadataMap = objectMapper.readValue((String) metadataObj, Map.class);
                        notification.setMetadata(metadataMap);
                    } else if (metadataObj instanceof Map) {
                        notification.setMetadata((Map<String, Object>) metadataObj);
                    }
                } catch (Exception e) {
                    System.err.println("解析metadata失败: " + e.getMessage());
                    notification.setMetadata(new HashMap<>());
                }
            }

            // 6. 创建响应
            GetNotificationByIdResponse response = new GetNotificationByIdResponse(
                    true,
                    "获取通知详情成功",
                    notification
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取通知详情过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetNotificationByIdResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}