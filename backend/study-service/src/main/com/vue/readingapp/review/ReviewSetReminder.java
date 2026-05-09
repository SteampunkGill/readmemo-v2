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
public class ReviewSetReminder {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到设置复习提醒请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("时间: " + LocalDateTime.now());
        System.out.println("========================");
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

    // 在 ReviewSetReminder.java 中重新定义 RemindersData 类
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

    public static class SetReminderRequest {
        private Boolean enabled;
        private String reminder_time;
        private List<String> reminder_days;
        private Boolean sound_enabled;
        private Boolean vibration_enabled;
        private String reminder_message;
        private Integer snooze_minutes;

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }

        public String getReminder_time() { return reminder_time; }
        public void setReminder_time(String reminder_time) { this.reminder_time = reminder_time; }

        public List<String> getReminder_days() { return reminder_days; }
        public void setReminder_days(List<String> reminder_days) { this.reminder_days = reminder_days; }

        public Boolean getSound_enabled() { return sound_enabled; }
        public void setSound_enabled(Boolean sound_enabled) { this.sound_enabled = sound_enabled; }

        public Boolean getVibration_enabled() { return vibration_enabled; }
        public void setVibration_enabled(Boolean vibration_enabled) { this.vibration_enabled = vibration_enabled; }

        public String getReminder_message() { return reminder_message; }
        public void setReminder_message(String reminder_message) { this.reminder_message = reminder_message; }

        public Integer getSnooze_minutes() { return snooze_minutes; }
        public void setSnooze_minutes(Integer snooze_minutes) { this.snooze_minutes = snooze_minutes; }
    }

    public static class SetReminderResponse {
        private boolean success;
        private String message;
        private RemindersData data;

        public SetReminderResponse(boolean success, String message, RemindersData data) {
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

    @PostMapping("/reminders")
    public ResponseEntity<SetReminderResponse> setReviewReminder(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SetReminderRequest request) {

        printRequest(request);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SetReminderResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SetReminderResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) users.get(0).get("user_id");
            System.out.println("当前用户ID: " + userId);

            // 2. 验证请求数据
            if (request.getReminder_time() != null) {
                // 简单验证时间格式
                String timePattern = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$";
                if (!request.getReminder_time().matches(timePattern)) {
                    return ResponseEntity.badRequest().body(
                            new SetReminderResponse(false, "提醒时间格式无效，请使用HH:mm格式", null)
                    );
                }
            }

            if (request.getSnooze_minutes() != null &&
                    (request.getSnooze_minutes() < 1 || request.getSnooze_minutes() > 60)) {
                return ResponseEntity.badRequest().body(
                        new SetReminderResponse(false, "延迟分钟数必须在1-60之间", null)
                );
            }

            // 3. 检查是否已有设置记录
            String checkSql = "SELECT COUNT(*) FROM review_settings WHERE user_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId);

            LocalDateTime now = LocalDateTime.now();

            if (count > 0) {
                // 更新现有记录
                StringBuilder updateSql = new StringBuilder("UPDATE review_settings SET ");
                List<Object> params = new ArrayList<>();
                List<String> updates = new ArrayList<>();

                if (request.getEnabled() != null) {
                    updates.add("reminder_enabled = ?");
                    params.add(request.getEnabled());
                }

                if (request.getReminder_time() != null) {
                    updates.add("reminder_time = ?");
                    params.add(request.getReminder_time());
                }

                if (request.getReminder_days() != null) {
                    updates.add("reminder_days = ?");
                    params.add(String.join(",", request.getReminder_days()));
                }

                if (request.getSound_enabled() != null) {
                    updates.add("sound_enabled = ?");
                    params.add(request.getSound_enabled());
                }

                if (request.getVibration_enabled() != null) {
                    updates.add("vibration_enabled = ?");
                    params.add(request.getVibration_enabled());
                }

                if (request.getReminder_message() != null) {
                    updates.add("reminder_message = ?");
                    params.add(request.getReminder_message());
                }

                if (request.getSnooze_minutes() != null) {
                    updates.add("snooze_minutes = ?");
                    params.add(request.getSnooze_minutes());
                }

                updates.add("updated_at = ?");
                params.add(now);

                updateSql.append(String.join(", ", updates));
                updateSql.append(" WHERE user_id = ?");
                params.add(userId);

                System.out.println("执行更新SQL: " + updateSql.toString());
                System.out.println("参数: " + params);

                int updated = jdbcTemplate.update(updateSql.toString(), params.toArray());
                printQueryResult("更新记录数: " + updated);

            } else {
                // 创建新记录
                String insertSql = "INSERT INTO review_settings " +
                        "(user_id, reminder_enabled, reminder_time, reminder_days, " +
                        "sound_enabled, vibration_enabled, reminder_message, " +
                        "snooze_minutes, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                boolean enabled = request.getEnabled() != null ? request.getEnabled() : true;
                String reminderTime = request.getReminder_time() != null ? request.getReminder_time() : "18:00";
                String reminderDays = request.getReminder_days() != null ?
                        String.join(",", request.getReminder_days()) : "Monday,Tuesday,Wednesday,Thursday,Friday";
                boolean soundEnabled = request.getSound_enabled() != null ? request.getSound_enabled() : true;
                boolean vibrationEnabled = request.getVibration_enabled() != null ? request.getVibration_enabled() : true;
                String reminderMessage = request.getReminder_message() != null ?
                        request.getReminder_message() : "该复习单词啦！";
                int snoozeMinutes = request.getSnooze_minutes() != null ? request.getSnooze_minutes() : 15;

                int inserted = jdbcTemplate.update(insertSql,
                        userId,
                        enabled,
                        reminderTime,
                        reminderDays,
                        soundEnabled,
                        vibrationEnabled,
                        reminderMessage,
                        snoozeMinutes,
                        now,
                        now
                );

                printQueryResult("插入记录数: " + inserted);
            }

            // 4. 获取更新后的提醒设置
            RemindersData data = getUpdatedRemindersData(userId, request);

            SetReminderResponse response = new SetReminderResponse(true, "设置复习提醒成功", data);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("设置复习提醒过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SetReminderResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private RemindersData getUpdatedRemindersData(int userId, SetReminderRequest request) {
        // 获取当前设置值或使用默认值
        boolean enabled = request.getEnabled() != null ? request.getEnabled() : true;
        String reminderTime = request.getReminder_time() != null ? request.getReminder_time() : "18:00";
        List<String> reminderDays = request.getReminder_days() != null ?
                request.getReminder_days() : Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
        boolean soundEnabled = request.getSound_enabled() != null ? request.getSound_enabled() : true;
        boolean vibrationEnabled = request.getVibration_enabled() != null ? request.getVibration_enabled() : true;
        String reminderMessage = request.getReminder_message() != null ?
                request.getReminder_message() : "该复习单词啦！";
        int snoozeMinutes = request.getSnooze_minutes() != null ? request.getSnooze_minutes() : 15;

        return new RemindersData(
                enabled,
                reminderTime,
                reminderDays,
                soundEnabled,
                vibrationEnabled,
                reminderMessage,
                LocalDateTime.now().minusHours(2).toString(),
                snoozeMinutes
        );
    }
}