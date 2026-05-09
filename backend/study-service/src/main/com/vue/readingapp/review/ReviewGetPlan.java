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
public class ReviewGetPlan {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到获取复习计划请求 ===");
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

    public static class ReviewPlanResponse {
        private boolean success;
        private String message;
        private ReviewPlanData data;

        public ReviewPlanResponse(boolean success, String message, ReviewPlanData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ReviewPlanData getData() { return data; }
        public void setData(ReviewPlanData data) { this.data = data; }
    }

    public static class ReviewPlanData {
        private String plan_id;
        private String user_id;
        private String plan_name;
        private String description;
        private int daily_target_words;
        private int weekly_target_days;
        private String preferred_time;
        private List<String> preferred_days;
        private String language_focus;
        private String difficulty_level;
        private boolean is_active;
        private String created_at;
        private String updated_at;
        private PlanProgress progress;

        public ReviewPlanData(String plan_id, String user_id, String plan_name, String description,
                              int daily_target_words, int weekly_target_days, String preferred_time,
                              List<String> preferred_days, String language_focus, String difficulty_level,
                              boolean is_active, String created_at, String updated_at, PlanProgress progress) {
            this.plan_id = plan_id;
            this.user_id = user_id;
            this.plan_name = plan_name;
            this.description = description;
            this.daily_target_words = daily_target_words;
            this.weekly_target_days = weekly_target_days;
            this.preferred_time = preferred_time;
            this.preferred_days = preferred_days;
            this.language_focus = language_focus;
            this.difficulty_level = difficulty_level;
            this.is_active = is_active;
            this.created_at = created_at;
            this.updated_at = updated_at;
            this.progress = progress;
        }

        public String getPlan_id() { return plan_id; }
        public void setPlan_id(String plan_id) { this.plan_id = plan_id; }

        public String getUser_id() { return user_id; }
        public void setUser_id(String user_id) { this.user_id = user_id; }

        public String getPlan_name() { return plan_name; }
        public void setPlan_name(String plan_name) { this.plan_name = plan_name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public int getDaily_target_words() { return daily_target_words; }
        public void setDaily_target_words(int daily_target_words) { this.daily_target_words = daily_target_words; }

        public int getWeekly_target_days() { return weekly_target_days; }
        public void setWeekly_target_days(int weekly_target_days) { this.weekly_target_days = weekly_target_days; }

        public String getPreferred_time() { return preferred_time; }
        public void setPreferred_time(String preferred_time) { this.preferred_time = preferred_time; }

        public List<String> getPreferred_days() { return preferred_days; }
        public void setPreferred_days(List<String> preferred_days) { this.preferred_days = preferred_days; }

        public String getLanguage_focus() { return language_focus; }
        public void setLanguage_focus(String language_focus) { this.language_focus = language_focus; }

        public String getDifficulty_level() { return difficulty_level; }
        public void setDifficulty_level(String difficulty_level) { this.difficulty_level = difficulty_level; }

        public boolean isIs_active() { return is_active; }
        public void setIs_active(boolean is_active) { this.is_active = is_active; }

        public String getCreated_at() { return created_at; }
        public void setCreated_at(String created_at) { this.created_at = created_at; }

        public String getUpdated_at() { return updated_at; }
        public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }

        public PlanProgress getProgress() { return progress; }
        public void setProgress(PlanProgress progress) { this.progress = progress; }
    }

    public static class PlanProgress {
        private int today_completed;
        private int today_target;
        private double today_percentage;
        private int week_completed;
        private int week_target;
        private double week_percentage;
        private int streak_days;
        private int total_words_reviewed;
        private double average_accuracy;

        public PlanProgress(int today_completed, int today_target, double today_percentage,
                            int week_completed, int week_target, double week_percentage,
                            int streak_days, int total_words_reviewed, double average_accuracy) {
            this.today_completed = today_completed;
            this.today_target = today_target;
            this.today_percentage = today_percentage;
            this.week_completed = week_completed;
            this.week_target = week_target;
            this.week_percentage = week_percentage;
            this.streak_days = streak_days;
            this.total_words_reviewed = total_words_reviewed;
            this.average_accuracy = average_accuracy;
        }

        public int getToday_completed() { return today_completed; }
        public void setToday_completed(int today_completed) { this.today_completed = today_completed; }

        public int getToday_target() { return today_target; }
        public void setToday_target(int today_target) { this.today_target = today_target; }

        public double getToday_percentage() { return today_percentage; }
        public void setToday_percentage(double today_percentage) { this.today_percentage = today_percentage; }

        public int getWeek_completed() { return week_completed; }
        public void setWeek_completed(int week_completed) { this.week_completed = week_completed; }

        public int getWeek_target() { return week_target; }
        public void setWeek_target(int week_target) { this.week_target = week_target; }

        public double getWeek_percentage() { return week_percentage; }
        public void setWeek_percentage(double week_percentage) { this.week_percentage = week_percentage; }

        public int getStreak_days() { return streak_days; }
        public void setStreak_days(int streak_days) { this.streak_days = streak_days; }

