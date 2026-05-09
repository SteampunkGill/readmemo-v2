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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/v1/export")
public class BatchExport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到批量导出请求 ===");
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

    // 请求DTO
    public static class BatchExportRequest {
        private List<String> types;
        private String format = "zip";
        private Map<String, Object> template;

        public List<String> getTypes() { return types; }
        public void setTypes(List<String> types) { this.types = types; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public Map<String, Object> getTemplate() { return template; }
        public void setTemplate(Map<String, Object> template) { this.template = template; }
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

    // 支持的导出类型
    private static final Set<String> SUPPORTED_TYPES = new HashSet<>(Arrays.asList(
            "documents", "vocabulary", "reviews", "statistics", "notes", "highlights", "reading_history"
    ));

    @PostMapping("/batch")
    public ResponseEntity<?> batchExport(@RequestBody BatchExportRequest request,
                                         @RequestHeader(value = "Authorization", required = false) String authHeader) {
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
            if (request.getTypes() == null || request.getTypes().isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("types", "导出类型列表不能为空");
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 验证每个类型是否支持
            List<String> invalidTypes = new ArrayList<>();
            for (String type : request.getTypes()) {
                if (!SUPPORTED_TYPES.contains(type.toLowerCase())) {
                    invalidTypes.add(type);
                }
            }

            if (!invalidTypes.isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("invalid_types", invalidTypes);
                details.put("supported_types", SUPPORTED_TYPES);
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "包含不支持的导出类型", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (!"zip".equalsIgnoreCase(request.getFormat())) {
                Map<String, Object> details = new HashMap<>();
                details.put("format", "批量导出只支持ZIP格式");
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 3. 收集导出数据
            Map<String, byte[]> exportFiles = new HashMap<>();
            Map<String, String> fileDescriptions = new HashMap<>();

            for (String type : request.getTypes()) {
                switch (type.toLowerCase()) {
                    case "documents":
                        byte[] documentsData = exportDocumentsData(userId);
                        exportFiles.put("documents.json", documentsData);
                        fileDescriptions.put("documents.json", "文档数据");
                        break;

                    case "vocabulary":
                        byte[] vocabularyData = exportVocabularyData(userId);
                        exportFiles.put("vocabulary.json", vocabularyData);
                        fileDescriptions.put("vocabulary.json", "词汇数据");
                        break;

                    case "reviews":
                        byte[] reviewsData = exportReviewsData(userId);
                        exportFiles.put("reviews.json", reviewsData);
                        fileDescriptions.put("reviews.json", "复习记录");
                        break;

                    case "statistics":
                        byte[] statisticsData = exportStatisticsData(userId);
                        exportFiles.put("statistics.json", statisticsData);
                        fileDescriptions.put("statistics.json", "学习统计");
                        break;

                    case "notes":
                        byte[] notesData = exportNotesData(userId);
                        exportFiles.put("notes.json", notesData);
                        fileDescriptions.put("notes.json", "阅读笔记");
                        break;

                    case "highlights":
                        byte[] highlightsData = exportHighlightsData(userId);
                        exportFiles.put("highlights.json", highlightsData);
                        fileDescriptions.put("highlights.json", "文档高亮");
                        break;

                    case "reading_history":
                        byte[] readingHistoryData = exportReadingHistoryData(userId);
                        exportFiles.put("reading_history.json", readingHistoryData);
                        fileDescriptions.put("reading_history.json", "阅读历史");
                        break;
                }
            }

            if (exportFiles.isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("types", request.getTypes());
                ErrorResponse errorResponse = new ErrorResponse("EXPORT_ERROR", "没有生成任何导出文件", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // 4. 创建ZIP文件
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            // 添加README文件
            String readmeContent = generateReadmeContent(request.getTypes(), fileDescriptions, userId);
            ZipEntry readmeEntry = new ZipEntry("README.txt");
            zos.putNextEntry(readmeEntry);
            zos.write(readmeContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 添加导出文件
            for (Map.Entry<String, byte[]> entry : exportFiles.entrySet()) {
                ZipEntry fileEntry = new ZipEntry(entry.getKey());
                zos.putNextEntry(fileEntry);
                zos.write(entry.getValue());
                zos.closeEntry();
            }

            zos.close();
            byte[] zipBytes = baos.toByteArray();

            // 5. 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = "batch_export_" + timestamp + ".zip";

            // 6. 记录导出历史
            String exportId = "export_batch_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String insertHistorySql = "INSERT INTO sync_logs (user_id, operation_type, entity_type, entity_ids, " +
                    "status, details, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

            String typesJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_ARRAY(?)", String.class,
                    String.join(",", request.getTypes())
            );

            Map<String, Object> exportDetails = new HashMap<>();
            exportDetails.put("export_id", exportId);
            exportDetails.put("format", request.getFormat());
            exportDetails.put("types", request.getTypes());
            exportDetails.put("file_count", exportFiles.size() + 1); // +1 for README
            exportDetails.put("total_size", zipBytes.length);
            exportDetails.put("filename", filename);

            String detailsJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_OBJECT(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", String.class,
                    "export_id", exportId,
                    "format", request.getFormat(),
                    "types", String.join(",", request.getTypes()),
                    "file_count", exportFiles.size() + 1,
                    "total_size", zipBytes.length,
                    "filename", filename
            );

            jdbcTemplate.update(insertHistorySql,
                    userId,
                    "EXPORT",
                    "BATCH",
                    typesJson,
                    "COMPLETED",
                    detailsJson,
                    LocalDateTime.now()
            );

            // 7. 准备文件响应
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/zip"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(zipBytes.length);

            // 打印返回信息
            Map<String, Object> responseInfo = new HashMap<>();
            responseInfo.put("filename", filename);
            responseInfo.put("file_size", zipBytes.length);
            responseInfo.put("format", request.getFormat());
            responseInfo.put("types", request.getTypes());
            responseInfo.put("file_count", exportFiles.size() + 1);
            printResponse(responseInfo);

            return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("批量导出过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("BATCH_EXPORT_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 导出文档数据
    private byte[] exportDocumentsData(int userId) {
        String sql = "SELECT document_id, title, author, description, file_path, " +
                "file_size, page_count, reading_progress, created_at, updated_at " +
                "FROM documents WHERE user_id = ?";

        List<Map<String, Object>> documents = jdbcTemplate.queryForList(sql, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("metadata", new HashMap<String, Object>() {{
            put("exportedAt", LocalDateTime.now().toString());
            put("type", "documents");
            put("count", documents.size());
        }});
        result.put("documents", documents);

        return result.toString().getBytes(StandardCharsets.UTF_8);
    }

    // 导出词汇数据
    private byte[] exportVocabularyData(int userId) {
        String sql = "SELECT uv.user_vocabulary_id, w.word, w.phonetic, wd.definition as meaning, " +
                "w.part_of_speech, uv.mastery_level, uv.study_count, uv.correct_count, " +
                "uv.wrong_count, uv.last_studied_at, uv.created_at " +
                "FROM user_vocabulary uv " +
                "JOIN words w ON uv.word_id = w.word_id " +
                "LEFT JOIN word_definitions wd ON w.word_id = wd.word_id AND wd.is_primary = 1 " +
                "WHERE uv.user_id = ?";

        List<Map<String, Object>> vocabulary = jdbcTemplate.queryForList(sql, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("metadata", new HashMap<String, Object>() {{
            put("exportedAt", LocalDateTime.now().toString());
            put("type", "vocabulary");
            put("count", vocabulary.size());
        }});
        result.put("vocabulary", vocabulary);

        return result.toString().getBytes(StandardCharsets.UTF_8);
    }

    // 导出复习数据
    private byte[] exportReviewsData(int userId) {
        String sql = "SELECT review_session_id, start_time, end_time, total_items, " +
                "correct_count, wrong_count, accuracy, session_type, created_at " +
                "FROM review_sessions WHERE user_id = ?";

        List<Map<String, Object>> reviews = jdbcTemplate.queryForList(sql, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("metadata", new HashMap<String, Object>() {{
            put("exportedAt", LocalDateTime.now().toString());
            put("type", "reviews");
            put("count", reviews.size());
        }});
        result.put("reviews", reviews);

        return result.toString().getBytes(StandardCharsets.UTF_8);
    }

    // 导出统计数据
    private byte[] exportStatisticsData(int userId) {
        // 查询各种统计数据
        Map<String, Object> statistics = new HashMap<>();

        // 每日统计
        String dailySql = "SELECT date, total_study_time, words_studied, documents_read, " +
                "notes_created, highlights_created FROM daily_learning_stats " +
                "WHERE user_id = ? ORDER BY date DESC LIMIT 30";
        statistics.put("daily_stats", jdbcTemplate.queryForList(dailySql, userId));

        // 词汇统计
        String vocabSql = "SELECT mastery_level, COUNT(*) as count FROM user_vocabulary " +
                "WHERE user_id = ? GROUP BY mastery_level";
        statistics.put("vocabulary_stats", jdbcTemplate.queryForList(vocabSql, userId));

        // 成就统计
        String achieveSql = "SELECT COUNT(*) as total_achievements FROM user_achievements " +
                "WHERE user_id = ?";
        statistics.put("achievement_stats", jdbcTemplate.queryForList(achieveSql, userId));

        Map<String, Object> result = new HashMap<>();
        result.put("metadata", new HashMap<String, Object>() {{
            put("exportedAt", LocalDateTime.now().toString());
            put("type", "statistics");
        }});
        result.put("statistics", statistics);

        return result.toString().getBytes(StandardCharsets.UTF_8);
    }

    // 导出笔记数据
    private byte[] exportNotesData(int userId) {
        String sql = "SELECT note_id, document_id, page_number, content, " +
                "highlight_id, created_at, updated_at " +
                "FROM document_notes WHERE user_id = ?";

        List<Map<String, Object>> notes = jdbcTemplate.queryForList(sql, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("metadata", new HashMap<String, Object>() {{
            put("exportedAt", LocalDateTime.now().toString());
            put("type", "notes");
            put("count", notes.size());
        }});
        result.put("notes", notes);

        return result.toString().getBytes(StandardCharsets.UTF_8);
    }

    // 导出高亮数据
    private byte[] exportHighlightsData(int userId) {
        String sql = "SELECT highlight_id, document_id, page_number, text_content, " +
                "color, start_position, end_position, created_at " +
                "FROM document_highlights WHERE user_id = ?";

        List<Map<String, Object>> highlights = jdbcTemplate.queryForList(sql, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("metadata", new HashMap<String, Object>() {{
            put("exportedAt", LocalDateTime.now().toString());
            put("type", "highlights");
            put("count", highlights.size());
        }});
        result.put("highlights", highlights);

        return result.toString().getBytes(StandardCharsets.UTF_8);
    }

    // 导出阅读历史数据
    private byte[] exportReadingHistoryData(int userId) {
        String sql = "SELECT history_id, document_id, start_time, end_time, " +
                "duration_seconds, pages_read, created_at " +
                "FROM reading_history WHERE user_id = ? ORDER BY start_time DESC LIMIT 100";

        List<Map<String, Object>> history = jdbcTemplate.queryForList(sql, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("metadata", new HashMap<String, Object>() {{
            put("exportedAt", LocalDateTime.now().toString());
            put("type", "reading_history");
            put("count", history.size());
        }});
        result.put("reading_history", history);

        return result.toString().getBytes(StandardCharsets.UTF_8);
    }

    // 生成README内容
    private String generateReadmeContent(List<String> types, Map<String, String> fileDescriptions, int userId) {
        StringBuilder readme = new StringBuilder();
        readme.append("批量导出数据包\n");
        readme.append("============\n\n");
        readme.append("导出信息:\n");
        readme.append("--------\n");
        readme.append("用户ID: ").append(userId).append("\n");
        readme.append("导出时间: ").append(LocalDateTime.now()).append("\n");
        readme.append("导出类型: ").append(String.join(", ", types)).append("\n");
        readme.append("文件格式: ZIP压缩包\n\n");

        readme.append("包含文件:\n");
        readme.append("--------\n");
        for (Map.Entry<String, String> entry : fileDescriptions.entrySet()) {
            readme.append("• ").append(entry.getKey()).append(" - ").append(entry.getValue()).append("\n");
        }
        readme.append("• README.txt - 本说明文件\n\n");

        readme.append("数据说明:\n");
        readme.append("--------\n");
        readme.append("1. 所有数据均为JSON格式，便于解析和处理\n");
        readme.append("2. 每个文件都包含metadata元数据，说明导出信息\n");
        readme.append("3. 数据时间范围为用户账户创建至今\n");
        readme.append("4. 敏感信息（如密码）已进行安全处理\n\n");

        readme.append("使用建议:\n");
        readme.append("--------\n");
        readme.append("1. 建议定期备份此数据包\n");
        readme.append("2. 可用于数据迁移或第三方分析\n");
        readme.append("3. 如需导入回系统，请使用专门的导入功能\n");
        readme.append("4. 妥善保管此文件，避免数据泄露\n");

        return readme.toString();
    }
}