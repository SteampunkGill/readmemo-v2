package com.vue.readingapp.user;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/user")
public class UserGetLearningReport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取学习报告请求 ===");
        System.out.println("请求参数: " + request);
        System.out.println("=================");
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
    public static class LearningReportResponse {
        private boolean success;
        private String message;
        private ReportData report;

        public LearningReportResponse(boolean success, String message, ReportData report) {
            this.success = success;
            this.message = message;
            this.report = report;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ReportData getReport() { return report; }
        public void setReport(ReportData report) { this.report = report; }
    }

    public static class ReportData {
        private String period;
        private String startDate;
        private String endDate;
        private SummaryData summary;
        private List<DailyData> dailyBreakdown;
        private List<TopWordData> topWords;
        private Map<String, Object> readingPatterns;
        private List<String> recommendations;
        private String generatedAt;

        // Getters and Setters
        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }

        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }

        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }

        public SummaryData getSummary() { return summary; }
        public void setSummary(SummaryData summary) { this.summary = summary; }

        public List<DailyData> getDailyBreakdown() { return dailyBreakdown; }
        public void setDailyBreakdown(List<DailyData> dailyBreakdown) { this.dailyBreakdown = dailyBreakdown; }

        public List<TopWordData> getTopWords() { return topWords; }
        public void setTopWords(List<TopWordData> topWords) { this.topWords = topWords; }

        public Map<String, Object> getReadingPatterns() { return readingPatterns; }
        public void setReadingPatterns(Map<String, Object> readingPatterns) { this.readingPatterns = readingPatterns; }

        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }

        public String getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
    }

    public static class SummaryData {
        private int totalReadingTime;
        private int documentsRead;
        private int wordsLearned;
        private int reviewsCompleted;

        public SummaryData(int totalReadingTime, int documentsRead, int wordsLearned, int reviewsCompleted) {
            this.totalReadingTime = totalReadingTime;
            this.documentsRead = documentsRead;
            this.wordsLearned = wordsLearned;
            this.reviewsCompleted = reviewsCompleted;
        }

        public int getTotalReadingTime() { return totalReadingTime; }
        public void setTotalReadingTime(int totalReadingTime) { this.totalReadingTime = totalReadingTime; }

        public int getDocumentsRead() { return documentsRead; }
        public void setDocumentsRead(int documentsRead) { this.documentsRead = documentsRead; }

        public int getWordsLearned() { return wordsLearned; }
        public void setWordsLearned(int wordsLearned) { this.wordsLearned = wordsLearned; }

        public int getReviewsCompleted() { return reviewsCompleted; }
        public void setReviewsCompleted(int reviewsCompleted) { this.reviewsCompleted = reviewsCompleted; }
    }

    public static class DailyData {
        private String date;
        private int readingTime;
        private int wordsLearned;
        private int reviewsCompleted;

        public DailyData(String date, int readingTime, int wordsLearned, int reviewsCompleted) {
            this.date = date;
            this.readingTime = readingTime;
            this.wordsLearned = wordsLearned;
            this.reviewsCompleted = reviewsCompleted;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public int getReadingTime() { return readingTime; }
        public void setReadingTime(int readingTime) { this.readingTime = readingTime; }

        public int getWordsLearned() { return wordsLearned; }
        public void setWordsLearned(int wordsLearned) { this.wordsLearned = wordsLearned; }

        public int getReviewsCompleted() { return reviewsCompleted; }
        public void setReviewsCompleted(int reviewsCompleted) { this.reviewsCompleted = reviewsCompleted; }
    }

    public static class TopWordData {
        private String word;
        private int frequency;
        private String meaning;

        public TopWordData(String word, int frequency, String meaning) {
            this.word = word;
            this.frequency = frequency;
            this.meaning = meaning;
        }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public int getFrequency() { return frequency; }
        public void setFrequency(int frequency) { this.frequency = frequency; }

        public String getMeaning() { return meaning; }
        public void setMeaning(String meaning) { this.meaning = meaning; }
    }

    @GetMapping("/learning-report")
    public ResponseEntity<LearningReportResponse> getLearningReport(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "period", defaultValue = "month") String period,
            @RequestParam(value = "language", required = false) String language) {

        // 打印接收到的请求
        Map<String, String> params = new HashMap<>();
        params.put("period", period);
        params.put("language", language);
        printRequest(params);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new LearningReportResponse(false, "请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 验证token有效性
            String tokenSql = "SELECT user_id, expires_at FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new LearningReportResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            int userId = (int) session.get("user_id");

            // 3. 验证周期参数
            String[] validPeriods = {"week", "month", "quarter", "year"};
            boolean validPeriod = false;
            for (String p : validPeriods) {
                if (p.equals(period)) {
                    validPeriod = true;
                    break;
                }
            }
            if (!validPeriod) {
                period = "month";
            }

            // 4. 根据周期计算日期范围
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate = now;
            LocalDateTime endDate = now;

            switch (period) {
                case "week":
                    startDate = now.minusDays(7);
                    break;
                case "month":
                    startDate = now.minusDays(30);
                    break;
                case "quarter":
                    startDate = now.minusDays(90);
                    break;
                case "year":
                    startDate = now.minusDays(365);
                    break;
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // 5. 构建报告数据
            ReportData report = new ReportData();
            report.setPeriod(period);
            report.setStartDate(startDate.format(dateFormatter));
            report.setEndDate(endDate.format(dateFormatter));
            report.setGeneratedAt(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // 6. 查询总结数据
            // 6.1 总阅读时间（秒）
            String readingTimeSql = "SELECT COALESCE(SUM(TIMESTAMPDIFF(SECOND, start_time, end_time)), 0) as total_seconds " +
                    "FROM reading_history WHERE user_id = ? AND created_at >= ? AND created_at <= ?";
            Map<String, Object> readingTimeResult = jdbcTemplate.queryForMap(readingTimeSql, userId, startDate, endDate);
            int totalReadingTime = ((Number) readingTimeResult.get("total_seconds")).intValue();

            // 6.2 已读文档数量
            String docsSql = "SELECT COUNT(DISTINCT document_id) as count FROM reading_history " +
                    "WHERE user_id = ? AND created_at >= ? AND created_at <= ?";
            Map<String, Object> docsResult = jdbcTemplate.queryForMap(docsSql, userId, startDate, endDate);
            int documentsRead = ((Number) docsResult.get("count")).intValue();

            // 6.3 已学单词数量（在这个时间段内添加的）
            String wordsSql = "SELECT COUNT(*) as count FROM user_vocabulary " +
                    "WHERE user_id = ? AND created_at >= ? AND created_at <= ?";
            Map<String, Object> wordsResult = jdbcTemplate.queryForMap(wordsSql, userId, startDate, endDate);
            int wordsLearned = ((Number) wordsResult.get("count")).intValue();

            // 6.4 已完成复习数量
            String reviewsSql = "SELECT COUNT(*) as count FROM review_sessions " +
                    "WHERE user_id = ? AND status = 'completed' AND created_at >= ? AND created_at <= ?";
            Map<String, Object> reviewsResult = jdbcTemplate.queryForMap(reviewsSql, userId, startDate, endDate);
            int reviewsCompleted = ((Number) reviewsResult.get("count")).intValue();

            SummaryData summary = new SummaryData(totalReadingTime, documentsRead, wordsLearned, reviewsCompleted);
            report.setSummary(summary);

            // 7. 查询每日数据
            List<DailyData> dailyBreakdown = new ArrayList<>();
            String dailySql = "SELECT DATE(created_at) as date, " +
                    "SUM(TIMESTAMPDIFF(SECOND, start_time, end_time)) as reading_time, " +
                    "COUNT(DISTINCT CASE WHEN type = 'vocabulary' THEN target_id END) as words_learned, " +
                    "COUNT(DISTINCT CASE WHEN type = 'review' THEN target_id END) as reviews_completed " +
                    "FROM reading_history " +
                    "WHERE user_id = ? AND created_at >= ? AND created_at <= ? " +
                    "GROUP BY DATE(created_at) ORDER BY date DESC";

            List<Map<String, Object>> dailyResults = jdbcTemplate.queryForList(dailySql, userId, startDate, endDate);

            for (Map<String, Object> daily : dailyResults) {
                String date = daily.get("date").toString();
                int readingTime = ((Number) daily.get("reading_time")).intValue();
                int wordsLearnedDaily = ((Number) daily.get("words_learned")).intValue();
                int reviewsCompletedDaily = ((Number) daily.get("reviews_completed")).intValue();

                dailyBreakdown.add(new DailyData(date, readingTime, wordsLearnedDaily, reviewsCompletedDaily));
            }

            report.setDailyBreakdown(dailyBreakdown);

            // 8. 查询热门单词
            List<TopWordData> topWords = new ArrayList<>();
            String topWordsSql = "SELECT w.word, COUNT(*) as frequency, wd.meaning " +
                    "FROM word_lookup_history wlh " +
                    "JOIN words w ON wlh.word_id = w.word_id " +
                    "LEFT JOIN word_definitions wd ON w.word_id = wd.word_id AND wd.is_primary = TRUE " +
                    "WHERE wlh.user_id = ? AND wlh.created_at >= ? AND wlh.created_at <= ? " +
                    "GROUP BY w.word_id, w.word, wd.meaning " +
                    "ORDER BY frequency DESC LIMIT 10";

            List<Map<String, Object>> topWordsResults = jdbcTemplate.queryForList(topWordsSql, userId, startDate, endDate);

            for (Map<String, Object> topWord : topWordsResults) {
                String word = (String) topWord.get("word");
                int frequency = ((Number) topWord.get("frequency")).intValue();
                String meaning = topWord.get("meaning") != null ? (String) topWord.get("meaning") : "";

                topWords.add(new TopWordData(word, frequency, meaning));
            }

            report.setTopWords(topWords);

            // 9. 阅读模式分析（简化处理）
            Map<String, Object> readingPatterns = new HashMap<>();

            // 9.1 最活跃的时间段
            String activeTimeSql = "SELECT HOUR(created_at) as hour, COUNT(*) as count " +
                    "FROM reading_history " +
                    "WHERE user_id = ? AND created_at >= ? AND created_at <= ? " +
                    "GROUP BY HOUR(created_at) ORDER BY count DESC LIMIT 1";

            List<Map<String, Object>> activeTimeResults = jdbcTemplate.queryForList(activeTimeSql, userId, startDate, endDate);
            if (!activeTimeResults.isEmpty()) {
                readingPatterns.put("mostActiveHour", activeTimeResults.get(0).get("hour"));
            }

            // 9.2 最常阅读的语言
            String languageSql = "SELECT d.language, COUNT(*) as count " +
                    "FROM reading_history rh " +
                    "JOIN documents d ON rh.document_id = d.document_id " +
                    "WHERE rh.user_id = ? AND rh.created_at >= ? AND rh.created_at <= ? " +
                    "GROUP BY d.language ORDER BY count DESC LIMIT 1";

            List<Map<String, Object>> languageResults = jdbcTemplate.queryForList(languageSql, userId, startDate, endDate);
            if (!languageResults.isEmpty()) {
                readingPatterns.put("mostReadLanguage", languageResults.get(0).get("language"));
            }

            report.setReadingPatterns(readingPatterns);

            // 10. 生成建议
            List<String> recommendations = new ArrayList<>();

            if (totalReadingTime < 3600) { // 少于1小时
                recommendations.add("建议增加每日阅读时间，目标每天至少阅读30分钟");
            }

            if (wordsLearned < 10) {
                recommendations.add("建议在学习过程中多标记生词，扩大词汇量");
            }

            if (reviewsCompleted < 5) {
                recommendations.add("定期复习已学单词，巩固记忆效果");
            }

            if (dailyBreakdown.size() < 7 && period.equals("week")) {
                recommendations.add("尝试保持每日学习的习惯，建立学习连续性");
            }

            report.setRecommendations(recommendations);

            printQueryResult(report);

            // 11. 准备响应数据
            LearningReportResponse response = new LearningReportResponse(true, "获取学习报告成功", report);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取学习报告过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new LearningReportResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
