package com.vue.readingapp.review;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/review")
public class ReviewGetCalendar {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到获取复习日历请求 ===");
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

    public static class ReviewCalendarResponse {
        private boolean success;
        private String message;
        private CalendarData data;

        public ReviewCalendarResponse(boolean success, String message, CalendarData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public CalendarData getData() { return data; }
        public void setData(CalendarData data) { this.data = data; }
    }

    public static class CalendarData {
        private int year;
        private int month;
        private List<CalendarDay> days;
        private int total_reviews;
        private int average_daily_reviews;
        private int streak;
        private Map<String, Integer> heatmap_data;

        public CalendarData(int year, int month, List<CalendarDay> days, int total_reviews,
                            int average_daily_reviews, int streak, Map<String, Integer> heatmap_data) {
            this.year = year;
            this.month = month;
            this.days = days;
            this.total_reviews = total_reviews;
            this.average_daily_reviews = average_daily_reviews;
            this.streak = streak;
            this.heatmap_data = heatmap_data;
        }

        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }

        public int getMonth() { return month; }
        public void setMonth(int month) { this.month = month; }

        public List<CalendarDay> getDays() { return days; }
        public void setDays(List<CalendarDay> days) { this.days = days; }

        public int getTotal_reviews() { return total_reviews; }
        public void setTotal_reviews(int total_reviews) { this.total_reviews = total_reviews; }

        public int getAverage_daily_reviews() { return average_daily_reviews; }
        public void setAverage_daily_reviews(int average_daily_reviews) { this.average_daily_reviews = average_daily_reviews; }

        public int getStreak() { return streak; }
        public void setStreak(int streak) { this.streak = streak; }

        public Map<String, Integer> getHeatmap_data() { return heatmap_data; }
        public void setHeatmap_data(Map<String, Integer> heatmap_data) { this.heatmap_data = heatmap_data; }
    }

    public static class CalendarDay {
        private String date;
        private int review_count;
        private int word_count;
        private double accuracy;
        private boolean has_review;

        public CalendarDay(String date, int review_count, int word_count, double accuracy, boolean has_review) {
            this.date = date;
            this.review_count = review_count;
            this.word_count = word_count;
            this.accuracy = accuracy;
            this.has_review = has_review;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public int getReview_count() { return review_count; }
        public void setReview_count(int review_count) { this.review_count = review_count; }

        public int getWord_count() { return word_count; }
        public void setWord_count(int word_count) { this.word_count = word_count; }

        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

        public boolean isHas_review() { return has_review; }
        public void setHas_review(boolean has_review) { this.has_review = has_review; }
    }

    @GetMapping("/calendar")
    public ResponseEntity<ReviewCalendarResponse> getReviewCalendar(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "year", required = false) Integer yearParam,
            @RequestParam(value = "month", required = false) Integer monthParam) {

        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("year", yearParam);
        requestParams.put("month", monthParam);
        printRequest(requestParams);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReviewCalendarResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReviewCalendarResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) users.get(0).get("user_id");
            System.out.println("当前用户ID: " + userId);

            // 2. 确定年份和月份
            LocalDateTime now = LocalDateTime.now();
            int year = yearParam != null ? yearParam : now.getYear();
            int month = monthParam != null ? monthParam : now.getMonthValue();

            // 验证年份和月份
            if (year < 2000 || year > 2100) year = now.getYear();
            if (month < 1 || month > 12) month = now.getMonthValue();

            // 3. 获取该月的所有日期
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDate firstDay = yearMonth.atDay(1);
            LocalDate lastDay = yearMonth.atEndOfMonth();

            // 4. 查询该月的复习数据
            String reviewSql = "SELECT DATE(completed_at) as review_date, " +
                    "COUNT(*) as session_count, " +
                    "SUM(total_words) as total_words, " +
                    "COALESCE(AVG(accuracy), 0) as avg_accuracy " +
                    "FROM review_sessions " +
                    "WHERE user_id = ? AND DATE(completed_at) BETWEEN ? AND ? " +
                    "GROUP BY DATE(completed_at)";

            List<Map<String, Object>> reviewData = jdbcTemplate.queryForList(
                    reviewSql, userId, firstDay.toString(), lastDay.toString());

            printQueryResult("查询到 " + reviewData.size() + " 天的复习数据");

            // 5. 构建日期数据映射
            Map<String, Map<String, Object>> dateDataMap = new HashMap<>();
            for (Map<String, Object> row : reviewData) {
                String date = ((java.sql.Date) row.get("review_date")).toString();
                dateDataMap.put(date, row);
            }

            // 6. 构建日历天数数据
            List<CalendarDay> days = new ArrayList<>();
            LocalDate currentDate = firstDay;
            int totalReviews = 0;
            int daysWithReviews = 0;

            while (!currentDate.isAfter(lastDay)) {
                String dateStr = currentDate.toString();
                Map<String, Object> dayData = dateDataMap.get(dateStr);

                int reviewCount = 0;
                int wordCount = 0;
                double accuracy = 0.0;
                boolean hasReview = false;

                if (dayData != null) {
                    reviewCount = ((Number) dayData.get("session_count")).intValue();
                    wordCount = ((Number) dayData.get("total_words")).intValue();
                    accuracy = ((Number) dayData.get("avg_accuracy")).doubleValue();
                    hasReview = true;

                    totalReviews += reviewCount;
                    daysWithReviews++;
                }

                CalendarDay day = new CalendarDay(
                        dateStr,
                        reviewCount,
                        wordCount,
                        Math.round(accuracy * 100.0) / 100.0,
                        hasReview
                );

                days.add(day);
                currentDate = currentDate.plusDays(1);
            }

            // 7. 计算平均每日复习次数
            int averageDailyReviews = daysWithReviews > 0 ? totalReviews / daysWithReviews : 0;

            // 8. 获取连续学习天数
            int streak = calculateStreakDays(userId);

            // 9. 构建热力图数据
            Map<String, Integer> heatmapData = new HashMap<>();
            for (CalendarDay day : days) {
                heatmapData.put(day.getDate(), day.getWord_count());
            }

            // 10. 构建响应数据
            CalendarData data = new CalendarData(
                    year,
                    month,
                    days,
                    totalReviews,
                    averageDailyReviews,
                    streak,
                    heatmapData
            );

            ReviewCalendarResponse response = new ReviewCalendarResponse(true, "获取复习日历成功", data);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取复习日历过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReviewCalendarResponse(false, "服务器内部错误: " + e.getMessage(), null)
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
