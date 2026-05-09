package com.vue.readingapp.user;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/user")
public class UserGetLearningCalendar {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取学习日历请求 ===");
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
    public static class LearningCalendarResponse {
        private boolean success;
        private String message;
        private CalendarData calendar;

        public LearningCalendarResponse(boolean success, String message, CalendarData calendar) {
            this.success = success;
            this.message = message;
            this.calendar = calendar;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public CalendarData getCalendar() { return calendar; }
        public void setCalendar(CalendarData calendar) { this.calendar = calendar; }
    }

    public static class CalendarData {
        private int year;
        private int month;
        private List<DayData> days;
        private int totalStudyDays;
        private int longestStreak;
        private int currentStreak;
        private Map<String, Integer> heatmapData;
        private MonthlyStats monthlyStats;

        // Getters and Setters
        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }

        public int getMonth() { return month; }
        public void setMonth(int month) { this.month = month; }

        public List<DayData> getDays() { return days; }
        public void setDays(List<DayData> days) { this.days = days; }

        public int getTotalStudyDays() { return totalStudyDays; }
        public void setTotalStudyDays(int totalStudyDays) { this.totalStudyDays = totalStudyDays; }

        public int getLongestStreak() { return longestStreak; }
        public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }

        public int getCurrentStreak() { return currentStreak; }
        public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

        public Map<String, Integer> getHeatmapData() { return heatmapData; }
        public void setHeatmapData(Map<String, Integer> heatmapData) { this.heatmapData = heatmapData; }

