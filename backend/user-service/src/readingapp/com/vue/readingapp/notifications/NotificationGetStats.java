package com.vue.readingapp.notifications;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationGetStats {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取通知统计数据请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=============================");
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
    public static class GetStatsResponse {
        private boolean success;
        private String message;
        private NotificationStatsData stats;

        public GetStatsResponse(boolean success, String message, NotificationStatsData stats) {
            this.success = success;
            this.message = message;
            this.stats = stats;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public NotificationStatsData getStats() { return stats; }
        public void setStats(NotificationStatsData stats) { this.stats = stats; }
    }

    public static class NotificationStatsData {
        private int totalNotifications;
        private int unreadCount;
        private double readRate;
        private String formattedReadRate;
        private Map<String, Integer> byType;
        private Map<String, Integer> byDay;
        private double averageDailyNotifications;
        private String lastNotificationAt;

        public NotificationStatsData() {
            this.byType = new HashMap<>();
            this.byDay = new HashMap<>();
        }

        public int getTotalNotifications() { return totalNotifications; }
        public void setTotalNotifications(int totalNotifications) { this.totalNotifications = totalNotifications; }

        public int getUnreadCount() { return unreadCount; }
        public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

        public double getReadRate() { return readRate; }
        public void setReadRate(double readRate) { this.readRate = readRate; }

        public String getFormattedReadRate() { return formattedReadRate; }
        public void setFormattedReadRate(String formattedReadRate) { this.formattedReadRate = formattedReadRate; }

        public Map<String, Integer> getByType() { return byType; }
        public void setByType(Map<String, Integer> byType) { this.byType = byType; }

        public Map<String, Integer> getByDay() { return byDay; }
        public void setByDay(Map<String, Integer> byDay) { this.byDay = byDay; }

        public double getAverageDailyNotifications() { return averageDailyNotifications; }
        public void setAverageDailyNotifications(double averageDailyNotifications) { this.averageDailyNotifications = averageDailyNotifications; }

        public String getLastNotificationAt() { return lastNotificationAt; }
        public void setLastNotificationAt(String lastNotificationAt) { this.lastNotificationAt = lastNotificationAt; }
    }

    @GetMapping("/stats")
    public ResponseEntity<GetStatsResponse> getStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest("获取通知统计数据");

            // 1. 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new GetStatsResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 2. 根据token获取用户ID
            String getUserSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(getUserSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new GetStatsResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            Integer userId = (Integer) session.get("user_id");

            // 3. 查询总通知数量
            String totalSql = "SELECT COUNT(*) as total FROM notifications WHERE user_id = ?";
            Map<String, Object> totalResult = jdbcTemplate.queryForMap(totalSql, userId);
            long totalNotifications = ((Number) totalResult.get("total")).longValue();

            // 4. 查询未读通知数量
            String unreadSql = "SELECT COUNT(*) as unread FROM notifications WHERE user_id = ? AND is_read = false";
            Map<String, Object> unreadResult = jdbcTemplate.queryForMap(unreadSql, userId);
            long unreadCount = ((Number) unreadResult.get("unread")).longValue();

            // 5. 计算阅读率
            double readRate = 0.0;
            String formattedReadRate = "0%";
            if (totalNotifications > 0) {
                readRate = ((double) (totalNotifications - unreadCount) / totalNotifications) * 100;
                formattedReadRate = String.format("%.1f%%", readRate);
            }

            // 6. 按类型统计
            String byTypeSql = "SELECT type, COUNT(*) as count FROM notifications WHERE user_id = ? GROUP BY type";
            List<Map<String, Object>> byTypeResults = jdbcTemplate.queryForList(byTypeSql, userId);
            Map<String, Integer> byType = new HashMap<>();

            for (Map<String, Object> row : byTypeResults) {
                String type = (String) row.get("type");
                long count = ((Number) row.get("count")).longValue();
                byType.put(type, (int) count);
            }

            // 7. 按天统计（最近7天）
            String byDaySql = "SELECT DATE(created_at) as day, COUNT(*) as count FROM notifications WHERE user_id = ? AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY DATE(created_at) ORDER BY day";
            List<Map<String, Object>> byDayResults = jdbcTemplate.queryForList(byDaySql, userId);
            Map<String, Integer> byDay = new HashMap<>();

            for (Map<String, Object> row : byDayResults) {
                String day = row.get("day").toString();
                long count = ((Number) row.get("count")).longValue();
                byDay.put(day, (int) count);
            }

            // 8. 计算平均每日通知数
            double averageDailyNotifications = 0.0;
            if (totalNotifications > 0) {
                // 获取用户注册时间或第一个通知的时间
                String firstNotificationSql = "SELECT MIN(created_at) as first_notification FROM notifications WHERE user_id = ?";
                Map<String, Object> firstResult = jdbcTemplate.queryForMap(firstNotificationSql, userId);

                if (firstResult.get("first_notification") != null) {
                    LocalDateTime firstNotification = null;
                    if (firstResult.get("first_notification") instanceof java.sql.Timestamp) {
                        firstNotification = ((java.sql.Timestamp) firstResult.get("first_notification")).toLocalDateTime();
                    }

                    if (firstNotification != null) {
                        LocalDateTime now = LocalDateTime.now();
                        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(firstNotification, now);
                        if (daysBetween > 0) {
                            averageDailyNotifications = (double) totalNotifications / daysBetween;
                        }
                    }
                }
            }

            // 9. 获取最后通知时间
            String lastNotificationSql = "SELECT MAX(created_at) as last_notification FROM notifications WHERE user_id = ?";
            Map<String, Object> lastResult = jdbcTemplate.queryForMap(lastNotificationSql, userId);

            String lastNotificationAt = null;
            if (lastResult.get("last_notification") != null) {
                LocalDateTime lastNotification = null;
                if (lastResult.get("last_notification") instanceof java.sql.Timestamp) {
                    lastNotification = ((java.sql.Timestamp) lastResult.get("last_notification")).toLocalDateTime();
                }

                if (lastNotification != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                    lastNotificationAt = lastNotification.format(formatter) + "Z";
                }
            }

            // 10. 创建统计数据
            NotificationStatsData stats = new NotificationStatsData();
            stats.setTotalNotifications((int) totalNotifications);
            stats.setUnreadCount((int) unreadCount);
            stats.setReadRate(readRate);
            stats.setFormattedReadRate(formattedReadRate);
            stats.setByType(byType);
            stats.setByDay(byDay);
            stats.setAverageDailyNotifications(averageDailyNotifications);
            stats.setLastNotificationAt(lastNotificationAt);

            printQueryResult(stats);

            // 11. 创建响应
            GetStatsResponse response = new GetStatsResponse(
                    true,
                    "获取通知统计数据成功",
                    stats
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取通知统计数据过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetStatsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}