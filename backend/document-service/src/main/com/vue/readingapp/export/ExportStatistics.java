package com.vue.readingapp.export;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/export")
public class ExportStatistics {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到学习统计导出请求 ===");
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
    public static class ExportStatisticsRequest {
        private String format = "pdf";
        private Map<String, Object> template;
        private String date_range = "all";
        private boolean include_charts = true;

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public Map<String, Object> getTemplate() { return template; }
        public void setTemplate(Map<String, Object> template) { this.template = template; }

        public String getDate_range() { return date_range; }
        public void setDate_range(String date_range) { this.date_range = date_range; }

        public boolean isInclude_charts() { return include_charts; }
        public void setInclude_charts(boolean include_charts) { this.include_charts = include_charts; }
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

    // 支持的格式
    private static final Set<String> SUPPORTED_FORMATS = new HashSet<>(Arrays.asList(
            "pdf", "xlsx", "csv", "json"
    ));

    @GetMapping("/statistics")
    public ResponseEntity<?> exportStatistics(@RequestParam(required = false) String format,
                                              @RequestParam(required = false) String date_range,
                                              @RequestParam(required = false, defaultValue = "true") boolean include_charts,
                                              @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 构建请求对象
        ExportStatisticsRequest request = new ExportStatisticsRequest();
        if (format != null) request.setFormat(format);
        if (date_range != null) request.setDate_range(date_range);
        request.setInclude_charts(include_charts);

        // 打印接收到的请求
        printRequest(request);

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

            // 2. 验证请求参数
            if (!SUPPORTED_FORMATS.contains(request.getFormat().toLowerCase())) {
                Map<String, Object> details = new HashMap<>();
                details.put("format", "不支持的导出格式: " + request.getFormat());
                details.put("supported_formats", SUPPORTED_FORMATS);
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 3. 查询学习统计数据
            Map<String, Object> statistics = new HashMap<>();

            // 3.1 查询每日学习统计
            String dailyStatsSql = "SELECT date, total_study_time, words_studied, documents_read, " +
                    "notes_created, highlights_created FROM daily_learning_stats " +
                    "WHERE user_id = ? ";

            List<Object> params = new ArrayList<>();
            params.add(userId);

            // 根据日期范围过滤
            if (!"all".equals(request.getDate_range())) {
                String[] dateRange = request.getDate_range().split(",");
                if (dateRange.length == 2) {
                    dailyStatsSql += "AND date BETWEEN ? AND ? ";
                    params.add(dateRange[0].trim());
                    params.add(dateRange[1].trim());
                }
            }

            dailyStatsSql += "ORDER BY date DESC LIMIT 30";

            List<Map<String, Object>> dailyStats = jdbcTemplate.queryForList(dailyStatsSql, params.toArray());
            statistics.put("daily_stats", dailyStats);
            printQueryResult("每日学习统计: " + dailyStats.size() + "条记录");

            // 3.2 查询词汇掌握统计
            String vocabStatsSql = "SELECT mastery_level, COUNT(*) as count FROM user_vocabulary " +
                    "WHERE user_id = ? GROUP BY mastery_level ORDER BY mastery_level";
            List<Map<String, Object>> vocabStats = jdbcTemplate.queryForList(vocabStatsSql, userId);
            statistics.put("vocabulary_stats", vocabStats);

            // 3.3 查询学习成就
            String achievementsSql = "SELECT a.achievement_id, a.name, a.description, a.icon_url, " +
                    "a.points, ua.unlocked_at FROM user_achievements ua " +
                    "JOIN learning_achievements a ON ua.achievement_id = a.achievement_id " +
                    "WHERE ua.user_id = ? ORDER BY ua.unlocked_at DESC";
            List<Map<String, Object>> achievements = jdbcTemplate.queryForList(achievementsSql, userId);
            statistics.put("achievements", achievements);

            // 3.4 查询复习统计
            String reviewStatsSql = "SELECT COUNT(*) as total_sessions, SUM(total_items) as total_items, " +
                    "SUM(correct_count) as total_correct, AVG(accuracy) as avg_accuracy " +
                    "FROM review_sessions WHERE user_id = ?";
            List<Map<String, Object>> reviewStats = jdbcTemplate.queryForList(reviewStatsSql, userId);
            statistics.put("review_stats", reviewStats);

            // 3.5 查询阅读统计
            String readingStatsSql = "SELECT COUNT(*) as total_documents, SUM(page_count) as total_pages, " +
                    "AVG(reading_progress) as avg_progress FROM documents " +
                    "WHERE user_id = ?";
            List<Map<String, Object>> readingStats = jdbcTemplate.queryForList(readingStatsSql, userId);
            statistics.put("reading_stats", readingStats);

            // 3.6 查询学习趋势（最近7天）
            String trendSql = "SELECT date, words_studied FROM daily_learning_stats " +
                    "WHERE user_id = ? AND date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
                    "ORDER BY date";
            List<Map<String, Object>> learningTrend = jdbcTemplate.queryForList(trendSql, userId);
            statistics.put("learning_trend", learningTrend);

            // 4. 生成导出文件内容
            String fileContent = generateExportContent(statistics, request.getFormat(), request.isInclude_charts());
            byte[] fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);

            // 5. 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String extension = getFileExtension(request.getFormat());
            String filename = "learning_statistics_" + timestamp + "." + extension;

            // 6. 记录导出历史
            String exportId = "export_stats_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String insertHistorySql = "INSERT INTO sync_logs (user_id, operation_type, entity_type, " +
                    "status, details, created_at) VALUES (?, ?, ?, ?, ?, ?)";

            Map<String, Object> exportDetails = new HashMap<>();
            exportDetails.put("export_id", exportId);
            exportDetails.put("format", request.getFormat());
            exportDetails.put("date_range", request.getDate_range());
            exportDetails.put("include_charts", request.isInclude_charts());
            exportDetails.put("filename", filename);
            exportDetails.put("file_size", fileBytes.length);

            String detailsJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_OBJECT(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", String.class,
                    "export_id", exportId,
                    "format", request.getFormat(),
                    "date_range", request.getDate_range(),
                    "include_charts", request.isInclude_charts(),
                    "filename", filename,
                    "file_size", fileBytes.length
            );

            jdbcTemplate.update(insertHistorySql,
                    userId,
                    "EXPORT",
                    "STATISTICS",
                    "COMPLETED",
                    detailsJson,
                    LocalDateTime.now()
            );

            // 7. 准备文件响应
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(getMediaType(request.getFormat()));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(fileBytes.length);

            // 打印返回信息
            Map<String, Object> responseInfo = new HashMap<>();
            responseInfo.put("filename", filename);
            responseInfo.put("file_size", fileBytes.length);
            responseInfo.put("format", request.getFormat());
            responseInfo.put("date_range", request.getDate_range());
            responseInfo.put("include_charts", request.isInclude_charts());
            printResponse(responseInfo);

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("学习统计导出过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("STATISTICS_EXPORT_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 生成导出内容
    private String generateExportContent(Map<String, Object> statistics, String format, boolean includeCharts) {
        StringBuilder content = new StringBuilder();

        if (format.equalsIgnoreCase("json")) {
            // JSON格式
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("metadata", new HashMap<String, Object>() {{
                put("exportedAt", LocalDateTime.now().toString());
                put("format", "json");
                put("version", "1.0");
                put("includeCharts", includeCharts);
            }});

            exportData.put("statistics", statistics);

            // 简单JSON序列化
            content.append("{\n");
            content.append("  \"metadata\": {\n");
            content.append("    \"exportedAt\": \"").append(LocalDateTime.now()).append("\",\n");
            content.append("    \"format\": \"json\",\n");
            content.append("    \"version\": \"1.0\",\n");
            content.append("    \"includeCharts\": ").append(includeCharts).append("\n");
            content.append("  },\n");
            content.append("  \"statistics\": {\n");

            // 添加每日统计
            List<Map<String, Object>> dailyStats = (List<Map<String, Object>>) statistics.get("daily_stats");
            content.append("    \"daily_stats\": [\n");
            for (int i = 0; i < dailyStats.size(); i++) {
                Map<String, Object> stat = dailyStats.get(i);
                content.append("      {\n");
                content.append("        \"date\": \"").append(stat.get("date")).append("\",\n");
                content.append("        \"total_study_time\": ").append(stat.get("total_study_time")).append(",\n");
                content.append("        \"words_studied\": ").append(stat.get("words_studied")).append(",\n");
                content.append("        \"documents_read\": ").append(stat.get("documents_read")).append(",\n");
                content.append("        \"notes_created\": ").append(stat.get("notes_created")).append(",\n");
                content.append("        \"highlights_created\": ").append(stat.get("highlights_created")).append("\n");
                content.append("      }");
                if (i < dailyStats.size() - 1) content.append(",");
                content.append("\n");
            }
            content.append("    ]\n");

            content.append("  }\n");
            content.append("}");

        } else if (format.equalsIgnoreCase("csv")) {
            // CSV格式
            content.append("学习统计报告\n");
            content.append("============\n\n");

            // 每日学习统计
            content.append("每日学习统计:\n");
            content.append("日期,学习时长(分钟),学习单词数,阅读文档数,创建笔记数,创建高亮数\n");

            List<Map<String, Object>> dailyStats = (List<Map<String, Object>>) statistics.get("daily_stats");
            for (Map<String, Object> stat : dailyStats) {
                content.append(escapeCsv(stat.get("date"))).append(",");
                content.append(stat.get("total_study_time")).append(",");
                content.append(stat.get("words_studied")).append(",");
                content.append(stat.get("documents_read")).append(",");
                content.append(stat.get("notes_created")).append(",");
                content.append(stat.get("highlights_created")).append("\n");
            }

            content.append("\n词汇掌握统计:\n");
            content.append("掌握等级,词汇数量\n");

            List<Map<String, Object>> vocabStats = (List<Map<String, Object>>) statistics.get("vocabulary_stats");
            for (Map<String, Object> stat : vocabStats) {
                content.append(stat.get("mastery_level")).append(",");
                content.append(stat.get("count")).append("\n");
            }

        } else {
            // 默认文本格式
            content.append("学习统计报告\n");
            content.append("============\n\n");
            content.append("生成时间: ").append(LocalDateTime.now()).append("\n");
            content.append("报告格式: ").append(format).append("\n");
            content.append("包含图表: ").append(includeCharts ? "是" : "否").append("\n\n");

            // 总体统计
            content.append("总体学习概况:\n");
            content.append("------------\n");

            List<Map<String, Object>> dailyStats = (List<Map<String, Object>>) statistics.get("daily_stats");
            List<Map<String, Object>> vocabStats = (List<Map<String, Object>>) statistics.get("vocabulary_stats");
            List<Map<String, Object>> reviewStats = (List<Map<String, Object>>) statistics.get("review_stats");
            List<Map<String, Object>> readingStats = (List<Map<String, Object>>) statistics.get("reading_stats");

            // 计算总学习时长
            int totalStudyTime = dailyStats.stream()
                    .mapToInt(stat -> stat.get("total_study_time") != null ? ((Number) stat.get("total_study_time")).intValue() : 0)
                    .sum();

            // 计算总学习单词数
            int totalWordsStudied = dailyStats.stream()
                    .mapToInt(stat -> stat.get("words_studied") != null ? ((Number) stat.get("words_studied")).intValue() : 0)
                    .sum();

            content.append("统计天数: ").append(dailyStats.size()).append("天\n");
            content.append("总学习时长: ").append(totalStudyTime).append("分钟\n");
            content.append("总学习单词数: ").append(totalWordsStudied).append("个\n");

            if (!reviewStats.isEmpty()) {
                Map<String, Object> review = reviewStats.get(0);
                content.append("总复习会话: ").append(review.get("total_sessions")).append("次\n");
                content.append("总复习项目: ").append(review.get("total_items")).append("个\n");
                content.append("平均正确率: ").append(String.format("%.2f", review.get("avg_accuracy"))).append("%\n");
            }

            if (!readingStats.isEmpty()) {
                Map<String, Object> reading = readingStats.get(0);
                content.append("总阅读文档: ").append(reading.get("total_documents")).append("个\n");
                content.append("总阅读页数: ").append(reading.get("total_pages")).append("页\n");
                content.append("平均阅读进度: ").append(String.format("%.2f", reading.get("avg_progress"))).append("%\n");
            }

            content.append("\n词汇掌握分布:\n");
            content.append("------------\n");

            for (Map<String, Object> vocab : vocabStats) {
                int level = ((Number) vocab.get("mastery_level")).intValue();
                int count = ((Number) vocab.get("count")).intValue();
                content.append("掌握度").append(level).append("%: ").append(count).append("个词汇\n");
            }

            // 最近学习趋势
            content.append("\n最近学习趋势（最近7天）:\n");
            content.append("--------------------\n");

            List<Map<String, Object>> learningTrend = (List<Map<String, Object>>) statistics.get("learning_trend");
            for (Map<String, Object> trend : learningTrend) {
                content.append(trend.get("date")).append(": ").append(trend.get("words_studied")).append("个单词\n");
            }

            // 学习成就
            List<Map<String, Object>> achievements = (List<Map<String, Object>>) statistics.get("achievements");
            if (!achievements.isEmpty()) {
                content.append("\n已获得的学习成就:\n");
                content.append("----------------\n");

                for (Map<String, Object> achievement : achievements) {
                    content.append("• ").append(achievement.get("name")).append("\n");
                    content.append("  ").append(achievement.get("description")).append("\n");
                    content.append("  获得时间: ").append(achievement.get("unlocked_at")).append("\n\n");
                }
            }
        }

        return content.toString();
    }

    // 获取文件扩展名
    private String getFileExtension(String format) {
        switch (format.toLowerCase()) {
            case "pdf": return "pdf";
            case "xlsx": return "xlsx";
            case "csv": return "csv";
            case "json": return "json";
            default: return "txt";
        }
    }

    // 获取媒体类型
    private MediaType getMediaType(String format) {
        switch (format.toLowerCase()) {
            case "pdf": return MediaType.APPLICATION_PDF;
            case "xlsx": return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "csv": return MediaType.parseMediaType("text/csv");
            case "json": return MediaType.APPLICATION_JSON;
            default: return MediaType.TEXT_PLAIN;
        }
    }

    // 转义JSON字符串
    private String escapeJson(Object value) {
        if (value == null) return "";
        return value.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // 转义CSV字符串
    private String escapeCsv(Object value) {
        if (value == null) return "";
        String str = value.toString();
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }
}