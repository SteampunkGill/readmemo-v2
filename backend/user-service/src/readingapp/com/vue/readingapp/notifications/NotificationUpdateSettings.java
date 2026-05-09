package com.vue.readingapp.notifications;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationUpdateSettings {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到更新通知设置请求 ===");
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
    public static class UpdateSettingsRequest {
        private Boolean email;
        private Boolean push;
        private Boolean desktop;
        private String frequency;
        private QuietHoursData quietHours;
        private NotificationTypesData types;

        public Boolean getEmail() { return email; }
        public void setEmail(Boolean email) { this.email = email; }

        public Boolean getPush() { return push; }
        public void setPush(Boolean push) { this.push = push; }

        public Boolean getDesktop() { return desktop; }
        public void setDesktop(Boolean desktop) { this.desktop = desktop; }

        public String getFrequency() { return frequency; }
        public void setFrequency(String frequency) { this.frequency = frequency; }

        public QuietHoursData getQuietHours() { return quietHours; }
        public void setQuietHours(QuietHoursData quietHours) { this.quietHours = quietHours; }

        public NotificationTypesData getTypes() { return types; }
        public void setTypes(NotificationTypesData types) { this.types = types; }
    }

    public static class QuietHoursData {
        private Boolean enabled;
        private String start;
        private String end;

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }

        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }

    public static class NotificationTypesData {
        private Boolean document_shared;
        private Boolean review_reminder;
        private Boolean achievement_unlocked;
        private Boolean system_update;
        private Boolean promotional;

        public Boolean getDocument_shared() { return document_shared; }
        public void setDocument_shared(Boolean document_shared) { this.document_shared = document_shared; }

        public Boolean getReview_reminder() { return review_reminder; }
        public void setReview_reminder(Boolean review_reminder) { this.review_reminder = review_reminder; }

        public Boolean getAchievement_unlocked() { return achievement_unlocked; }
        public void setAchievement_unlocked(Boolean achievement_unlocked) { this.achievement_unlocked = achievement_unlocked; }

        public Boolean getSystem_update() { return system_update; }
        public void setSystem_update(Boolean system_update) { this.system_update = system_update; }

        public Boolean getPromotional() { return promotional; }
        public void setPromotional(Boolean promotional) { this.promotional = promotional; }
    }

    // 响应DTO
    public static class UpdateSettingsResponse {
        private boolean success;
        private String message;
        private NotificationSettingsData settings;

        public UpdateSettingsResponse(boolean success, String message, NotificationSettingsData settings) {
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
        private QuietHoursResponseData quietHours;
        private NotificationTypesResponseData types;
        private String updatedAt;

        public boolean isEmail() { return email; }
        public void setEmail(boolean email) { this.email = email; }

        public boolean isPush() { return push; }
        public void setPush(boolean push) { this.push = push; }

        public boolean isDesktop() { return desktop; }
        public void setDesktop(boolean desktop) { this.desktop = desktop; }

        public String getFrequency() { return frequency; }
        public void setFrequency(String frequency) { this.frequency = frequency; }

        public QuietHoursResponseData getQuietHours() { return quietHours; }
        public void setQuietHours(QuietHoursResponseData quietHours) { this.quietHours = quietHours; }

        public NotificationTypesResponseData getTypes() { return types; }
        public void setTypes(NotificationTypesResponseData types) { this.types = types; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class QuietHoursResponseData {
        private boolean enabled;
        private String start;
        private String end;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }

        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }

    public static class NotificationTypesResponseData {
        private boolean document_shared;
        private boolean review_reminder;
        private boolean achievement_unlocked;
        private boolean system_update;
        private boolean promotional;

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

    @PutMapping("/settings")
    public ResponseEntity<UpdateSettingsResponse> updateSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateSettingsRequest request) {

        try {
            // 打印接收到的请求
            printRequest(request);

            // 1. 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateSettingsResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 2. 根据token获取用户ID
            String getUserSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(getUserSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateSettingsResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            Integer userId = (Integer) session.get("user_id");

            // 3. 准备更新数据
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

            // 检查是否已存在设置
            String checkSql = "SELECT COUNT(*) as count FROM notification_settings WHERE user_id = ?";
            Map<String, Object> checkResult = jdbcTemplate.queryForMap(checkSql, userId);
            long count = ((Number) checkResult.get("count")).longValue();

            // 准备参数
            boolean email = request.getEmail() != null ? request.getEmail() : true;
            boolean push = request.getPush() != null ? request.getPush() : true;
            boolean desktop = request.getDesktop() != null ? request.getDesktop() : true;
            String frequency = request.getFrequency() != null ? request.getFrequency() : "immediate";

            // 处理静默时间
            boolean quietHoursEnabled = true;
            String quietHoursStart = "22:00";
            String quietHoursEnd = "08:00";

            if (request.getQuietHours() != null) {
                if (request.getQuietHours().getEnabled() != null) {
                    quietHoursEnabled = request.getQuietHours().getEnabled();
                }
                if (request.getQuietHours().getStart() != null) {
                    quietHoursStart = request.getQuietHours().getStart();
                }
                if (request.getQuietHours().getEnd() != null) {
                    quietHoursEnd = request.getQuietHours().getEnd();
                }
            }

            // 处理通知类型
            Map<String, Boolean> notificationTypes = new HashMap<>();
            notificationTypes.put("document_shared", true);
            notificationTypes.put("review_reminder", true);
            notificationTypes.put("achievement_unlocked", true);
            notificationTypes.put("system_update", true);
            notificationTypes.put("promotional", false);

            if (request.getTypes() != null) {
                if (request.getTypes().getDocument_shared() != null) {
                    notificationTypes.put("document_shared", request.getTypes().getDocument_shared());
                }
                if (request.getTypes().getReview_reminder() != null) {
                    notificationTypes.put("review_reminder", request.getTypes().getReview_reminder());
                }
                if (request.getTypes().getAchievement_unlocked() != null) {
                    notificationTypes.put("achievement_unlocked", request.getTypes().getAchievement_unlocked());
                }
                if (request.getTypes().getSystem_update() != null) {
                    notificationTypes.put("system_update", request.getTypes().getSystem_update());
                }
                if (request.getTypes().getPromotional() != null) {
                    notificationTypes.put("promotional", request.getTypes().getPromotional());
                }
            }

            String notificationTypesJson = objectMapper.writeValueAsString(notificationTypes);

            int updatedRows;

            if (count > 0) {
                // 更新现有设置
                String updateSql = "UPDATE notification_settings SET email_notifications = ?, push_notifications = ?, desktop_notifications = ?, notification_frequency = ?, quiet_hours_enabled = ?, quiet_hours_start = ?, quiet_hours_end = ?, notification_types = ?, updated_at = ? WHERE user_id = ?";
                updatedRows = jdbcTemplate.update(updateSql,
                        email,
                        push,
                        desktop,
                        frequency,
                        quietHoursEnabled,
                        quietHoursStart,
                        quietHoursEnd,
                        notificationTypesJson,
                        now,
                        userId
                );
            } else {
                // 插入新设置
                String insertSql = "INSERT INTO notification_settings (user_id, email_notifications, push_notifications, desktop_notifications, notification_frequency, quiet_hours_enabled, quiet_hours_start, quiet_hours_end, notification_types, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                updatedRows = jdbcTemplate.update(insertSql,
                        userId,
                        email,
                        push,
                        desktop,
                        frequency,
                        quietHoursEnabled,
                        quietHoursStart,
                        quietHoursEnd,
                        notificationTypesJson,
                        now,
                        now
                );
            }

            printQueryResult("更新行数: " + updatedRows);

            if (updatedRows > 0) {
                // 4. 创建响应数据
                NotificationSettingsData settings = new NotificationSettingsData();
                settings.setEmail(email);
                settings.setPush(push);
                settings.setDesktop(desktop);
                settings.setFrequency(frequency);
                settings.setUpdatedAt(now.format(formatter) + "Z");

                QuietHoursResponseData quietHoursResp = new QuietHoursResponseData();
                quietHoursResp.setEnabled(quietHoursEnabled);
                quietHoursResp.setStart(quietHoursStart);
                quietHoursResp.setEnd(quietHoursEnd);
                settings.setQuietHours(quietHoursResp);

                NotificationTypesResponseData typesResp = new NotificationTypesResponseData();
                typesResp.setDocument_shared(notificationTypes.get("document_shared"));
                typesResp.setReview_reminder(notificationTypes.get("review_reminder"));
                typesResp.setAchievement_unlocked(notificationTypes.get("achievement_unlocked"));
                typesResp.setSystem_update(notificationTypes.get("system_update"));
                typesResp.setPromotional(notificationTypes.get("promotional"));
                settings.setTypes(typesResp);

                // 5. 创建响应
                UpdateSettingsResponse response = new UpdateSettingsResponse(
                        true,
                        "更新通知设置成功",
                        settings
                );

                // 打印返回数据
                printResponse(response);

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new UpdateSettingsResponse(false, "更新通知设置失败", null)
                );
            }

        } catch (Exception e) {
            System.err.println("更新通知设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateSettingsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}