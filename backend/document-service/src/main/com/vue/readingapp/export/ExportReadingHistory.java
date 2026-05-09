package com.vue.readingapp.export;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/export")
public class ExportReadingHistory {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到阅读历史导出请求 ===");
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

    @GetMapping("/reading-history")
    public ResponseEntity<?> exportReadingHistory(@RequestParam(required = false, defaultValue = "csv") String format,
                                                  @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest("format=" + format);

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
            if (!"csv".equalsIgnoreCase(format) && !"json".equalsIgnoreCase(format) && !"txt".equalsIgnoreCase(format)) {
                Map<String, Object> details = new HashMap<>();
                details.put("format", "不支持的导出格式: " + format);
                details.put("supported_formats", Arrays.asList("csv", "json", "txt"));
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 3. 查询阅读历史
            String historySql = "SELECT rh.history_id, rh.document_id, d.title as document_title, " +
                    "rh.start_time, rh.end_time, rh.duration_seconds, rh.pages_read, " +
                    "rh.created_at " +
                    "FROM reading_history rh " +
                    "JOIN documents d ON rh.document_id = d.document_id " +
                    "WHERE rh.user_id = ? " +
                    "ORDER BY rh.start_time DESC";

            List<Map<String, Object>> readingHistory = jdbcTemplate.queryForList(historySql, userId);
            printQueryResult("阅读历史查询结果: " + readingHistory.size() + "条记录");

            if (readingHistory.isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("userId", userId);
                ErrorResponse errorResponse = new ErrorResponse("READING_HISTORY_NOT_FOUND", "没有找到阅读历史记录", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // 4. 生成导出文件内容
            String fileContent = generateReadingHistoryContent(readingHistory, format);
            byte[] fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);

            // 5. 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String extension = getFileExtension(format);
            String filename = "reading_history_" + timestamp + "." + extension;

            // 6. 记录导出历史
            String exportId = "export_reading_history_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String insertHistorySql = "INSERT INTO sync_logs (user_id, operation_type, entity_type, " +
                    "status, details, created_at) VALUES (?, ?, ?, ?, ?, ?)";

            Map<String, Object> exportDetails = new HashMap<>();
            exportDetails.put("export_id", exportId);
            exportDetails.put("format", format);
            exportDetails.put("item_count", readingHistory.size());
            exportDetails.put("filename", filename);
            exportDetails.put("file_size", fileBytes.length);

            String detailsJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_OBJECT(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", String.class,
                    "export_id", exportId,
                    "format", format,
                    "item_count", readingHistory.size(),
                    "filename", filename,
                    "file_size", fileBytes.length
            );

            jdbcTemplate.update(insertHistorySql,
                    userId,
                    "EXPORT",
                    "READING_HISTORY",
                    "COMPLETED",
                    detailsJson,
                    LocalDateTime.now()
            );

            // 7. 准备文件响应
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(getMediaType(format));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(fileBytes.length);

            // 打印返回信息
            Map<String, Object> responseInfo = new HashMap<>();
            responseInfo.put("filename", filename);
            responseInfo.put("file_size", fileBytes.length);
            responseInfo.put("format", format);
            responseInfo.put("history_count", readingHistory.size());
            printResponse(responseInfo);

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("阅读历史导出过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("READING_HISTORY_EXPORT_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 生成阅读历史内容
    private String generateReadingHistoryContent(List<Map<String, Object>> readingHistory, String format) {
        StringBuilder content = new StringBuilder();

        if (format.equalsIgnoreCase("json")) {
            // JSON格式
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("metadata", new HashMap<String, Object>() {{
                put("exportedAt", LocalDateTime.now().toString());
                put("type", "reading_history");
                put("count", readingHistory.size());
                put("format", "json");
                put("version", "1.0");
            }});

            exportData.put("reading_history", readingHistory);

            // 简单JSON序列化
            content.append("{\n");
            content.append("  \"metadata\": {\n");
            content.append("    \"exportedAt\": \"").append(LocalDateTime.now()).append("\",\n");
            content.append("    \"type\": \"reading_history\",\n");
            content.append("    \"count\": ").append(readingHistory.size()).append(",\n");
            content.append("    \"format\": \"json\",\n");
            content.append("    \"version\": \"1.0\"\n");
            content.append("  },\n");
            content.append("  \"reading_history\": [\n");

            for (int i = 0; i < readingHistory.size(); i++) {
                Map<String, Object> history = readingHistory.get(i);
                content.append("    {\n");
                content.append("      \"history_id\": ").append(history.get("history_id")).append(",\n");
                content.append("      \"document_id\": ").append(history.get("document_id")).append(",\n");
                content.append("      \"document_title\": \"").append(escapeJson(history.get("document_title"))).append("\",\n");
                content.append("      \"start_time\": \"").append(history.get("start_time")).append("\",\n");
                content.append("      \"end_time\": \"").append(history.get("end_time")).append("\",\n");
                content.append("      \"duration_seconds\": ").append(history.get("duration_seconds")).append(",\n");
                content.append("      \"pages_read\": ").append(history.get("pages_read")).append(",\n");
                content.append("      \"created_at\": \"").append(history.get("created_at")).append("\"\n");
                content.append("    }");
                if (i < readingHistory.size() - 1) content.append(",");
                content.append("\n");
            }

            content.append("  ]\n");
            content.append("}");

        } else if (format.equalsIgnoreCase("csv")) {
            // CSV格式
            content.append("阅读历史导出报告\n");
            content.append("================\n\n");
            content.append("导出时间: ").append(LocalDateTime.now()).append("\n");
            content.append("记录数量: ").append(readingHistory.size()).append("\n\n");

            content.append("历史ID,文档ID,文档标题,开始时间,结束时间,持续时间(秒),阅读页数,创建时间\n");

            for (Map<String, Object> history : readingHistory) {
                content.append(history.get("history_id")).append(",");
                content.append(history.get("document_id")).append(",");
                content.append(escapeCsv(history.get("document_title"))).append(",");
                content.append(escapeCsv(history.get("start_time"))).append(",");
                content.append(escapeCsv(history.get("end_time"))).append(",");
                content.append(history.get("duration_seconds")).append(",");
                content.append(history.get("pages_read")).append(",");
                content.append(escapeCsv(history.get("created_at"))).append("\n");
            }

        } else {
            // 默认文本格式
            content.append("阅读历史导出报告\n");
            content.append("================\n\n");
            content.append("导出时间: ").append(LocalDateTime.now()).append("\n");
            content.append("记录数量: ").append(readingHistory.size()).append("\n");
            content.append("导出格式: ").append(format).append("\n\n");

            content.append("阅读历史列表:\n");
            content.append("------------\n");

            for (int i = 0; i < readingHistory.size(); i++) {
                Map<String, Object> history = readingHistory.get(i);

                content.append(i + 1).append(". ").append(history.get("document_title")).append("\n");
                content.append("   开始时间: ").append(history.get("start_time")).append("\n");
                content.append("   结束时间: ").append(history.get("end_time")).append("\n");
                content.append("   持续时间: ").append(history.get("duration_seconds")).append(" 秒\n");
                content.append("   阅读页数: ").append(history.get("pages_read")).append(" 页\n");
                content.append("   记录时间: ").append(history.get("created_at")).append("\n");
                content.append("   -------------------------\n\n");
            }

            // 添加统计信息
            int totalPagesRead = readingHistory.stream()
                    .mapToInt(h -> ((Number) h.get("pages_read")).intValue())
                    .sum();

            int totalDuration = readingHistory.stream()
                    .mapToInt(h -> ((Number) h.get("duration_seconds")).intValue())
                    .sum();

            content.append("统计摘要:\n");
            content.append("--------\n");
            content.append("总阅读次数: ").append(readingHistory.size()).append(" 次\n");
            content.append("总阅读页数: ").append(totalPagesRead).append(" 页\n");
            content.append("总阅读时间: ").append(totalDuration).append(" 秒 (约 ");
            content.append(String.format("%.1f", totalDuration / 3600.0)).append(" 小时)\n");

            // 按文档统计
            Map<String, Integer> pagesByDocument = new HashMap<>();
            Map<String, Integer> durationByDocument = new HashMap<>();

            for (Map<String, Object> history : readingHistory) {
                String docTitle = (String) history.get("document_title");
                int pages = ((Number) history.get("pages_read")).intValue();
                int duration = ((Number) history.get("duration_seconds")).intValue();

                pagesByDocument.put(docTitle, pagesByDocument.getOrDefault(docTitle, 0) + pages);
                durationByDocument.put(docTitle, durationByDocument.getOrDefault(docTitle, 0) + duration);
            }

            content.append("\n按文档统计:\n");
            content.append("----------\n");
            for (Map.Entry<String, Integer> entry : pagesByDocument.entrySet()) {
                String docTitle = entry.getKey();
                int pages = entry.getValue();
                int duration = durationByDocument.getOrDefault(docTitle, 0);

                content.append(docTitle).append(": ");
                content.append(pages).append(" 页, ");
                content.append(duration).append(" 秒\n");
            }
        }

        return content.toString();
    }

    // 获取文件扩展名
    private String getFileExtension(String format) {
        switch (format.toLowerCase()) {
            case "csv": return "csv";
            case "json": return "json";
            case "txt": return "txt";
            default: return "txt";
        }
    }

    // 获取媒体类型
    private MediaType getMediaType(String format) {
        switch (format.toLowerCase()) {
            case "csv": return MediaType.parseMediaType("text/csv");
            case "json": return MediaType.APPLICATION_JSON;
            case "txt": return MediaType.TEXT_PLAIN;
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
}