        public int getTotal_words_reviewed() { return total_words_reviewed; }
        public void setTotal_words_reviewed(int total_words_reviewed) { this.total_words_reviewed = total_words_reviewed; }

        public double getAverage_accuracy() { return average_accuracy; }
        public void setAverage_accuracy(double average_accuracy) { this.average_accuracy = average_accuracy; }
    }

    @GetMapping("/plan")
    public ResponseEntity<ReviewPlanResponse> getReviewPlan(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        printRequest("获取复习计划");

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReviewPlanResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReviewPlanResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) users.get(0).get("user_id");
            System.out.println("当前用户ID: " + userId);

            // 2. 获取用户的复习设置（从review_settings表）
            String settingsSql = "SELECT * FROM review_settings WHERE user_id = ?";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);

            // 3. 获取今日进度
            LocalDateTime now = LocalDateTime.now();
            String today = now.toLocalDate().toString();

            // 获取今日已复习单词数
            String todayReviewsSql = "SELECT COALESCE(SUM(total_words), 0) as today_words " +
                    "FROM review_sessions " +
                    "WHERE user_id = ? AND DATE(completed_at) = ?";

            int todayCompleted = jdbcTemplate.queryForObject(todayReviewsSql, Integer.class, userId, today);

            // 4. 获取本周进度
            LocalDateTime weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1);
            String weekStartStr = weekStart.toLocalDate().toString();

            String weekReviewsSql = "SELECT COALESCE(SUM(total_words), 0) as week_words " +
                    "FROM review_sessions " +
                    "WHERE user_id = ? AND DATE(completed_at) BETWEEN ? AND ?";

            int weekCompleted = jdbcTemplate.queryForObject(weekReviewsSql, Integer.class,
                    userId, weekStartStr, today);

            // 5. 获取连续学习天数
            int streakDays = calculateStreakDays(userId);

            // 6. 获取总复习单词数和平均准确率
            String totalStatsSql = "SELECT COALESCE(SUM(total_words), 0) as total_words, " +
                    "COALESCE(AVG(accuracy), 0) as avg_accuracy " +
                    "FROM review_sessions WHERE user_id = ?";

            Map<String, Object> totalStats = jdbcTemplate.queryForMap(totalStatsSql, userId);
            int totalWordsReviewed = ((Number) totalStats.get("total_words")).intValue();
            double averageAccuracy = ((Number) totalStats.get("avg_accuracy")).doubleValue();

            // 7. 构建复习计划数据
            // 如果没有设置，使用默认值
            int dailyTargetWords = 20;
            int weeklyTargetDays = 5;
            String preferredTime = "18:00";
            List<String> preferredDays = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
            String languageFocus = "en";
            String difficultyLevel = "medium";

            if (!settingsList.isEmpty()) {
                Map<String, Object> settings = settingsList.get(0);
                dailyTargetWords = settings.get("daily_target_words") != null ?
                        ((Number) settings.get("daily_target_words")).intValue() : dailyTargetWords;
                weeklyTargetDays = settings.get("weekly_target_days") != null ?
                        ((Number) settings.get("weekly_target_days")).intValue() : weeklyTargetDays;
                preferredTime = settings.get("preferred_time") != null ?
                        (String) settings.get("preferred_time") : preferredTime;

                // 解析偏好日期（假设以逗号分隔）
                if (settings.get("preferred_days") != null) {
                    String daysStr = (String) settings.get("preferred_days");
                    preferredDays = Arrays.asList(daysStr.split(","));
                }

                languageFocus = settings.get("language_focus") != null ?
                        (String) settings.get("language_focus") : languageFocus;
                difficultyLevel = settings.get("difficulty_level") != null ?
                        (String) settings.get("difficulty_level") : difficultyLevel;
            }

            // 计算百分比
            double todayPercentage = dailyTargetWords > 0 ?
                    (double) todayCompleted / dailyTargetWords * 100 : 0;
            double weekPercentage = weeklyTargetDays > 0 ?
                    (double) weekCompleted / (dailyTargetWords * weeklyTargetDays) * 100 : 0;

            // 8. 构建响应数据
            PlanProgress progress = new PlanProgress(
                    todayCompleted,
                    dailyTargetWords,
                    Math.round(todayPercentage * 100.0) / 100.0,
                    weekCompleted,
                    dailyTargetWords * weeklyTargetDays,
                    Math.round(weekPercentage * 100.0) / 100.0,
                    streakDays,
                    totalWordsReviewed,
                    Math.round(averageAccuracy * 100.0) / 100.0
            );

            ReviewPlanData planData = new ReviewPlanData(
                    "plan_" + userId,
                    String.valueOf(userId),
                    "默认复习计划",
                    "每日复习" + dailyTargetWords + "个单词，每周学习" + weeklyTargetDays + "天",
                    dailyTargetWords,
                    weeklyTargetDays,
                    preferredTime,
                    preferredDays,
                    languageFocus,
                    difficultyLevel,
                    true,
                    now.minusDays(30).toString(),
                    now.toString(),
                    progress
            );

            ReviewPlanResponse response = new ReviewPlanResponse(true, "获取复习计划成功", planData);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取复习计划过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReviewPlanResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
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
}
