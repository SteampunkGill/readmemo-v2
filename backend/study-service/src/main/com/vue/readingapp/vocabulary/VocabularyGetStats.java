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
public class VocabularyGetStats {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取生词统计请求 ===");
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
    public static class VocabularyStatsResponse {
        private boolean success;
        private String message;
        private VocabularyStatsData data;

        public VocabularyStatsResponse(boolean success, String message, VocabularyStatsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public VocabularyStatsData getData() { return data; }
        public void setData(VocabularyStatsData data) { this.data = data; }
    }

    public static class VocabularyStatsData {
        private int totalWords;
        private int masteredWords;
        private int learningWords;
        private int newWords;
        private int todayAdded;
        private int todayReviewed;
        private int weeklyProgress;
        private int monthlyProgress;
        private Map<String, Integer> byLanguage;
        private Map<String, Integer> byStatus;
        private int streakDays;
        private List<DailyStat> dailyStats;

        public VocabularyStatsData() {
            this.byLanguage = new HashMap<>();
            this.byStatus = new HashMap<>();
            this.dailyStats = new ArrayList<>();
        }

        // Getters and Setters
        public int getTotalWords() { return totalWords; }
        public void setTotalWords(int totalWords) { this.totalWords = totalWords; }

        public int getMasteredWords() { return masteredWords; }
        public void setMasteredWords(int masteredWords) { this.masteredWords = masteredWords; }

        public int getLearningWords() { return learningWords; }
        public void setLearningWords(int learningWords) { this.learningWords = learningWords; }

        public int getNewWords() { return newWords; }
        public void setNewWords(int newWords) { this.newWords = newWords; }

        public int getTodayAdded() { return todayAdded; }
        public void setTodayAdded(int todayAdded) { this.todayAdded = todayAdded; }

        public int getTodayReviewed() { return todayReviewed; }
        public void setTodayReviewed(int todayReviewed) { this.todayReviewed = todayReviewed; }

        public int getWeeklyProgress() { return weeklyProgress; }
        public void setWeeklyProgress(int weeklyProgress) { this.weeklyProgress = weeklyProgress; }

        public int getMonthlyProgress() { return monthlyProgress; }
        public void setMonthlyProgress(int monthlyProgress) { this.monthlyProgress = monthlyProgress; }

        public Map<String, Integer> getByLanguage() { return byLanguage; }
        public void setByLanguage(Map<String, Integer> byLanguage) { this.byLanguage = byLanguage; }

        public Map<String, Integer> getByStatus() { return byStatus; }
        public void setByStatus(Map<String, Integer> byStatus) { this.byStatus = byStatus; }

        public int getStreakDays() { return streakDays; }
        public void setStreakDays(int streakDays) { this.streakDays = streakDays; }

        public List<DailyStat> getDailyStats() { return dailyStats; }
        public void setDailyStats(List<DailyStat> dailyStats) { this.dailyStats = dailyStats; }
    }

    public static class DailyStat {
        private String date;
        private int addedCount;
        private int reviewedCount;
        private int masteredCount;

        public DailyStat(String date, int addedCount, int reviewedCount, int masteredCount) {
            this.date = date;
            this.addedCount = addedCount;
            this.reviewedCount = reviewedCount;
            this.masteredCount = masteredCount;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public int getAddedCount() { return addedCount; }
        public void setAddedCount(int addedCount) { this.addedCount = addedCount; }

        public int getReviewedCount() { return reviewedCount; }
        public void setReviewedCount(int reviewedCount) { this.reviewedCount = reviewedCount; }

        public int getMasteredCount() { return masteredCount; }
        public void setMasteredCount(int masteredCount) { this.masteredCount = masteredCount; }
    }

    @GetMapping("/stats")
    public ResponseEntity<VocabularyStatsResponse> getVocabularyStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        printRequest("获取生词统计");

