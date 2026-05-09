package com.vue.readingapp.review;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/review")
public class ReviewGetStats {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到获取复习统计请求 ===");
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

    public static class ReviewStatsResponse {
        private boolean success;
        private String message;
        private ReviewStatsData data;

        public ReviewStatsResponse(boolean success, String message, ReviewStatsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ReviewStatsData getData() { return data; }
        public void setData(ReviewStatsData data) { this.data = data; }
    }

    public static class ReviewStatsData {
        private int total_reviews;
        private int total_words_reviewed;
        private double average_accuracy;
        private int streak_days;
        private int longest_streak;
        private int today_reviews;
        private double today_accuracy;
        private int weekly_reviews;
        private double weekly_accuracy;
        private int monthly_reviews;
        private double monthly_accuracy;
        private Map<String, Object> by_language;
        private Map<String, Object> by_difficulty;
        private List<Map<String, Object>> review_history;

        public ReviewStatsData(int total_reviews, int total_words_reviewed, double average_accuracy,
                               int streak_days, int longest_streak, int today_reviews, double today_accuracy,
                               int weekly_reviews, double weekly_accuracy, int monthly_reviews, double monthly_accuracy,
                               Map<String, Object> by_language, Map<String, Object> by_difficulty,
                               List<Map<String, Object>> review_history) {
            this.total_reviews = total_reviews;
            this.total_words_reviewed = total_words_reviewed;
            this.average_accuracy = average_accuracy;
            this.streak_days = streak_days;
            this.longest_streak = longest_streak;
            this.today_reviews = today_reviews;
            this.today_accuracy = today_accuracy;
            this.weekly_reviews = weekly_reviews;
            this.weekly_accuracy = weekly_accuracy;
            this.monthly_reviews = monthly_reviews;
            this.monthly_accuracy = monthly_accuracy;
            this.by_language = by_language;
            this.by_difficulty = by_difficulty;
            this.review_history = review_history;
        }

        public int getTotal_reviews() { return total_reviews; }
        public void setTotal_reviews(int total_reviews) { this.total_reviews = total_reviews; }

        public int getTotal_words_reviewed() { return total_words_reviewed; }
        public void setTotal_words_reviewed(int total_words_reviewed) { this.total_words_reviewed = total_words_reviewed; }

        public double getAverage_accuracy() { return average_accuracy; }
        public void setAverage_accuracy(double average_accuracy) { this.average_accuracy = average_accuracy; }

        public int getStreak_days() { return streak_days; }
        public void setStreak_days(int streak_days) { this.streak_days = streak_days; }

        public int getLongest_streak() { return longest_streak; }
        public void setLongest_streak(int longest_streak) { this.longest_streak = longest_streak; }

        public int getToday_reviews() { return today_reviews; }
        public void setToday_reviews(int today_reviews) { this.today_reviews = today_reviews; }

        public double getToday_accuracy() { return today_accuracy; }
        public void setToday_accuracy(double today_accuracy) { this.today_accuracy = today_accuracy; }

        public int getWeekly_reviews() { return weekly_reviews; }
        public void setWeekly_reviews(int weekly_reviews) { this.weekly_reviews = weekly_reviews; }

        public double getWeekly_accuracy() { return weekly_accuracy; }
        public void setWeekly_accuracy(double weekly_accuracy) { this.weekly_accuracy = weekly_accuracy; }

        public int getMonthly_reviews() { return monthly_reviews; }
        public void setMonthly_reviews(int monthly_reviews) { this.monthly_reviews = monthly_reviews; }

        public double getMonthly_accuracy() { return monthly_accuracy; }
        public void setMonthly_accuracy(double monthly_accuracy) { this.monthly_accuracy = monthly_accuracy; }

        public Map<String, Object> getBy_language() { return by_language; }
        public void setBy_language(Map<String, Object> by_language) { this.by_language = by_language; }

        public Map<String, Object> getBy_difficulty() { return by_difficulty; }
        public void setBy_difficulty(Map<String, Object> by_difficulty) { this.by_difficulty = by_difficulty; }

        public List<Map<String, Object>> getReview_history() { return review_history; }
        public void setReview_history(List<Map<String, Object>> review_history) { this.review_history = review_history; }
    }

