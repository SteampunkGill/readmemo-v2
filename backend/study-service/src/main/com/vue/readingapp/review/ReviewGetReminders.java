package com.vue.readingapp.review;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/review")
public class ReviewGetReminders {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到获取复习提醒设置请求 ===");
        System.out.println("请求参数: " + request);
        System.out.println("时间: " + LocalDateTime.now());
        System.out.println("==========================");
    }

    private void printQueryResult(Object result) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("查询结果: " + result);
        System.out.println("===================");
    }

    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    public static class RemindersResponse {
        private boolean success;
        private String message;
        private RemindersData data;

        public RemindersResponse(boolean success, String message, RemindersData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public RemindersData getData() { return data; }
        public void setData(RemindersData data) { this.data = data; }
    }

    public static class RemindersData {
        private boolean enabled;
        private String reminder_time;
        private List<String> reminder_days;
        private boolean sound_enabled;
        private boolean vibration_enabled;
        private String reminder_message;
        private String last_reminder_sent;
        private int snooze_minutes;

        public RemindersData(boolean enabled, String reminder_time, List<String> reminder_days,
                             boolean sound_enabled, boolean vibration_enabled, String reminder_message,
                             String last_reminder_sent, int snooze_minutes) {
            this.enabled = enabled;
            this.reminder_time = reminder_time;
            this.reminder_days = reminder_days;
            this.sound_enabled = sound_enabled;
            this.vibration_enabled = vibration_enabled;
            this.reminder_message = reminder_message;
            this.last_reminder_sent = last_reminder_sent;
            this.snooze_minutes = snooze_minutes;
        }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getReminder_time() { return reminder_time; }
        public void setReminder_time(String reminder_time) { this.reminder_time = reminder_time; }

        public List<String> getReminder_days() { return reminder_days; }
        public void setReminder_days(List<String> reminder_days) { this.reminder_days = reminder_days; }

        public boolean isSound_enabled() { return sound_enabled; }
        public void setSound_enabled(boolean sound_enabled) { this.sound_enabled = sound_enabled; }

        public boolean isVibration_enabled() { return vibration_enabled; }
        public void setVibration_enabled(boolean vibration_enabled) { this.vibration_enabled = vibration_enabled; }

        public String getReminder_message() { return reminder_message; }
        public void setReminder_message(String reminder_message) { this.reminder_message = reminder_message; }

        public String getLast_reminder_sent() { return last_reminder_sent; }
        public void setLast_reminder_sent(String last_reminder_sent) { this.last_reminder_sent = last_reminder_sent; }

        public int getSnooze_minutes() { return snooze_minutes; }
        public void setSnooze_minutes(int snooze_minutes) { this.snooze_minutes = snooze_minutes; }
    }

    @GetMapping("/reminders")
    public ResponseEntity<RemindersResponse> getReviewReminders(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        printRequest("获取复习提醒设置");

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new RemindersResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new RemindersResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) users.get(0).get("user_id");
            System.out.println("当前用户ID: " + userId);

            // 2. 从review_settings表获取提醒设置
            String settingsSql = "SELECT reminder_enabled, reminder_time, reminder_days, " +
                    "sound_enabled, vibration_enabled, reminder_message, " +
                    "last_reminder_sent, snooze_minutes " +
                    "FROM review_settings WHERE user_id = ?";

            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);

            // 3. 如果没有设置，使用默认值
            boolean enabled = true;
            String reminderTime = "18:00";
            List<String> reminderDays = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
            boolean soundEnabled = true;
            boolean vibrationEnabled = true;
            String reminderMessage = "该复习单词啦！";
            String lastReminderSent = LocalDateTime.now().minusHours(2).toString();
            int snoozeMinutes = 15;

            if (!settingsList.isEmpty()) {
                Map<String, Object> settings = settingsList.get(0);

                if (settings.get("reminder_enabled") != null) {
                    enabled = (boolean) settings.get("reminder_enabled");
                }

                if (settings.get("reminder_time") != null) {
                    reminderTime = (String) settings.get("reminder_time");
                }

                if (settings.get("reminder_days") != null) {
                    String daysStr = (String) settings.get("reminder_days");
                    if (daysStr != null && !daysStr.isEmpty()) {
                        reminderDays = Arrays.asList(daysStr.split(","));
                    }
                }

                if (settings.get("sound_enabled") != null) {
                    soundEnabled = (boolean) settings.get("sound_enabled");
                }

                if (settings.get("vibration_enabled") != null) {
                    vibrationEnabled = (boolean) settings.get("vibration_enabled");
                }

                if (settings.get("reminder_message") != null) {
                    reminderMessage = (String) settings.get("reminder_message");
                }

                if (settings.get("last_reminder_sent") != null) {
                    lastReminderSent = settings.get("last_reminder_sent").toString();
                }

                if (settings.get("snooze_minutes") != null) {
                    snoozeMinutes = ((Number) settings.get("snooze_minutes")).intValue();
                }
            }

            // 4. 构建响应数据
            RemindersData data = new RemindersData(
                    enabled,
                    reminderTime,
                    reminderDays,
                    soundEnabled,
                    vibrationEnabled,
                    reminderMessage,
                    lastReminderSent,
                    snoozeMinutes
            );

            RemindersResponse response = new RemindersResponse(true, "获取复习提醒设置成功", data);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取复习提醒设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new RemindersResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
