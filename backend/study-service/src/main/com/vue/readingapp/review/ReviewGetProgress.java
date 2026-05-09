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
public class ReviewGetProgress {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到获取复习进度请求 ===");
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

    public static class ReviewProgressResponse {
        private boolean success;
        private String message;
        private ProgressData data;

        public ReviewProgressResponse(boolean success, String message, ProgressData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ProgressData getData() { return data; }
        public void setData(ProgressData data) { this.data = data; }
    }

    public static class ProgressData {
        private int daily_goal;
        private int daily_completed;
        private double daily_progress;
        private int weekly_goal;
        private int weekly_completed;
        private double weekly_progress;
        private int monthly_goal;
        private int monthly_completed;
        private double monthly_progress;
        private String next_milestone;
        private double milestone_progress;

        public ProgressData(int daily_goal, int daily_completed, double daily_progress,
                            int weekly_goal, int weekly_completed, double weekly_progress,
                            int monthly_goal, int monthly_completed, double monthly_progress,
                            String next_milestone, double milestone_progress) {
            this.daily_goal = daily_goal;
            this.daily_completed = daily_completed;
            this.daily_progress = daily_progress;
            this.weekly_goal = weekly_goal;
            this.weekly_completed = weekly_completed;
            this.weekly_progress = weekly_progress;
            this.monthly_goal = monthly_goal;
            this.monthly_completed = monthly_completed;
            this.monthly_progress = monthly_progress;
            this.next_milestone = next_milestone;
            this.milestone_progress = milestone_progress;
        }

        public int getDaily_goal() { return daily_goal; }
        public void setDaily_goal(int daily_goal) { this.daily_goal = daily_goal; }

        public int getDaily_completed() { return daily_completed; }
        public void setDaily_completed(int daily_completed) { this.daily_completed = daily_completed; }

        public double getDaily_progress() { return daily_progress; }
        public void setDaily_progress(double daily_progress) { this.daily_progress = daily_progress; }

        public int getWeekly_goal() { return weekly_goal; }
        public void setWeekly_goal(int weekly_goal) { this.weekly_goal = weekly_goal; }

        public int getWeekly_completed() { return weekly_completed; }
        public void setWeekly_completed(int weekly_completed) { this.weekly_completed = weekly_completed; }

        public double getWeekly_progress() { return weekly_progress; }
        public void setWeekly_progress(double weekly_progress) { this.weekly_progress = weekly_progress; }

        public int getMonthly_goal() { return monthly_goal; }
        public void setMonthly_goal(int monthly_goal) { this.monthly_goal = monthly_goal; }

        public int getMonthly_completed() { return monthly_completed; }
        public void setMonthly_completed(int monthly_completed) { this.monthly_completed = monthly_completed; }

        public double getMonthly_progress() { return monthly_progress; }
        public void setMonthly_progress(double monthly_progress) { this.monthly_progress = monthly_progress; }

        public String getNext_milestone() { return next_milestone; }
        public void setNext_milestone(String next_milestone) { this.next_milestone = next_milestone; }

        public double getMilestone_progress() { return milestone_progress; }
        public void setMilestone_progress(double milestone_progress) { this.milestone_progress = milestone_progress; }
    }

    @GetMapping("/progress")
    public ResponseEntity<ReviewProgressResponse> getReviewProgress(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        printRequest("获取复习进度");

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReviewProgressResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReviewProgressResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) users.get(0).get("user_id");
            System.out.println("当前用户ID: " + userId);

            // 2. 获取每日目标
            int dailyGoal = getDailyGoal(userId);

            // 3. 获取今日完成情况
            LocalDateTime now = LocalDateTime.now();
            String today = now.toLocalDate().toString();
            int dailyCompleted = getTodayCompleted(userId, today);

            // 4. 计算每日进度
            double dailyProgress = dailyGoal > 0 ? (double) dailyCompleted / dailyGoal * 100 : 0;

            // 5. 获取本周完成情况
            LocalDateTime weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1);
            String weekStartStr = weekStart.toLocalDate().toString();
            int weeklyCompleted = getWeekCompleted(userId, weekStartStr, today);

            // 6. 计算每周进度
            int weeklyGoal = dailyGoal * 5; // 假设每周目标为5天的每日目标
            double weeklyProgress = weeklyGoal > 0 ? (double) weeklyCompleted / weeklyGoal * 100 : 0;

            // 7. 获取本月完成情况
            LocalDateTime monthStart = LocalDate.of(now.getYear(), now.getMonth(), 1).atStartOfDay();
            String monthStartStr = monthStart.toLocalDate().toString();
            int monthlyCompleted = getMonthCompleted(userId, monthStartStr, today);

