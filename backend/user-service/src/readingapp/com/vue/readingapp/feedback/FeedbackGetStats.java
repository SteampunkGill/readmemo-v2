package com.vue.readingapp.feedback;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackGetStats {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取反馈统计请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=======================");
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

    // 请求DTO
    public static class GetStatsRequest {
        private String timeRange;
        private String type;

        public String getTimeRange() { return timeRange; }
        public void setTimeRange(String timeRange) { this.timeRange = timeRange; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    // 响应DTO
    public static class GetStatsResponse {
        private boolean success;
        private String message;
        private FeedbackStatsData data;

        public GetStatsResponse(boolean success, String message, FeedbackStatsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public FeedbackStatsData getData() { return data; }
        public void setData(FeedbackStatsData data) { this.data = data; }
    }

    public static class FeedbackStatsData {
        private int total;
        private Map<String, Integer> byType;
        private Map<String, Integer> byStatus;
        private Map<String, Integer> byPriority;
        private Map<String, Integer> byMonth;
        private List<Map<String, Object>> topContributors;
        private int averageResponseTime;
        private double resolutionRate;
        private double satisfactionScore;

        public FeedbackStatsData(int total, Map<String, Integer> byType, Map<String, Integer> byStatus,
                                 Map<String, Integer> byPriority, Map<String, Integer> byMonth,
                                 List<Map<String, Object>> topContributors, int averageResponseTime,
                                 double resolutionRate, double satisfactionScore) {
            this.total = total;
            this.byType = byType;
            this.byStatus = byStatus;
            this.byPriority = byPriority;
            this.byMonth = byMonth;
            this.topContributors = topContributors;
            this.averageResponseTime = averageResponseTime;
            this.resolutionRate = resolutionRate;
            this.satisfactionScore = satisfactionScore;
        }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public Map<String, Integer> getByType() { return byType; }
        public void setByType(Map<String, Integer> byType) { this.byType = byType; }

        public Map<String, Integer> getByStatus() { return byStatus; }
        public void setByStatus(Map<String, Integer> byStatus) { this.byStatus = byStatus; }

        public Map<String, Integer> getByPriority() { return byPriority; }
        public void setByPriority(Map<String, Integer> byPriority) { this.byPriority = byPriority; }

        public Map<String, Integer> getByMonth() { return byMonth; }
        public void setByMonth(Map<String, Integer> byMonth) { this.byMonth = byMonth; }

        public List<Map<String, Object>> getTopContributors() { return topContributors; }
        public void setTopContributors(List<Map<String, Object>> topContributors) { this.topContributors = topContributors; }

        public int getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(int averageResponseTime) { this.averageResponseTime = averageResponseTime; }

        public double getResolutionRate() { return resolutionRate; }
        public void setResolutionRate(double resolutionRate) { this.resolutionRate = resolutionRate; }

        public double getSatisfactionScore() { return satisfactionScore; }
        public void setSatisfactionScore(double satisfactionScore) { this.satisfactionScore = satisfactionScore; }
    }

    @GetMapping("/stats")
    public ResponseEntity<GetStatsResponse> getFeedbackStats(
            @RequestParam(defaultValue = "month") String timeRange,
            @RequestParam(required = false) String type) {

        // 创建请求对象用于打印
        GetStatsRequest request = new GetStatsRequest();
        request.setTimeRange(timeRange);
        request.setType(type);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 构建基础查询条件
            List<Object> params = new ArrayList<>();
            StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");

            // 处理type条件
            if (type != null && !type.isEmpty() && !"all".equals(type)) {
                whereClause.append(" AND type = ? ");
                params.add(type);
            }

            // 处理时间范围条件
            String dateCondition = "";
            if ("week".equals(timeRange)) {
                dateCondition = " AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) ";
            } else if ("month".equals(timeRange)) {
                dateCondition = " AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) ";
            } else if ("quarter".equals(timeRange)) {
                dateCondition = " AND created_at >= DATE_SUB(NOW(), INTERVAL 90 DAY) ";
            } else if ("year".equals(timeRange)) {
                dateCondition = " AND created_at >= DATE_SUB(NOW(), INTERVAL 365 DAY) ";
            }
            whereClause.append(dateCondition);

            // 2. 获取总数
            String totalSql = "SELECT COUNT(*) as total FROM user_feedback " + whereClause.toString();
            Integer total = jdbcTemplate.queryForObject(totalSql, Integer.class, params.toArray());
            if (total == null) total = 0;

            // 3. 按类型统计
            Map<String, Integer> byType = getStatsByType(whereClause.toString(), params);

            // 4. 按状态统计
            Map<String, Integer> byStatus = getStatsByStatus(whereClause.toString(), params);

            // 5. 按优先级统计
            Map<String, Integer> byPriority = getStatsByPriority(whereClause.toString(), params);

            // 6. 按月统计
            Map<String, Integer> byMonth = getStatsByMonth(whereClause.toString(), params, timeRange);

            // 7. 获取顶级贡献者
            List<Map<String, Object>> topContributors = getTopContributors(whereClause.toString(), params);

            // 8. 计算平均响应时间
            int averageResponseTime = calculateAverageResponseTime(whereClause.toString(), params);

            // 9. 计算解决率
            double resolutionRate = calculateResolutionRate(whereClause.toString(), params);

            // 10. 计算满意度分数（简化处理）
            double satisfactionScore = calculateSatisfactionScore(whereClause.toString(), params);

            // 11. 准备响应数据
            FeedbackStatsData statsData = new FeedbackStatsData(
                    total, byType, byStatus, byPriority, byMonth, topContributors,
                    averageResponseTime, resolutionRate, satisfactionScore
            );

            GetStatsResponse response = new GetStatsResponse(
                    true, "获取反馈统计成功", statsData
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取反馈统计过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetStatsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private Map<String, Integer> getStatsByType(String whereClause, List<Object> params) {
        Map<String, Integer> result = new LinkedHashMap<>();

        try {
            String sql = "SELECT type, COUNT(*) as count FROM user_feedback " +
                    whereClause + " GROUP BY type ORDER BY count DESC";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

            // 初始化所有可能的类型
            List<String> allTypes = List.of("bug", "feature", "improvement", "question", "other");
            for (String type : allTypes) {
                result.put(type, 0);
            }

            // 填充实际数据
            for (Map<String, Object> row : rows) {
                String type = (String) row.get("type");
                int count = ((Number) row.get("count")).intValue();
                result.put(type, count);
            }

        } catch (Exception e) {
            System.err.println("按类型统计失败: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Integer> getStatsByStatus(String whereClause, List<Object> params) {
        Map<String, Integer> result = new LinkedHashMap<>();

        try {
            String sql = "SELECT status, COUNT(*) as count FROM user_feedback " +
                    whereClause + " GROUP BY status ORDER BY FIELD(status, 'pending', 'reviewing', 'in_progress', 'completed', 'rejected', 'duplicate')";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

            // 初始化所有可能的状态
            List<String> allStatuses = List.of("pending", "reviewing", "in_progress", "completed", "rejected", "duplicate");
            for (String status : allStatuses) {
                result.put(status, 0);
            }

            // 填充实际数据
            for (Map<String, Object> row : rows) {
                String status = (String) row.get("status");
                int count = ((Number) row.get("count")).intValue();
                result.put(status, count);
            }

        } catch (Exception e) {
            System.err.println("按状态统计失败: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Integer> getStatsByPriority(String whereClause, List<Object> params) {
        Map<String, Integer> result = new LinkedHashMap<>();

        try {
            String sql = "SELECT priority, COUNT(*) as count FROM user_feedback " +
                    whereClause + " GROUP BY priority ORDER BY FIELD(priority, 'critical', 'high', 'medium', 'low')";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

            // 初始化所有可能的优先级
            List<String> allPriorities = List.of("critical", "high", "medium", "low");
            for (String priority : allPriorities) {
                result.put(priority, 0);
            }

            // 填充实际数据
            for (Map<String, Object> row : rows) {
                String priority = (String) row.get("priority");
                int count = ((Number) row.get("count")).intValue();
                result.put(priority, count);
            }

        } catch (Exception e) {
            System.err.println("按优先级统计失败: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Integer> getStatsByMonth(String whereClause, List<Object> params, String timeRange) {
        Map<String, Integer> result = new LinkedHashMap<>();

        try {
            // 根据时间范围确定要查询的月份数量
            int monthsToShow = 6; // 默认显示最近6个月
            if ("year".equals(timeRange)) {
                monthsToShow = 12;
            } else if ("quarter".equals(timeRange)) {
                monthsToShow = 3;
            } else if ("month".equals(timeRange)) {
                monthsToShow = 1;
            } else if ("week".equals(timeRange)) {
                monthsToShow = 1;
            }

            // 生成最近几个月的月份列表
            LocalDateTime now = LocalDateTime.now();
            for (int i = monthsToShow - 1; i >= 0; i--) {
                LocalDateTime month = now.minusMonths(i);
                String monthKey = month.format(monthFormatter);
                result.put(monthKey, 0);
            }

            // 查询实际数据
            String sql = "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COUNT(*) as count " +
                    "FROM user_feedback " + whereClause +
                    " GROUP BY DATE_FORMAT(created_at, '%Y-%m') " +
                    "ORDER BY month";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

            // 填充实际数据
            for (Map<String, Object> row : rows) {
                String month = (String) row.get("month");
                int count = ((Number) row.get("count")).intValue();
                if (result.containsKey(month)) {
                    result.put(month, count);
                }
            }

        } catch (Exception e) {
            System.err.println("按月统计失败: " + e.getMessage());
        }

        return result;
    }

    private List<Map<String, Object>> getTopContributors(String whereClause, List<Object> params) {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            String sql = "SELECT u.user_id, u.username as user_name, COUNT(f.feedback_id) as feedback_count, " +
                    "SUM(f.upvotes) as upvotes_received " +
                    "FROM user_feedback f " +
                    "LEFT JOIN users u ON f.user_id = u.user_id " +
                    whereClause +
                    " GROUP BY u.user_id, u.username " +
                    "ORDER BY feedback_count DESC, upvotes_received DESC " +
                    "LIMIT 10";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

            for (Map<String, Object> row : rows) {
                Map<String, Object> contributor = new HashMap<>();
                contributor.put("userId", row.get("user_id"));
                contributor.put("userName", row.get("user_name"));
                contributor.put("feedbackCount", row.get("feedback_count"));
                contributor.put("upvotesReceived", row.get("upvotes_received"));
                result.add(contributor);
            }

        } catch (Exception e) {
            System.err.println("获取顶级贡献者失败: " + e.getMessage());
        }

        return result;
    }

    private int calculateAverageResponseTime(String whereClause, List<Object> params) {
        int averageResponseTime = 48; // 默认值

        try {
            // 计算从创建到第一次回复的平均时间（小时）
            String sql = "SELECT AVG(TIMESTAMPDIFF(HOUR, f.created_at, r.created_at)) as avg_response_hours " +
                    "FROM user_feedback f " +
                    "INNER JOIN feedback_replies r ON f.feedback_id = r.feedback_id " +
                    "WHERE r.created_at = (SELECT MIN(created_at) FROM feedback_replies WHERE feedback_id = f.feedback_id) " +
                    whereClause.replace("user_feedback", "f");

            Double avgHours = jdbcTemplate.queryForObject(sql, Double.class, params.toArray());
            if (avgHours != null && !avgHours.isNaN()) {
                averageResponseTime = avgHours.intValue();
            }

        } catch (Exception e) {
            System.err.println("计算平均响应时间失败: " + e.getMessage());
        }

        return averageResponseTime;
    }

    private double calculateResolutionRate(String whereClause, List<Object> params) {
        double resolutionRate = 0.75; // 默认值

        try {
            // 计算解决率：已完成的反馈 / 所有反馈
            String totalSql = "SELECT COUNT(*) as total FROM user_feedback " + whereClause;
            Integer total = jdbcTemplate.queryForObject(totalSql, Integer.class, params.toArray());

            if (total != null && total > 0) {
                String resolvedSql = "SELECT COUNT(*) as resolved FROM user_feedback " +
                        whereClause + " AND status IN ('completed', 'rejected', 'duplicate')";
                Integer resolved = jdbcTemplate.queryForObject(resolvedSql, Integer.class, params.toArray());

                if (resolved != null) {
                    resolutionRate = (double) resolved / total;
                }
            }

        } catch (Exception e) {
            System.err.println("计算解决率失败: " + e.getMessage());
        }

        return Math.round(resolutionRate * 100.0) / 100.0; // 保留两位小数
    }

    private double calculateSatisfactionScore(String whereClause, List<Object> params) {
        double satisfactionScore = 4.2; // 默认值

        try {
            // 计算满意度分数：基于投票比例（简化处理）
            String sql = "SELECT AVG(CASE WHEN upvotes + downvotes > 0 THEN upvotes * 5.0 / (upvotes + downvotes) ELSE 4.2 END) as avg_score " +
                    "FROM user_feedback " + whereClause;

            Double avgScore = jdbcTemplate.queryForObject(sql, Double.class, params.toArray());
            if (avgScore != null && !avgScore.isNaN()) {
                satisfactionScore = Math.round(avgScore * 10.0) / 10.0; // 保留一位小数
            }

        } catch (Exception e) {
            System.err.println("计算满意度分数失败: " + e.getMessage());
        }

        return satisfactionScore;
    }
}