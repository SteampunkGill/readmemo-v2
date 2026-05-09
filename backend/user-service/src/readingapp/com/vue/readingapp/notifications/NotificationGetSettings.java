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
public class NotificationGetSettings {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取通知设置请求 ===");
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
    public static class GetSettingsResponse {
        private boolean success;
        private String message;
        private NotificationSettingsData settings;

        public GetSettingsResponse(boolean success, String message, NotificationSettingsData settings) {
            this.success = success;
            this.message = message;
            this.settings = settings;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public NotificationSettingsData getSettings() { return settings; }
        public void setSettings(NotificationSettingsData settings) { this.settings = settings; }
    }

    public static class NotificationSettingsData {
        private boolean email;
        private boolean push;
        private boolean desktop;
        private String frequency;
        private QuietHoursData quietHours;
        private NotificationTypesData types;
        private String updatedAt;

        public NotificationSettingsData() {
            this.email = true;
            this.push = true;
            this.desktop = true;
            this.frequency = "immediate";
            this.quietHours = new QuietHoursData();
            this.types = new NotificationTypesData();
        }

        public boolean isEmail() { return email; }
        public void setEmail(boolean email) { this.email = email; }

        public boolean isPush() { return push; }
        public void setPush(boolean push) { this.push = push; }

        public boolean isDesktop() { return desktop; }
        public void setDesktop(boolean desktop) { this.desktop = desktop; }

        public String getFrequency() { return frequency; }
        public void setFrequency(String frequency) { this.frequency = frequency; }

        public QuietHoursData getQuietHours() { return quietHours; }
        public void setQuietHours(QuietHoursData quietHours) { this.quietHours = quietHours; }

        public NotificationTypesData getTypes() { return types; }
        public void setTypes(NotificationTypesData types) { this.types = types; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class QuietHoursData {
        private boolean enabled;
        private String start;
        private String end;

        public QuietHoursData() {
            this.enabled = true;
            this.start = "22:00";
            this.end = "08:00";
        }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }

        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }

    public static class NotificationTypesData {
        private boolean document_shared;
        private boolean review_reminder;
        private boolean achievement_unlocked;
        private boolean system_update;
        private boolean promotional;

        public NotificationTypesData() {
            this.document_shared = true;
            this.review_reminder = true;
            this.achievement_unlocked = true;
            this.system_update = true;
            this.promotional = false;
        }

        public boolean isDocument_shared() { return document_shared; }
        public void setDocument_shared(boolean document_shared) { this.document_shared = document_shared; }

        public boolean isReview_reminder() { return review_reminder; }
        public void setReview_reminder(boolean review_reminder) { this.review_reminder = review_reminder; }

        public boolean isAchievement_unlocked() { return achievement_unlocked; }
        public void setAchievement_unlocked(boolean achievement_unlocked) { this.achievement_unlocked = achievement_unlocked; }

        public boolean isSystem_update() { return system_update; }
        public void setSystem_update(boolean system_update) { this.system_update = system_update; }

        public boolean isPromotional() { return promotional; }
        public void setPromotional(boolean promotional) { this.promotional = promotional; }
    }

    @GetMapping("/settings")
    public ResponseEntity<GetSettingsResponse> getSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest("获取通知设置");

            // 1. 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new GetSettingsResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 2. 根据token获取用户ID
            String getUserSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(getUserSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new GetSettingsResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            Integer userId = (Integer) session.get("user_id");

            // 3. 查询通知设置
            String querySql = "SELECT email_notifications, push_notifications, desktop_notifications, notification_frequency, quiet_hours_enabled, quiet_hours_start, quiet_hours_end, notification_types, updated_at FROM notification_settings WHERE user_id = ?";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(querySql, userId);

            printQueryResult(settingsList);

            // 4. 创建设置数据
            NotificationSettingsData settings = new NotificationSettingsData();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

            if (!settingsList.isEmpty()) {
                Map<String, Object> row = settingsList.get(0);

                // 设置基本字段
                settings.setEmail(row.get("email_notifications") != null && (Boolean) row.get("email_notifications"));
                settings.setPush(row.get("push_notifications") != null && (Boolean) row.get("push_notifications"));
                settings.setDesktop(row.get("desktop_notifications") != null && (Boolean) row.get("desktop_notifications"));

                if (row.get("notification_frequency") != null) {
                    settings.setFrequency(row.get("notification_frequency").toString());
                }

                // 设置静默时间
                QuietHoursData quietHours = new QuietHoursData();
                quietHours.setEnabled(row.get("quiet_hours_enabled") != null && (Boolean) row.get("quiet_hours_enabled"));

                if (row.get("quiet_hours_start") != null) {
                    quietHours.setStart(row.get("quiet_hours_start").toString());
                }

                if (row.get("quiet_hours_end") != null) {
                    quietHours.setEnd(row.get("quiet_hours_end").toString());
                }

                settings.setQuietHours(quietHours);

                // 设置通知类型
                NotificationTypesData types = new NotificationTypesData();
                if (row.get("notification_types") != null) {
                    try {
                        String typesJson = row.get("notification_types").toString();
                        // 简单解析JSON字符串，这里简化处理
                        if (typesJson.contains("document_shared")) {
                            types.setDocument_shared(typesJson.contains("\"document_shared\":true"));
                        }
                        if (typesJson.contains("review_reminder")) {
                            types.setReview_reminder(typesJson.contains("\"review_reminder\":true"));
                        }
                        if (typesJson.contains("achievement_unlocked")) {
                            types.setAchievement_unlocked(typesJson.contains("\"achievement_unlocked\":true"));
                        }
                        if (typesJson.contains("system_update")) {
                            types.setSystem_update(typesJson.contains("\"system_update\":true"));
                        }
                        if (typesJson.contains("promotional")) {
                            types.setPromotional(typesJson.contains("\"promotional\":true"));
                        }
                    } catch (Exception e) {
                        System.err.println("解析通知类型失败: " + e.getMessage());
                    }
                }

                settings.setTypes(types);

                // 设置更新时间
                if (row.get("updated_at") != null) {
                    LocalDateTime updatedAt = null;
                    if (row.get("updated_at") instanceof java.sql.Timestamp) {
                        updatedAt = ((java.sql.Timestamp) row.get("updated_at")).toLocalDateTime();
                    } else if (row.get("updated_at") instanceof LocalDateTime) {
                        updatedAt = (LocalDateTime) row.get("updated_at");
                    }

                    if (updatedAt != null) {
                        settings.setUpdatedAt(updatedAt.format(formatter) + "Z");
                    }
                }
            } else {
                // 如果没有设置记录，使用默认值
                settings.setUpdatedAt(LocalDateTime.now().format(formatter) + "Z");
            }

            // 5. 创建响应
            GetSettingsResponse response = new GetSettingsResponse(
                    true,
                    "获取通知设置成功",
                    settings
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取通知设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetSettingsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}