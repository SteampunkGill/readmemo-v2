package com.vue.readingapp.review;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/review")
public class ReviewUpdateDailyGoal {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到更新每日目标请求 ===");
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

    // 在 ReviewUpdateDailyGoal.java 中重新定义 DailyGoalData 类
    public static class DailyGoalData {
        private int target_words;
        private int streak_days;
        private int completed_today;
        private double today_progress;
        private double average_daily_words;
        private int best_day;
        private String reminder_time;
        private boolean is_reminder_enabled;

        public DailyGoalData(int target_words, int streak_days, int completed_today,
                             double today_progress, double average_daily_words, int best_day,
                             String reminder_time, boolean is_reminder_enabled) {
            this.target_words = target_words;
            this.streak_days = streak_days;
            this.completed_today = completed_today;
            this.today_progress = today_progress;
            this.average_daily_words = average_daily_words;
            this.best_day = best_day;
            this.reminder_time = reminder_time;
            this.is_reminder_enabled = is_reminder_enabled;
        }

        public int getTarget_words() { return target_words; }
        public void setTarget_words(int target_words) { this.target_words = target_words; }

        public int getStreak_days() { return streak_days; }
        public void setStreak_days(int streak_days) { this.streak_days = streak_days; }

        public int getCompleted_today() { return completed_today; }
        public void setCompleted_today(int completed_today) { this.completed_today = completed_today; }

        public double getToday_progress() { return today_progress; }
        public void setToday_progress(double today_progress) { this.today_progress = today_progress; }

        public double getAverage_daily_words() { return average_daily_words; }
        public void setAverage_daily_words(double average_daily_words) { this.average_daily_words = average_daily_words; }

        public int getBest_day() { return best_day; }
        public void setBest_day(int best_day) { this.best_day = best_day; }

        public String getReminder_time() { return reminder_time; }
        public void setReminder_time(String reminder_time) { this.reminder_time = reminder_time; }

        public boolean isIs_reminder_enabled() { return is_reminder_enabled; }
        public void setIs_reminder_enabled(boolean is_reminder_enabled) { this.is_reminder_enabled = is_reminder_enabled; }
    }

    public static class UpdateDailyGoalRequest {
        private Integer target_words;
        private String reminder_time;
        private Boolean is_reminder_enabled;

        public Integer getTarget_words() { return target_words; }
        public void setTarget_words(Integer target_words) { this.target_words = target_words; }

        public String getReminder_time() { return reminder_time; }
        public void setReminder_time(String reminder_time) { this.reminder_time = reminder_time; }

        public Boolean getIs_reminder_enabled() { return is_reminder_enabled; }
        public void setIs_reminder_enabled(Boolean is_reminder_enabled) { this.is_reminder_enabled = is_reminder_enabled; }
    }

    public static class UpdateDailyGoalResponse {
        private boolean success;
        private String message;
        private DailyGoalData data;

        public UpdateDailyGoalResponse(boolean success, String message, DailyGoalData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public DailyGoalData getData() { return data; }
        public void setData(DailyGoalData data) { this.data = data; }
    }

    @PutMapping("/daily-goal")
    public ResponseEntity<UpdateDailyGoalResponse> updateDailyGoal(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateDailyGoalRequest request) {

        printRequest(request);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateDailyGoalResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateDailyGoalResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) users.get(0).get("user_id");
            System.out.println("当前用户ID: " + userId);

            // 2. 验证请求数据
            if (request.getTarget_words() != null && request.getTarget_words() < 1) {
                return ResponseEntity.badRequest().body(
                        new UpdateDailyGoalResponse(false, "目标单词数必须大于0", null)
                );
            }

            if (request.getTarget_words() != null && request.getTarget_words() > 1000) {
                return ResponseEntity.badRequest().body(
                        new UpdateDailyGoalResponse(false, "目标单词数不能超过1000", null)
                );
            }

