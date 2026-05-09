package com.vue.readingapp.vocabulary;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/vocabulary")
public class VocabularyGetReviewStats {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取复习统计请求 ===");
        System.out.println("请求参数: " + request);
        System.out.println("==========================");
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
        private int dueToday;
        private int dueTomorrow;
        private int dueThisWeek;
        private int totalDue;
        private double reviewAccuracy;
        private double averageMastery;
        private String lastReviewDate;
        private List<ReviewHistoryItem> reviewHistory;
        private Map<String, Integer> dailyReviewTrend;
        private Map<String, Object> reviewPerformance;

        public ReviewStatsData() {
            this.reviewHistory = new ArrayList<>();
            this.dailyReviewTrend = new HashMap<>();
            this.reviewPerformance = new HashMap<>();
        }

        // Getters and Setters
        public int getDueToday() { return dueToday; }
        public void setDueToday(int dueToday) { this.dueToday = dueToday; }

        public int getDueTomorrow() { return dueTomorrow; }
        public void setDueTomorrow(int dueTomorrow) { this.dueTomorrow = dueTomorrow; }

        public int getDueThisWeek() { return dueThisWeek; }
        public void setDueThisWeek(int dueThisWeek) { this.dueThisWeek = dueThisWeek; }

        public int getTotalDue() { return totalDue; }
        public void setTotalDue(int totalDue) { this.totalDue = totalDue; }

        public double getReviewAccuracy() { return reviewAccuracy; }
        public void setReviewAccuracy(double reviewAccuracy) { this.reviewAccuracy = reviewAccuracy; }

        public double getAverageMastery() { return averageMastery; }
        public void setAverageMastery(double averageMastery) { this.averageMastery = averageMastery; }

        public String getLastReviewDate() { return lastReviewDate; }
        public void setLastReviewDate(String lastReviewDate) { this.lastReviewDate = lastReviewDate; }

        public List<ReviewHistoryItem> getReviewHistory() { return reviewHistory; }
        public void setReviewHistory(List<ReviewHistoryItem> reviewHistory) { this.reviewHistory = reviewHistory; }

        public Map<String, Integer> getDailyReviewTrend() { return dailyReviewTrend; }
        public void setDailyReviewTrend(Map<String, Integer> dailyReviewTrend) { this.dailyReviewTrend = dailyReviewTrend; }

