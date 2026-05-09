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
public class VocabularyGetLearningStats {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取学习统计请求 ===");
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
    public static class LearningStatsResponse {
        private boolean success;
        private String message;
        private LearningStatsData data;

        public LearningStatsResponse(boolean success, String message, LearningStatsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public LearningStatsData getData() { return data; }
        public void setData(LearningStatsData data) { this.data = data; }
    }

    public static class LearningStatsData {
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
        private List<DailyLearningStat> dailyStats;
        private Map<String, Object> masteryDistribution;
        private Map<String, Object> reviewFrequency;

        public LearningStatsData() {
            this.byLanguage = new HashMap<>();
            this.byStatus = new HashMap<>();
            this.dailyStats = new ArrayList<>();
            this.masteryDistribution = new HashMap<>();
            this.reviewFrequency = new HashMap<>();
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

        public List<DailyLearningStat> getDailyStats() { return dailyStats; }
        public void setDailyStats(List<DailyLearningStat> dailyStats) { this.dailyStats = dailyStats; }

        public Map<String, Object> getMasteryDistribution() { return masteryDistribution; }
        public void setMasteryDistribution(Map<String, Object> masteryDistribution) { this.masteryDistribution = masteryDistribution; }

        public Map<String, Object> getReviewFrequency() { return reviewFrequency; }
        public void setReviewFrequency(Map<String, Object> reviewFrequency) { this.reviewFrequency = reviewFrequency; }
    }

    public static class DailyLearningStat {
        private String date;
        private int addedCount;
        private int reviewedCount;
        private int masteredCount;
        private int totalStudyTime; // 分钟

        public DailyLearningStat(String date, int addedCount, int reviewedCount, int masteredCount, int totalStudyTime) {
            this.date = date;
            this.addedCount = addedCount;
            this.reviewedCount = reviewedCount;
            this.masteredCount = masteredCount;
            this.totalStudyTime = totalStudyTime;
        }

        // Getters and Setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public int getAddedCount() { return addedCount; }
        public void setAddedCount(int addedCount) { this.addedCount = addedCount; }

        public int getReviewedCount() { return reviewedCount; }
        public void setReviewedCount(int reviewedCount) { this.reviewedCount = reviewedCount; }

        public int getMasteredCount() { return masteredCount; }
        public void setMasteredCount(int masteredCount) { this.masteredCount = masteredCount; }

        public int getTotalStudyTime() { return totalStudyTime; }
        public void setTotalStudyTime(int totalStudyTime) { this.totalStudyTime = totalStudyTime; }
    }