        public MonthlyStats getMonthlyStats() { return monthlyStats; }
        public void setMonthlyStats(MonthlyStats monthlyStats) { this.monthlyStats = monthlyStats; }
    }

    public static class DayData {
        private String date;
        private int readingTime;
        private int wordsLearned;
        private int reviewsCompleted;
        private boolean hasStudyData;

        public DayData(String date, int readingTime, int wordsLearned, int reviewsCompleted, boolean hasStudyData) {
            this.date = date;
            this.readingTime = readingTime;
            this.wordsLearned = wordsLearned;
            this.reviewsCompleted = reviewsCompleted;
            this.hasStudyData = hasStudyData;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public int getReadingTime() { return readingTime; }
        public void setReadingTime(int readingTime) { this.readingTime = readingTime; }

        public int getWordsLearned() { return wordsLearned; }
        public void setWordsLearned(int wordsLearned) { this.wordsLearned = wordsLearned; }

        public int getReviewsCompleted() { return reviewsCompleted; }
        public void setReviewsCompleted(int reviewsCompleted) { this.reviewsCompleted = reviewsCompleted; }

        public boolean isHasStudyData() { return hasStudyData; }
        public void setHasStudyData(boolean hasStudyData) { this.hasStudyData = hasStudyData; }
    }

    public static class MonthlyStats {
        private int totalReadingTime;
        private int documentsRead;
        private int wordsLearned;

        public MonthlyStats(int totalReadingTime, int documentsRead, int wordsLearned) {
            this.totalReadingTime = totalReadingTime;
            this.documentsRead = documentsRead;
            this.wordsLearned = wordsLearned;
        }

        public int getTotalReadingTime() { return totalReadingTime; }
        public void setTotalReadingTime(int totalReadingTime) { this.totalReadingTime = totalReadingTime; }

        public int getDocumentsRead() { return documentsRead; }
        public void setDocumentsRead(int documentsRead) { this.documentsRead = documentsRead; }

        public int getWordsLearned() { return wordsLearned; }
        public void setWordsLearned(int wordsLearned) { this.wordsLearned = wordsLearned; }
    }

    @GetMapping("/learning-calendar")
    public ResponseEntity<LearningCalendarResponse> getUserLearningCalendar(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month) {

        // 打印接收到的请求
        Map<String, Object> params = new HashMap<>();
        params.put("year", year);
        params.put("month", month);
        printRequest(params);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new LearningCalendarResponse(false, "请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 验证token有效性
            String tokenSql = "SELECT user_id, expires_at FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new LearningCalendarResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            int userId = (int) session.get("user_id");

            // 3. 验证参数
            LocalDateTime now = LocalDateTime.now();
            if (year == null || year < 2000 || year > 2100) {
                year = now.getYear();
            }

            if (month == null || month < 1 || month > 12) {
                month = now.getMonthValue();
            }

            // 4. 计算日期范围
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);

            // 5. 查询每日学习数据
            String dailySql = "SELECT DATE(created_at) as date, " +
                    "SUM(TIMESTAMPDIFF(SECOND, start_time, end_time)) as reading_time, " +
                    "COUNT(DISTINCT CASE WHEN type = 'vocabulary' THEN target_id END) as words_learned, " +
                    "COUNT(DISTINCT CASE WHEN type = 'review' THEN target_id END) as reviews_completed " +
                    "FROM reading_history " +
                    "WHERE user_id = ? AND created_at >= ? AND created_at <= ? " +
                    "GROUP BY DATE(created_at) ORDER BY date";

            List<Map<String, Object>> dailyResults = jdbcTemplate.queryForList(
                    dailySql, userId, startDate, endDate);

            // 6. 构建每日数据
            List<DayData> days = new ArrayList<>();
            Map<String, Integer> heatmapData = new HashMap<>();
            int totalStudyDays = 0;

            // 获取该月的天数
            int daysInMonth = yearMonth.lengthOfMonth();

            for (int day = 1; day <= daysInMonth; day++) {
                String dateStr = String.format("%04d-%02d-%02d", year, month, day);

                // 查找当天的数据
                int readingTime = 0;
                int wordsLearned = 0;
                int reviewsCompleted = 0;
                boolean hasStudyData = false;

                for (Map<String, Object> daily : dailyResults) {
                    String resultDate = daily.get("date").toString();
                    if (resultDate.equals(dateStr)) {
                        readingTime = ((Number) daily.get("reading_time")).intValue();
                        wordsLearned = ((Number) daily.get("words_learned")).intValue();
                        reviewsCompleted = ((Number) daily.get("reviews_completed")).intValue();
                        hasStudyData = true;
                        totalStudyDays++;
                        break;
                    }
                }

                // 添加热力图数据（使用阅读时间作为热力值）
                heatmapData.put(dateStr, readingTime);

                days.add(new DayData(dateStr, readingTime, wordsLearned, reviewsCompleted, hasStudyData));
            }

            // 7. 查询连续学习天数
            int currentStreak = calculateCurrentStreak(userId);
            int longestStreak = calculateLongestStreak(userId);

            // 8. 查询月度统计
            String monthlyStatsSql = "SELECT " +
                    "COALESCE(SUM(TIMESTAMPDIFF(SECOND, start_time, end_time)), 0) as total_reading_time, " +
                    "COUNT(DISTINCT document_id) as documents_read, " +
                    "COUNT(DISTINCT CASE WHEN type = 'vocabulary' THEN target_id END) as words_learned " +
                    "FROM reading_history " +
                    "WHERE user_id = ? AND created_at >= ? AND created_at <= ?";

            Map<String, Object> monthlyStatsResult = jdbcTemplate.queryForMap(
                    monthlyStatsSql, userId, startDate, endDate);

            MonthlyStats monthlyStats = new MonthlyStats(
                    ((Number) monthlyStatsResult.get("total_reading_time")).intValue(),
                    ((Number) monthlyStatsResult.get("documents_read")).intValue(),
                    ((Number) monthlyStatsResult.get("words_learned")).intValue()
            );

            // 9. 构建日历数据
            CalendarData calendar = new CalendarData();
            calendar.setYear(year);
            calendar.setMonth(month);
            calendar.setDays(days);
            calendar.setTotalStudyDays(totalStudyDays);
            calendar.setLongestStreak(longestStreak);
            calendar.setCurrentStreak(currentStreak);
            calendar.setHeatmapData(heatmapData);
            calendar.setMonthlyStats(monthlyStats);

            printQueryResult(calendar);

            // 10. 准备响应数据
            LearningCalendarResponse response = new LearningCalendarResponse(
                    true, "获取学习日历成功", calendar);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取学习日历过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new LearningCalendarResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 计算当前连续学习天数
    private int calculateCurrentStreak(int userId) {
        try {
            String streakSql = "SELECT COUNT(DISTINCT DATE(created_at)) as streak " +
                    "FROM reading_history " +
                    "WHERE user_id = ? AND created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
                    "AND created_at <= CURDATE() " +
                    "AND TIMESTAMPDIFF(SECOND, start_time, end_time) > 0 " +
                    "ORDER BY DATE(created_at) DESC";

            Map<String, Object> streakResult = jdbcTemplate.queryForMap(streakSql, userId);
            return ((Number) streakResult.get("streak")).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    // 计算最长连续学习天数
    private int calculateLongestStreak(int userId) {
        try {
            String longestStreakSql = "SELECT MAX(streak_days) as longest_streak " +
                    "FROM daily_learning_stats WHERE user_id = ?";

            Map<String, Object> longestStreakResult = jdbcTemplate.queryForMap(longestStreakSql, userId);
            return ((Number) longestStreakResult.get("longest_streak")).intValue();
        } catch (Exception e) {
            return 0;
        }
    }
}