            if (request.getReminder_time() != null) {
                // 简单验证时间格式
                String timePattern = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$";
                if (!request.getReminder_time().matches(timePattern)) {
                    return ResponseEntity.badRequest().body(
                            new UpdateDailyGoalResponse(false, "提醒时间格式无效，请使用HH:mm格式", null)
                    );
                }
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

                if (request.getTarget_words() != null) {
                    updates.add("daily_target_words = ?");
                    params.add(request.getTarget_words());
                }

                if (request.getReminder_time() != null) {
                    updates.add("reminder_time = ?");
                    params.add(request.getReminder_time());
                }

                if (request.getIs_reminder_enabled() != null) {
                    updates.add("reminder_enabled = ?");
                    params.add(request.getIs_reminder_enabled());
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
                        "(user_id, daily_target_words, reminder_time, " +
                        "reminder_enabled, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

                int targetWords = request.getTarget_words() != null ? request.getTarget_words() : 20;
                String reminderTime = request.getReminder_time() != null ? request.getReminder_time() : "18:00";
                boolean isReminderEnabled = request.getIs_reminder_enabled() != null ?
                        request.getIs_reminder_enabled() : true;

                int inserted = jdbcTemplate.update(insertSql,
                        userId,
                        targetWords,
                        reminderTime,
                        isReminderEnabled,
                        now,
                        now
                );

                printQueryResult("插入记录数: " + inserted);
            }

            // 4. 获取更新后的每日目标数据
            DailyGoalData data = getUpdatedDailyGoalData(userId, request);

            UpdateDailyGoalResponse response = new UpdateDailyGoalResponse(true, "更新每日目标成功", data);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新每日目标过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateDailyGoalResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private DailyGoalData getUpdatedDailyGoalData(int userId, UpdateDailyGoalRequest request) {
        // 获取当前设置值或使用默认值
        int targetWords = request.getTarget_words() != null ? request.getTarget_words() : 20;
        String reminderTime = request.getReminder_time() != null ? request.getReminder_time() : "18:00";
        boolean isReminderEnabled = request.getIs_reminder_enabled() != null ?
                request.getIs_reminder_enabled() : true;

        // 获取连续学习天数
        int streakDays = calculateStreakDays(userId);

        // 获取今日完成情况
        LocalDateTime now = LocalDateTime.now();
        String today = now.toLocalDate().toString();
        int completedToday = getTodayCompleted(userId, today);

        // 计算今日进度
        double todayProgress = targetWords > 0 ? (double) completedToday / targetWords * 100 : 0;

        // 获取平均每日单词数
        double averageDailyWords = getAverageDailyWords(userId);

        // 获取最佳日单词数
        int bestDay = getBestDay(userId);

        return new DailyGoalData(
                targetWords,
                streakDays,
                completedToday,
                Math.round(todayProgress * 100.0) / 100.0,
                Math.round(averageDailyWords * 100.0) / 100.0,
                bestDay,
                reminderTime,
                isReminderEnabled
        );
    }

    private int calculateStreakDays(int userId) {
        try {
            // 获取用户有复习记录的所有日期
            String sql = "SELECT DISTINCT DATE(completed_at) as review_date " +
                    "FROM review_sessions " +
                    "WHERE user_id = ? " +
                    "ORDER BY review_date DESC";

            List<Map<String, Object>> dates = jdbcTemplate.queryForList(sql, userId);

            if (dates.isEmpty()) {
                return 0;
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDate today = now.toLocalDate();
            LocalDate yesterday = today.minusDays(1);
            int streak = 0;

            // 检查今天是否有复习
            boolean todayReviewed = false;
            for (Map<String, Object> dateRow : dates) {
                LocalDate reviewDate = ((java.sql.Date) dateRow.get("review_date")).toLocalDate();
                if (reviewDate.equals(today)) {
                    todayReviewed = true;
                    streak = 1;
                    break;
                }
            }

            // 如果今天没有复习，检查昨天是否有
            if (!todayReviewed) {
                for (Map<String, Object> dateRow : dates) {
                    LocalDate reviewAddress = ((java.sql.Date) dateRow.get("review_date")).toLocalDate();
                    if (reviewAddress.equals(yesterday)) {
                        streak = 1;
                        break;
                    }
                }
            }

            // 计算连续天数
            if (streak > 0) {
                LocalDate expectedDate = todayReviewed ? today.minusDays(1) : yesterday.minusDays(1);

                for (Map<String, Object> dateRow : dates) {
                    LocalDate reviewDate = ((java.sql.Date) dateRow.get("review_date")).toLocalDate();

                    if (reviewDate.equals(expectedDate)) {
                        streak++;
                        expectedDate = expectedDate.minusDays(1);
                    } else if (reviewDate.isBefore(expectedDate)) {
                        // 跳过中间缺失的日期
                        expectedDate = reviewDate.minusDays(1);
                    }
                }
            }

            return streak;

        } catch (Exception e) {
            System.err.println("计算连续学习天数失败: " + e.getMessage());
            return 0;
        }
    }

    private int getTodayCompleted(int userId, String today) {
        try {
            String sql = "SELECT COALESCE(SUM(total_words), 0) as today_words " +
                    "FROM review_sessions " +
                    "WHERE user_id = ? AND DATE(completed_at) = ?";

            return jdbcTemplate.queryForObject(sql, Integer.class, userId, today);

        } catch (Exception e) {
            System.err.println("获取今日完成情况失败: " + e.getMessage());
            return 0;
        }
    }

    private double getAverageDailyWords(int userId) {
        try {
            // 获取总单词数和学习天数
            String sql = "SELECT " +
                    "COALESCE(SUM(total_words), 0) as total_words, " +
                    "COUNT(DISTINCT DATE(completed_at)) as days_count " +
                    "FROM review_sessions WHERE user_id = ?";

            Map<String, Object> stats = jdbcTemplate.queryForMap(sql, userId);
            int totalWords = ((Number) stats.get("total_words")).intValue();
            int daysCount = ((Number) stats.get("days_count")).intValue();

            return daysCount > 0 ? (double) totalWords / daysCount : 0;

        } catch (Exception e) {
            System.err.println("获取平均每日单词数失败: " + e.getMessage());
            return 0;
        }
    }

    private int getBestDay(int userId) {
        try {
            String sql = "SELECT COALESCE(MAX(total_words), 0) as best_day " +
                    "FROM review_sessions WHERE user_id = ?";

            return jdbcTemplate.queryForObject(sql, Integer.class, userId);

        } catch (Exception e) {
            System.err.println("获取最佳日单词数失败: " + e.getMessage());
            return 0;
        }
    }
}