            // 8. 计算每月进度
            int monthlyGoal = dailyGoal * 20; // 假设每月目标为20天的每日目标
            double monthlyProgress = monthlyGoal > 0 ? (double) monthlyCompleted / monthlyGoal * 100 : 0;

            // 9. 获取下一个里程碑
            String nextMilestone = getNextMilestone(userId);
            double milestoneProgress = calculateMilestoneProgress(userId, nextMilestone);

            // 10. 构建响应数据
            ProgressData data = new ProgressData(
                    dailyGoal,
                    dailyCompleted,
                    Math.round(dailyProgress * 100.0) / 100.0,
                    weeklyGoal,
                    weeklyCompleted,
                    Math.round(weeklyProgress * 100.0) / 100.0,
                    monthlyGoal,
                    monthlyCompleted,
                    Math.round(monthlyProgress * 100.0) / 100.0,
                    nextMilestone,
                    Math.round(milestoneProgress * 100.0) / 100.0
            );

            ReviewProgressResponse response = new ReviewProgressResponse(true, "获取复习进度成功", data);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取复习进度过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReviewProgressResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private int getDailyGoal(int userId) {
        try {
            String sql = "SELECT daily_target_words FROM review_settings WHERE user_id = ?";
            List<Map<String, Object>> settings = jdbcTemplate.queryForList(sql, userId);

            if (!settings.isEmpty()) {
                return ((Number) settings.get(0).get("daily_target_words")).intValue();
            }

            return 20; // 默认每日目标

        } catch (Exception e) {
            System.err.println("获取每日目标失败: " + e.getMessage());
            return 20;
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

    private int getWeekCompleted(int userId, String weekStart, String today) {
        try {
            String sql = "SELECT COALESCE(SUM(total_words), 0) as week_words " +
                    "FROM review_sessions " +
                    "WHERE user_id = ? AND DATE(completed_at) BETWEEN ? AND ?";

            return jdbcTemplate.queryForObject(sql, Integer.class, userId, weekStart, today);

        } catch (Exception e) {
            System.err.println("获取本周完成情况失败: " + e.getMessage());
            return 0;
        }
    }

    private int getMonthCompleted(int userId, String monthStart, String today) {
        try {
            String sql = "SELECT COALESCE(SUM(total_words), 0) as month_words " +
                    "FROM review_sessions " +
                    "WHERE user_id = ? AND DATE(completed_at) BETWEEN ? AND ?";

            return jdbcTemplate.queryForObject(sql, Integer.class, userId, monthStart, today);

        } catch (Exception e) {
            System.err.println("获取本月完成情况失败: " + e.getMessage());
            return 0;
        }
    }

    private String getNextMilestone(int userId) {
        try {
            // 获取总复习单词数
            String sql = "SELECT COALESCE(SUM(total_words), 0) as total_words " +
                    "FROM review_sessions WHERE user_id = ?";

            int totalWords = jdbcTemplate.queryForObject(sql, Integer.class, userId);

            // 定义里程碑
            if (totalWords < 100) {
                return "100个单词";
            } else if (totalWords < 500) {
                return "500个单词";
            } else if (totalWords < 1000) {
                return "1000个单词";
            } else if (totalWords < 5000) {
                return "5000个单词";
            } else {
                return "10000个单词";
            }

        } catch (Exception e) {
            System.err.println("获取下一个里程碑失败: " + e.getMessage());
            return "100个单词";
        }
    }

    private double calculateMilestoneProgress(int userId, String nextMilestone) {
        try {
            // 获取总复习单词数
            String sql = "SELECT COALESCE(SUM(total_words), 0) as total_words " +
                    "FROM review_sessions WHERE user_id = ?";

            int totalWords = jdbcTemplate.queryForObject(sql, Integer.class, userId);

            // 解析里程碑目标
            int milestoneTarget = 0;
            if (nextMilestone.contains("100")) {
                milestoneTarget = 100;
            } else if (nextMilestone.contains("500")) {
                milestoneTarget = 500;
            } else if (nextMilestone.contains("1000")) {
                milestoneTarget = 1000;
            } else if (nextMilestone.contains("5000")) {
                milestoneTarget = 5000;
            } else if (nextMilestone.contains("10000")) {
                milestoneTarget = 10000;
            }

            // 计算进度
            return milestoneTarget > 0 ? (double) totalWords / milestoneTarget * 100 : 0;

        } catch (Exception e) {
            System.err.println("计算里程碑进度失败: " + e.getMessage());
            return 0;
        }
    }
}