        public Map<String, Object> getReviewPerformance() { return reviewPerformance; }
        public void setReviewPerformance(Map<String, Object> reviewPerformance) { this.reviewPerformance = reviewPerformance; }
    }

    public static class ReviewHistoryItem {
        private String date;
        private int reviewedCount;
        private int correctCount;
        private double accuracy;

        public ReviewHistoryItem(String date, int reviewedCount, int correctCount, double accuracy) {
            this.date = date;
            this.reviewedCount = reviewedCount;
            this.correctCount = correctCount;
            this.accuracy = accuracy;
        }

        // Getters and Setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public int getReviewedCount() { return reviewedCount; }
        public void setReviewedCount(int reviewedCount) { this.reviewedCount = reviewedCount; }

        public int getCorrectCount() { return correctCount; }
        public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }

        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    }

    @GetMapping("/review-stats")
    public ResponseEntity<ReviewStatsResponse> getReviewStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        printRequest("获取复习统计");

        try {
            // 1. 验证用户身份
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReviewStatsResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > ?";
            LocalDateTime now = LocalDateTime.now();
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(userSql, token, now);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReviewStatsResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Long userId = ((Number) sessions.get(0).get("user_id")).longValue();
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            LocalDate weekEnd = today.plusDays(7);

            // 2. 查询今日到期单词
            String dueTodaySql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ? AND " +
                    "(next_review_at IS NULL OR DATE(next_review_at) <= ?) AND status != 'mastered'";
            Long dueToday = jdbcTemplate.queryForObject(dueTodaySql, Long.class, userId, today);

            // 3. 查询明日到期单词
            String dueTomorrowSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ? AND " +
                    "DATE(next_review_at) = ? AND status != 'mastered'";
            Long dueTomorrow = jdbcTemplate.queryForObject(dueTomorrowSql, Long.class, userId, tomorrow);

            // 4. 查询本周到期单词
            String dueThisWeekSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ? AND " +
                    "DATE(next_review_at) BETWEEN ? AND ? AND status != 'mastered'";
            Long dueThisWeek = jdbcTemplate.queryForObject(dueThisWeekSql, Long.class, userId, today, weekEnd);

            // 5. 查询总到期单词（所有未掌握的）
            String totalDueSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ? AND status != 'mastered'";
            Long totalDue = jdbcTemplate.queryForObject(totalDueSql, Long.class, userId);

            // 6. 查询复习准确率（从复习记录中计算）
            String accuracySql = "SELECT " +
                    "COUNT(*) as total_reviews, " +
                    "SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) as correct_reviews " +
                    "FROM review_items WHERE user_id = ? AND review_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)";

            List<Map<String, Object>> accuracyResults = jdbcTemplate.queryForList(accuracySql, userId);
            double reviewAccuracy = 0.0;
            if (!accuracyResults.isEmpty()) {
                Map<String, Object> row = accuracyResults.get(0);
                Long totalReviews = (Long) row.get("total_reviews");
                Long correctReviews = (Long) row.get("correct_reviews");

                if (totalReviews != null && totalReviews > 0) {
                    reviewAccuracy = (double) correctReviews / totalReviews * 100;
                }
            }

            // 7. 查询平均掌握程度
            String avgMasterySql = "SELECT AVG(mastery_level) as avg_mastery FROM user_vocabulary WHERE user_id = ?";
            Double avgMastery = jdbcTemplate.queryForObject(avgMasterySql, Double.class, userId);
            if (avgMastery == null) avgMastery = 0.0;

            // 8. 查询最后复习日期
            String lastReviewSql = "SELECT MAX(last_reviewed_at) as last_review FROM user_vocabulary WHERE user_id = ?";
            List<Map<String, Object>> lastReviewResults = jdbcTemplate.queryForList(lastReviewSql, userId);
            String lastReviewDate = null;
            if (!lastReviewResults.isEmpty()) {
                Object lastReview = lastReviewResults.get(0).get("last_review");
                if (lastReview != null) {
                    lastReviewDate = lastReview.toString();
                }
            }

            // 9. 查询复习历史（最近7天）
            List<ReviewHistoryItem> reviewHistory = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                String dateStr = date.format(formatter);

                // 查询当日复习数量
                String dailyReviewSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ? AND DATE(last_reviewed_at) = ?";
                Long dailyReviewed = jdbcTemplate.queryForObject(dailyReviewSql, Long.class, userId, date);

                // 查询当日正确数量（从review_items表）
                String dailyCorrectSql = "SELECT COUNT(*) as count FROM review_items WHERE user_id = ? AND DATE(review_date) = ? AND is_correct = 1";
                Long dailyCorrect = jdbcTemplate.queryForObject(dailyCorrectSql, Long.class, userId, date);

                double dailyAccuracy = 0.0;
                if (dailyReviewed != null && dailyReviewed > 0 && dailyCorrect != null) {
                    dailyAccuracy = (double) dailyCorrect / dailyReviewed * 100;
                }

                reviewHistory.add(new ReviewHistoryItem(dateStr,
                        dailyReviewed != null ? dailyReviewed.intValue() : 0,
                        dailyCorrect != null ? dailyCorrect.intValue() : 0,
                        dailyAccuracy));
            }

            // 10. 查询每日复习趋势（最近30天）
            Map<String, Integer> dailyReviewTrend = new LinkedHashMap<>();
            for (int i = 29; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                String dateStr = date.format(formatter);

                String trendSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ? AND DATE(last_reviewed_at) = ?";
                Long dailyCount = jdbcTemplate.queryForObject(trendSql, Long.class, userId, date);

                dailyReviewTrend.put(dateStr, dailyCount != null ? dailyCount.intValue() : 0);
            }

            // 11. 查询复习表现统计
            Map<String, Object> reviewPerformance = new HashMap<>();

            // 查询不同掌握程度的复习表现
            String performanceSql = "SELECT " +
                    "mastery_level, " +
                    "COUNT(*) as total_words, " +
                    "AVG(review_count) as avg_reviews " +
                    "FROM user_vocabulary WHERE user_id = ? AND status != 'mastered' " +
                    "GROUP BY mastery_level ORDER BY mastery_level";

            List<Map<String, Object>> performanceResults = jdbcTemplate.queryForList(performanceSql, userId);
            Map<String, Object> masteryPerformance = new HashMap<>();
            for (Map<String, Object> row : performanceResults) {
                Integer level = row.get("mastery_level") != null ? ((Number) row.get("mastery_level")).intValue() : 0;
                Long totalWords = (Long) row.get("total_words");
                Double avgReviews = (Double) row.get("avg_reviews");

                Map<String, Object> levelStats = new HashMap<>();
                levelStats.put("total_words", totalWords != null ? totalWords.intValue() : 0);
                levelStats.put("avg_reviews", avgReviews != null ? avgReviews : 0.0);
                masteryPerformance.put("level_" + level, levelStats);
            }

            reviewPerformance.put("mastery_level_performance", masteryPerformance);

            // 12. 组装统计数据
            ReviewStatsData statsData = new ReviewStatsData();
            statsData.setDueToday(dueToday != null ? dueToday.intValue() : 0);
            statsData.setDueTomorrow(dueTomorrow != null ? dueTomorrow.intValue() : 0);
            statsData.setDueThisWeek(dueThisWeek != null ? dueThisWeek.intValue() : 0);
            statsData.setTotalDue(totalDue != null ? totalDue.intValue() : 0);
            statsData.setReviewAccuracy(reviewAccuracy);
            statsData.setAverageMastery(avgMastery);
            statsData.setLastReviewDate(lastReviewDate);
            statsData.setReviewHistory(reviewHistory);
            statsData.setDailyReviewTrend(dailyReviewTrend);
            statsData.setReviewPerformance(reviewPerformance);

            printQueryResult(statsData);

            // 13. 创建响应
            ReviewStatsResponse response = new ReviewStatsResponse(true, "获取复习统计成功", statsData);

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
}