        try {
            // 1. 验证用户身份
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new VocabularyStatsResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > ?";
            LocalDateTime now = LocalDateTime.now();
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(userSql, token, now);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new VocabularyStatsResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Long userId = ((Number) sessions.get(0).get("user_id")).longValue();

            // 2. 查询总单词数
            String totalSql = "SELECT COUNT(*) as total FROM user_vocabulary WHERE user_id = ?";
            Long totalWords = jdbcTemplate.queryForObject(totalSql, Long.class, userId);

            // 3. 查询按状态统计
            String statusSql = "SELECT status, COUNT(*) as count FROM user_vocabulary WHERE user_id = ? GROUP BY status";
            List<Map<String, Object>> statusStats = jdbcTemplate.queryForList(statusSql, userId);

            int masteredWords = 0;
            int learningWords = 0;
            int newWords = 0;
            Map<String, Integer> byStatus = new HashMap<>();

            for (Map<String, Object> row : statusStats) {
                String status = (String) row.get("status");
                Long count = (Long) row.get("count");
                byStatus.put(status, count.intValue());

                switch (status) {
                    case "mastered":
                        masteredWords = count.intValue();
                        break;
                    case "learning":
                        learningWords = count.intValue();
                        break;
                    case "new":
                        newWords = count.intValue();
                        break;
                }
            }

            // 4. 查询按语言统计
            String languageSql = "SELECT language, COUNT(*) as count FROM user_vocabulary WHERE user_id = ? GROUP BY language";
            List<Map<String, Object>> languageStats = jdbcTemplate.queryForList(languageSql, userId);

            Map<String, Integer> byLanguage = new HashMap<>();
            for (Map<String, Object> row : languageStats) {
                String language = (String) row.get("language");
                Long count = (Long) row.get("count");
                byLanguage.put(language, count.intValue());
            }

            // 5. 查询今日添加
            LocalDate today = LocalDate.now();
            String todayAddedSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ? AND DATE(created_at) = ?";
            Long todayAdded = jdbcTemplate.queryForObject(todayAddedSql, Long.class, userId, today);

            // 6. 查询今日复习
            String todayReviewedSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ? AND DATE(last_reviewed_at) = ?";
            Long todayReviewed = jdbcTemplate.queryForObject(todayReviewedSql, Long.class, userId, today);

            // 7. 查询本周进度（过去7天）
            LocalDate weekStart = today.minusDays(6);
            String weeklyProgressSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ? AND DATE(created_at) >= ?";
            Long weeklyProgress = jdbcTemplate.queryForObject(weeklyProgressSql, Long.class, userId, weekStart);

            // 8. 查询本月进度（过去30天）
            LocalDate monthStart = today.minusDays(29);
            String monthlyProgressSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ? AND DATE(created_at) >= ?";
            Long monthlyProgress = jdbcTemplate.queryForObject(monthlyProgressSql, Long.class, userId, monthStart);

            // 9. 查询连续学习天数
            String streakSql = "SELECT COUNT(DISTINCT DATE(created_at)) as streak FROM user_vocabulary " +
                    "WHERE user_id = ? AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                    "ORDER BY created_at DESC";
            Long streakDays = jdbcTemplate.queryForObject(streakSql, Long.class, userId);

            // 10. 查询每日统计（最近7天）
            List<DailyStat> dailyStats = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                String dateStr = date.format(formatter);

                // 查询当日添加
                String dailyAddedSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ? AND DATE(created_at) = ?";
                Long dailyAdded = jdbcTemplate.queryForObject(dailyAddedSql, Long.class, userId, date);

                // 查询当日复习
                String dailyReviewedSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ? AND DATE(last_reviewed_at) = ?";
                Long dailyReviewed = jdbcTemplate.queryForObject(dailyReviewedSql, Long.class, userId, date);

                // 查询当日掌握
                String dailyMasteredSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ? AND status = 'mastered' AND DATE(updated_at) = ?";
                Long dailyMastered = jdbcTemplate.queryForObject(dailyMasteredSql, Long.class, userId, date);

                dailyStats.add(new DailyStat(dateStr, dailyAdded.intValue(), dailyReviewed.intValue(), dailyMastered.intValue()));
            }

            // 11. 组装统计数据
            VocabularyStatsData statsData = new VocabularyStatsData();
            statsData.setTotalWords(totalWords.intValue());
            statsData.setMasteredWords(masteredWords);
            statsData.setLearningWords(learningWords);
            statsData.setNewWords(newWords);
            statsData.setTodayAdded(todayAdded.intValue());
            statsData.setTodayReviewed(todayReviewed.intValue());
            statsData.setWeeklyProgress(weeklyProgress.intValue());
            statsData.setMonthlyProgress(monthlyProgress.intValue());
            statsData.setByLanguage(byLanguage);
            statsData.setByStatus(byStatus);
            statsData.setStreakDays(streakDays != null ? streakDays.intValue() : 0);
            statsData.setDailyStats(dailyStats);

            printQueryResult(statsData);

            // 12. 创建响应
            VocabularyStatsResponse response = new VocabularyStatsResponse(true, "获取生词统计成功", statsData);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取生词统计过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new VocabularyStatsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