    @GetMapping("/learning-stats")
    public ResponseEntity<LearningStatsResponse> getLearningStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String language) {

        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("period", period);
        requestParams.put("language", language);
        printRequest(requestParams);

        try {
            // 1. 验证用户身份
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new LearningStatsResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > ?";
            LocalDateTime now = LocalDateTime.now();
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(userSql, token, now);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new LearningStatsResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Long userId = ((Number) sessions.get(0).get("user_id")).longValue();

            // 2. 设置默认参数
            if (period == null) period = "week";
            LocalDate today = LocalDate.now();
            LocalDate startDate;

            switch (period.toLowerCase()) {
                case "day":
                    startDate = today;
                    break;
                case "week":
                    startDate = today.minusDays(6);
                    break;
                case "month":
                    startDate = today.minusDays(29);
                    break;
                case "year":
                    startDate = today.minusDays(364);
                    break;
                default:
                    startDate = today.minusDays(6);
                    period = "week";
            }

            // 3. 构建查询条件
            StringBuilder whereClause = new StringBuilder("WHERE user_id = ?");
            List<Object> params = new ArrayList<>();
            params.add(userId);

            if (language != null && !language.trim().isEmpty()) {
                whereClause.append(" AND language = ?");
                params.add(language.trim());
            }

            // 4. 查询总单词数
            String totalSql = "SELECT COUNT(*) as total FROM user_vocabulary " + whereClause.toString();
            Long totalWords = jdbcTemplate.queryForObject(totalSql, Long.class, params.toArray());

            // 5. 查询按状态统计
            String statusSql = "SELECT status, COUNT(*) as count FROM user_vocabulary " + whereClause.toString() + " GROUP BY status";
            List<Map<String, Object>> statusStats = jdbcTemplate.queryForList(statusSql, params.toArray());

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

            // 6. 查询按语言统计
            String languageSql = "SELECT language, COUNT(*) as count FROM user_vocabulary WHERE user_id = ? GROUP BY language";
            List<Map<String, Object>> languageStats = jdbcTemplate.queryForList(languageSql, userId);

            Map<String, Integer> byLanguage = new HashMap<>();
            for (Map<String, Object> row : languageStats) {
                String lang = (String) row.get("language");
                Long count = (Long) row.get("count");
                byLanguage.put(lang, count.intValue());
            }

            // 7. 查询今日添加
            String todayAddedSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ? AND DATE(created_at) = ?";
            Long todayAdded = jdbcTemplate.queryForObject(todayAddedSql, Long.class, userId, today);

            // 8. 查询今日复习
            String todayReviewedSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ? AND DATE(last_reviewed_at) = ?";
            Long todayReviewed = jdbcTemplate.queryForObject(todayReviewedSql, Long.class, userId, today);

            // 9. 查询本周进度（过去7天）
            LocalDate weekStart = today.minusDays(6);
            String weeklyProgressSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ? AND DATE(created_at) >= ?";
            Long weeklyProgress = jdbcTemplate.queryForObject(weeklyProgressSql, Long.class, userId, weekStart);

            // 10. 查询本月进度（过去30天）
            LocalDate monthStart = today.minusDays(29);
            String monthlyProgressSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ? AND DATE(created_at) >= ?";
            Long monthlyProgress = jdbcTemplate.queryForObject(monthlyProgressSql, Long.class, userId, monthStart);

            // 11. 查询连续学习天数
            String streakSql = "SELECT COUNT(DISTINCT DATE(created_at)) as streak FROM user_vocabulary " +
                    "WHERE user_id = ? AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                    "ORDER BY created_at DESC";
            Long streakDays = jdbcTemplate.queryForObject(streakSql, Long.class, userId);

            // 12. 查询每日统计
            List<DailyLearningStat> dailyStats = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // 根据period决定查询多少天的数据
            int daysToQuery = 0;
            switch (period.toLowerCase()) {
                case "day": daysToQuery = 1; break;
                case "week": daysToQuery = 7; break;
                case "month": daysToQuery = 30; break;
                case "year": daysToQuery = 365; break;
                default: daysToQuery = 7;
            }

            for (int i = daysToQuery - 1; i >= 0; i--) {
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

                // 查询当日学习时间（从daily_learning_stats表）
                String studyTimeSql = "SELECT total_study_time FROM daily_learning_stats WHERE user_id = ? AND date = ?";
                List<Map<String, Object>> studyTimeResults = jdbcTemplate.queryForList(studyTimeSql, userId, date);
                int totalStudyTime = 0;
                if (!studyTimeResults.isEmpty()) {
                    totalStudyTime = studyTimeResults.get(0).get("total_study_time") != null ?
                            ((Number) studyTimeResults.get(0).get("total_study_time")).intValue() : 0;
                }

                dailyStats.add(new DailyLearningStat(dateStr, dailyAdded.intValue(), dailyReviewed.intValue(),
                        dailyMastered.intValue(), totalStudyTime));
            }

            // 13. 查询掌握程度分布
            Map<String, Object> masteryDistribution = new HashMap<>();
            String masterySql = "SELECT mastery_level, COUNT(*) as count FROM user_vocabulary WHERE user_id = ? GROUP BY mastery_level ORDER BY mastery_level";
            List<Map<String, Object>> masteryStats = jdbcTemplate.queryForList(masterySql, userId);

            for (Map<String, Object> row : masteryStats) {
                Integer level = row.get("mastery_level") != null ? ((Number) row.get("mastery_level")).intValue() : 0;
                Long count = (Long) row.get("count");
                masteryDistribution.put("level_" + level, count.intValue());
            }

            // 14. 查询复习频率
            Map<String, Object> reviewFrequency = new HashMap<>();
            String reviewFreqSql = "SELECT review_count, COUNT(*) as count FROM user_vocabulary WHERE user_id = ? GROUP BY review_count ORDER BY review_count";
            List<Map<String, Object>> reviewFreqStats = jdbcTemplate.queryForList(reviewFreqSql, userId);

            for (Map<String, Object> row : reviewFreqStats) {
                Integer count = row.get("review_count") != null ? ((Number) row.get("review_count")).intValue() : 0;
                Long freq = (Long) row.get("count");
                reviewFrequency.put("reviewed_" + count + "_times", freq.intValue());
            }

            // 15. 组装统计数据
            LearningStatsData statsData = new LearningStatsData();
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
            statsData.setMasteryDistribution(masteryDistribution);
            statsData.setReviewFrequency(reviewFrequency);

            printQueryResult(statsData);

            // 16. 创建响应
            LearningStatsResponse response = new LearningStatsResponse(true, "获取学习统计成功", statsData);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取学习统计过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new LearningStatsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