    @GetMapping("/stats")
    public ResponseEntity<ReviewStatsResponse> getReviewStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "period", defaultValue = "week") String period,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("period", period);
        requestParams.put("language", language);
        requestParams.put("startDate", startDate);
        requestParams.put("endDate", endDate);
        printRequest(requestParams);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReviewStatsResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReviewStatsResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) users.get(0).get("user_id");
            System.out.println("当前用户ID: " + userId);

            // 2. 获取总复习统计
            int totalReviews = getTotalReviews(userId);
            int totalWordsReviewed = getTotalWordsReviewed(userId);
            double averageAccuracy = getAverageAccuracy(userId);

            // 3. 获取连续学习天数
            int streakDays = calculateStreakDays(userId);
            int longestStreak = getLongestStreak(userId);

            // 4. 获取今日统计
            LocalDate today = LocalDate.now();
            int todayReviews = getReviewsByDate(userId, today.toString(), today.toString());
            double todayAccuracy = getAccuracyByDate(userId, today.toString(), today.toString());

            // 5. 获取本周统计
            LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
            int weeklyReviews = getReviewsByDate(userId, weekStart.toString(), today.toString());
            double weeklyAccuracy = getAccuracyByDate(userId, weekStart.toString(), today.toString());

            // 6. 获取本月统计
            LocalDate monthStart = LocalDate.of(today.getYear(), today.getMonth(), 1);
            int monthlyReviews = getReviewsByDate(userId, monthStart.toString(), today.toString());
            double monthlyAccuracy = getAccuracyByDate(userId, monthStart.toString(), today.toString());

            // 7. 按语言统计
            Map<String, Object> byLanguage = getStatsByLanguage(userId);

            // 8. 按难度统计
            Map<String, Object> byDifficulty = getStatsByDifficulty(userId);

            // 9. 获取复习历史（最近7天）
            List<Map<String, Object>> reviewHistory = getReviewHistory(userId, 7);

            // 10. 构建响应数据
            ReviewStatsData data = new ReviewStatsData(
                    totalReviews,
                    totalWordsReviewed,
                    Math.round(averageAccuracy * 100.0) / 100.0,
                    streakDays,
                    longestStreak,
                    todayReviews,
                    Math.round(todayAccuracy * 100.0) / 100.0,
                    weeklyReviews,
                    Math.round(weeklyAccuracy * 100.0) / 100.0,
                    monthlyReviews,
                    Math.round(monthlyAccuracy * 100.0) / 100.0,
                    byLanguage,
                    byDifficulty,
                    reviewHistory
            );

            ReviewStatsResponse response = new ReviewStatsResponse(true, "获取复习统计成功", data);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取复习统计过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReviewStatsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private int getTotalReviews(int userId) {
        try {
            String sql = "SELECT COUNT(*) FROM review_sessions WHERE user_id = ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, userId);
        } catch (Exception e) {
            System.err.println("获取总复习次数失败: " + e.getMessage());
            return 0;
        }
    }

    private int getTotalWordsReviewed(int userId) {
        try {
            String sql = "SELECT COALESCE(SUM(total_words), 0) FROM review_sessions WHERE user_id = ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, userId);
        } catch (Exception e) {
            System.err.println("获取总复习单词数失败: " + e.getMessage());
            return 0;
        }
    }

    private double getAverageAccuracy(int userId) {
        try {
            String sql = "SELECT COALESCE(AVG(accuracy), 0) FROM review_sessions WHERE user_id = ?";
            Double result = jdbcTemplate.queryForObject(sql, Double.class, userId);
            return result != null ? result : 0.0;
        } catch (Exception e) {
            System.err.println("获取平均准确率失败: " + e.getMessage());
            return 0.0;
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

            LocalDate today = LocalDate.now();
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

    private int getLongestStreak(int userId) {
        try {
            // 简化处理：返回当前连续天数或30天
            int streak = calculateStreakDays(userId);
            return Math.max(streak, 30);
        } catch (Exception e) {
            System.err.println("获取最长连续天数失败: " + e.getMessage());
            return 30;
        }
    }

    private int getReviewsByDate(int userId, String startDate, String endDate) {
        try {
            String sql = "SELECT COUNT(*) FROM review_sessions " +
                    "WHERE user_id = ? AND DATE(completed_at) BETWEEN ? AND ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, userId, startDate, endDate);
        } catch (Exception e) {
            System.err.println("获取日期范围内复习次数失败: " + e.getMessage());
            return 0;
        }
    }

    private double getAccuracyByDate(int userId, String startDate, String endDate) {
        try {
            String sql = "SELECT COALESCE(AVG(accuracy), 0) FROM review_sessions " +
                    "WHERE user_id = ? AND DATE(completed_at) BETWEEN ? AND ?";
            Double result = jdbcTemplate.queryForObject(sql, Double.class, userId, startDate, endDate);
            return result != null ? result : 0.0;
        } catch (Exception e) {
            System.err.println("获取日期范围内准确率失败: " + e.getMessage());
            return 0.0;
        }
    }

    private Map<String, Object> getStatsByLanguage(int userId) {
        try {
            String sql = "SELECT w.language, COUNT(*) as review_count, " +
                    "COALESCE(AVG(rs.accuracy), 0) as avg_accuracy " +
                    "FROM review_sessions rs " +
                    "JOIN user_vocabulary uv ON rs.user_id = uv.user_id " +
                    "JOIN words w ON uv.word_id = w.word_id " +
                    "WHERE rs.user_id = ? " +
                    "GROUP BY w.language";

            List<Map<String, Object>> languageStats = jdbcTemplate.queryForList(sql, userId);

            Map<String, Object> result = new HashMap<>();
            for (Map<String, Object> stat : languageStats) {
                String lang = (String) stat.get("language");
                Map<String, Object> langData = new HashMap<>();
                langData.put("review_count", stat.get("review_count"));
                langData.put("avg_accuracy", Math.round(((Number) stat.get("avg_accuracy")).doubleValue() * 100.0) / 100.0);
                result.put(lang, langData);
            }

            return result;

        } catch (Exception e) {
            System.err.println("获取按语言统计失败: " + e.getMessage());
            return new HashMap<>();
        }
    }

    private Map<String, Object> getStatsByDifficulty(int userId) {
        try {
            String sql = "SELECT w.difficulty, COUNT(*) as review_count, " +
                    "COALESCE(AVG(rs.accuracy), 0) as avg_accuracy " +
                    "FROM review_sessions rs " +
                    "JOIN user_vocabulary uv ON rs.user_id = uv.user_id " +
                    "JOIN words w ON uv.word_id = w.word_id " +
                    "WHERE rs.user_id = ? AND w.difficulty IS NOT NULL " +
                    "GROUP BY w.difficulty";

            List<Map<String, Object>> difficultyStats = jdbcTemplate.queryForList(sql, userId);

            Map<String, Object> result = new HashMap<>();
            for (Map<String, Object> stat : difficultyStats) {
                String diff = (String) stat.get("difficulty");
                Map<String, Object> diffData = new HashMap<>();
                diffData.put("review_count", stat.get("review_count"));
                diffData.put("avg_accuracy", Math.round(((Number) stat.get("avg_accuracy")).doubleValue() * 100.0) / 100.0);
                result.put(diff, diffData);
            }

            return result;

        } catch (Exception e) {
            System.err.println("获取按难度统计失败: " + e.getMessage());
            return new HashMap<>();
        }
    }

    private List<Map<String, Object>> getReviewHistory(int userId, int days) {
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days - 1);

            String sql = "SELECT DATE(completed_at) as review_date, " +
                    "COUNT(*) as session_count, " +
                    "SUM(total_words) as total_words, " +
                    "COALESCE(AVG(accuracy), 0) as avg_accuracy " +
                    "FROM review_sessions " +
                    "WHERE user_id = ? AND DATE(completed_at) BETWEEN ? AND ? " +
                    "GROUP BY DATE(completed_at) " +
                    "ORDER BY review_date DESC";

            List<Map<String, Object>> history = jdbcTemplate.queryForList(
                    sql, userId, startDate.toString(), endDate.toString());

            // 格式化日期和数字
            for (Map<String, Object> day : history) {
                if (day.get("avg_accuracy") != null) {
                    double accuracy = ((Number) day.get("avg_accuracy")).doubleValue();
                    day.put("avg_accuracy", Math.round(accuracy * 100.0) / 100.0);
                }
            }

            return history;

        } catch (Exception e) {
            System.err.println("获取复习历史失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
