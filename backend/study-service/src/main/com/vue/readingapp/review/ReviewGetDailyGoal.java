package com.vue.readingapp.review;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/review")
public class ReviewGetDailyGoal {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到获取每日目标请求 ===");
        System.out.println("请求参数: " + request);
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

    public static class DailyGoalResponse {
        private boolean success;
        private String message;
        private DailyGoalData data;

        public DailyGoalResponse(boolean success, String message, DailyGoalData data) {
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

    @GetMapping("/daily-goal")
    public ResponseEntity<DailyGoalResponse> getDailyGoal(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        printRequest("获取每日目标");

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DailyGoalResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            printQueryResult(users);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DailyGoalResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) users.get(0).get("user_id");
            System.out.println("当前用户ID: " + userId);

            // 2. 获取每日目标设置
            int targetWords = getTargetWords(userId);

            // 3. 获取连续学习天数
            int streakDays = calculateStreakDays(userId);

            // 4. 获取今日完成情况
            LocalDateTime now = LocalDateTime.now();
            String today = now.toLocalDate().toString();
            int completedToday = getTodayCompleted(userId, today);

            // 5. 计算今日进度
            double todayProgress = targetWords > 0 ? (double) completedToday / targetWords * 100 : 0;

            // 6. 获取平均每日单词数
            double averageDailyWords = getAverageDailyWords(userId);

            // 7. 获取最佳日单词数
            int bestDay = getBestDay(userId);

            // 8. 获取提醒设置
            String reminderTime = getReminderTime(userId);
            boolean isReminderEnabled = isReminderEnabled(userId);

            // 9. 构建响应数据
            DailyGoalData data = new DailyGoalData(
                    targetWords,
                    streakDays,
                    completedToday,
                    Math.round(todayProgress * 100.0) / 100.0,
                    Math.round(averageDailyWords * 100.0) / 100.0,
                    bestDay,
                    reminderTime,
                    isReminderEnabled
            );

            DailyGoalResponse response = new DailyGoalResponse(true, "获取每日目标成功", data);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取每日目标过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DailyGoalResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private int getTargetWords(int userId) {
        try {
            String sql = "SELECT daily_target_words FROM review_settings WHERE user_id = ?";
            List<Map<String, Object>> settings = jdbcTemplate.queryForList(sql, userId);

            if (!settings.isEmpty()) {
                return ((Number) settings.get(0).get("daily_target_words")).intValue();
            }

            return 20; // 默认每日目标

        } catch (Exception e) {
            System.err.println("获取目标单词数失败: " + e.getMessage());
            return 20;
        }
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
                    LocalDate reviewDate = ((java.sql.Date) dateRow.get("review_date")).toLocalDate();
                    if (reviewDate.equals(yesterday)) {
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

    private String getReminderTime(int userId) {
        try {
            String sql = "SELECT reminder_time FROM review_settings WHERE user_id = ?";
            List<Map<String, Object>> settings = jdbcTemplate.queryForList(sql, userId);

            if (!settings.isEmpty() && settings.get(0).get("reminder_time") != null) {
                return (String) settings.get(0).get("reminder_time");
            }

            return "18:00"; // 默认提醒时间

        } catch (Exception e) {
            System.err.println("获取提醒时间失败: " + e.getMessage());
            return "18:00";
        }
    }

    private boolean isReminderEnabled(int userId) {
        try {
            String sql = "SELECT reminder_enabled FROM review_settings WHERE user_id = ?";
            List<Map<String, Object>> settings = jdbcTemplate.queryForList(sql, userId);

            if (!settings.isEmpty() && settings.get(0).get("reminder_enabled") != null) {
                return (boolean) settings.get(0).get("reminder_enabled");
            }

            return true; // 默认启用提醒

        } catch (Exception e) {
            System.err.println("获取提醒启用状态失败: " + e.getMessage());
            return true;
        }
    }
}
