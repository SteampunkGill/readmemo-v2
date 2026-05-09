package com.vue.readingapp.export;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/export")
public class GetExportStats {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到导出统计请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("====================");
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
    public static class GetExportStatsResponse {
        private boolean success = true;
        private String message = "查询成功";
        private ExportStatsData data;

        public GetExportStatsResponse(ExportStatsData data) {
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ExportStatsData getData() { return data; }
        public void setData(ExportStatsData data) { this.data = data; }
    }

    public static class ExportStatsData {
        private Map<String, Object> overall;
        private Map<String, Object> byType;
        private Map<String, Object> byFormat;
        private Map<String, Object> recentActivity;

        public ExportStatsData(Map<String, Object> overall, Map<String, Object> byType,
                               Map<String, Object> byFormat, Map<String, Object> recentActivity) {
            this.overall = overall;
            this.byType = byType;
            this.byFormat = byFormat;
            this.recentActivity = recentActivity;
        }

        public Map<String, Object> getOverall() { return overall; }
        public void setOverall(Map<String, Object> overall) { this.overall = overall; }

        public Map<String, Object> getByType() { return byType; }
        public void setByType(Map<String, Object> byType) { this.byType = byType; }

        public Map<String, Object> getByFormat() { return byFormat; }
        public void setByFormat(Map<String, Object> byFormat) { this.byFormat = byFormat; }

        public Map<String, Object> getRecentActivity() { return recentActivity; }
        public void setRecentActivity(Map<String, Object> recentActivity) { this.recentActivity = recentActivity; }
    }

    // 错误响应DTO
    public static class ErrorResponse {
        private boolean success = false;
        private ErrorDetail error;

        public ErrorResponse(String code, String message, Map<String, Object> details) {
            this.error = new ErrorDetail(code, message, details);
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public ErrorDetail getError() { return error; }
        public void setError(ErrorDetail error) { this.error = error; }

        public static class ErrorDetail {
            private String code;
            private String message;
            private Map<String, Object> details;

            public ErrorDetail(String code, String message, Map<String, Object> details) {
                this.code = code;
                this.message = message;
                this.details = details;
            }

            public String getCode() { return code; }
            public void setCode(String code) { this.code = code; }

            public String getMessage() { return message; }
            public void setMessage(String message) { this.message = message; }

            public Map<String, Object> getDetails() { return details; }
            public void setDetails(Map<String, Object> details) { this.details = details; }
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getExportStats(@RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest("查询导出统计");

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> details = new HashMap<>();
                details.put("auth", "缺少有效的认证令牌");
                ErrorResponse errorResponse = new ErrorResponse("UNAUTHORIZED", "未授权，需要重新登录", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            String token = authHeader.substring(7);

            // 验证token并获取用户ID
            String userSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(userSql, token);

            if (sessions.isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("token", "令牌无效或已过期");
                ErrorResponse errorResponse = new ErrorResponse("UNAUTHORIZED", "未授权，需要重新登录", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 查询总体统计
            Map<String, Object> overallStats = new HashMap<>();

            // 总导出次数
            String totalExportsSql = "SELECT COUNT(*) as total FROM sync_logs WHERE user_id = ? AND operation_type = 'EXPORT'";
            int totalExports = jdbcTemplate.queryForObject(totalExportsSql, Integer.class, userId);
            overallStats.put("total_exports", totalExports);

            // 最近30天导出次数
            String recentExportsSql = "SELECT COUNT(*) as recent FROM sync_logs WHERE user_id = ? AND operation_type = 'EXPORT' " +
                    "AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
            int recentExports = jdbcTemplate.queryForObject(recentExportsSql, Integer.class, userId);
            overallStats.put("recent_exports_30d", recentExports);

            // 总导出文件大小
            String totalSizeSql = "SELECT SUM(JSON_EXTRACT(details, '$.file_size')) as total_size FROM sync_logs " +
                    "WHERE user_id = ? AND operation_type = 'EXPORT'";
            Long totalSize = jdbcTemplate.queryForObject(totalSizeSql, Long.class, userId);
            overallStats.put("total_file_size", totalSize != null ? totalSize : 0);

            // 平均导出文件大小
            if (totalExports > 0 && totalSize != null) {
                overallStats.put("avg_file_size", totalSize / totalExports);
            } else {
                overallStats.put("avg_file_size", 0);
            }

            // 3. 按类型统计
            Map<String, Object> typeStats = new HashMap<>();

            String typeCountSql = "SELECT entity_type, COUNT(*) as count FROM sync_logs " +
                    "WHERE user_id = ? AND operation_type = 'EXPORT' " +
                    "GROUP BY entity_type ORDER BY count DESC";

            List<Map<String, Object>> typeCounts = jdbcTemplate.queryForList(typeCountSql, userId);

            for (Map<String, Object> typeCount : typeCounts) {
                String entityType = (String) typeCount.get("entity_type");
                long count = ((Number) typeCount.get("count")).longValue();
                typeStats.put(entityType, count);
            }

            // 4. 按格式统计
            Map<String, Object> formatStats = new HashMap<>();

            String formatCountSql = "SELECT JSON_EXTRACT(details, '$.format') as format, COUNT(*) as count FROM sync_logs " +
                    "WHERE user_id = ? AND operation_type = 'EXPORT' " +
                    "GROUP BY JSON_EXTRACT(details, '$.format') ORDER BY count DESC";

            List<Map<String, Object>> formatCounts = jdbcTemplate.queryForList(formatCountSql, userId);

            for (Map<String, Object> formatCount : formatCounts) {
                String format = (String) formatCount.get("format");
                if (format != null) {
                    long count = ((Number) formatCount.get("count")).longValue();
                    formatStats.put(format.replace("\"", ""), count);
                }
            }

            // 5. 最近活动统计
            Map<String, Object> recentActivity = new HashMap<>();

            // 最近7天导出趋势
            String weeklyTrendSql = "SELECT DATE(created_at) as date, COUNT(*) as count FROM sync_logs " +
                    "WHERE user_id = ? AND operation_type = 'EXPORT' " +
                    "AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                    "GROUP BY DATE(created_at) ORDER BY date";

            List<Map<String, Object>> weeklyTrend = jdbcTemplate.queryForList(weeklyTrendSql, userId);
            recentActivity.put("weekly_trend", weeklyTrend);

            // 最常导出的类型
            String topTypesSql = "SELECT entity_type, COUNT(*) as count FROM sync_logs " +
                    "WHERE user_id = ? AND operation_type = 'EXPORT' " +
                    "GROUP BY entity_type ORDER BY count DESC LIMIT 5";

            List<Map<String, Object>> topTypes = jdbcTemplate.queryForList(topTypesSql, userId);
            recentActivity.put("top_types", topTypes);

            // 最近10次导出
            String recentExportsSql2 = "SELECT log_id, operation_type, entity_type, status, details, created_at " +
                    "FROM sync_logs WHERE user_id = ? AND operation_type = 'EXPORT' " +
                    "ORDER BY created_at DESC LIMIT 10";

            List<Map<String, Object>> recentExportsList = jdbcTemplate.queryForList(recentExportsSql2, userId);
            recentActivity.put("recent_exports", recentExportsList);

            // 6. 构建响应数据
            ExportStatsData statsData = new ExportStatsData(overallStats, typeStats, formatStats, recentActivity);
            GetExportStatsResponse response = new GetExportStatsResponse(statsData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("查询导出统计过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("STATS_QUERY_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}