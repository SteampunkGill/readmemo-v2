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
public class ExportAllData {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到所有数据导出请求 ===");
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
    public static class ExportAllDataRequest {
        private String format = "json";
        private boolean encrypt = false;

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public boolean isEncrypt() { return encrypt; }
        public void setEncrypt(boolean encrypt) { this.encrypt = encrypt; }
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

    @GetMapping("/all")
    public ResponseEntity<?> exportAllData(@RequestParam(required = false, defaultValue = "json") String format,
                                           @RequestParam(required = false, defaultValue = "false") boolean encrypt,
                                           @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 构建请求对象
        ExportAllDataRequest request = new ExportAllDataRequest();
        request.setFormat(format);
        request.setEncrypt(encrypt);

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
            if (!"json".equalsIgnoreCase(request.getFormat())) {
                Map<String, Object> details = new HashMap<>();
                details.put("format", "所有数据导出只支持JSON格式");
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (request.isEncrypt()) {
                // 加密功能暂不支持
                Map<String, Object> details = new HashMap<>();
                details.put("encrypt", "加密功能暂未实现");
                ErrorResponse errorResponse = new ErrorResponse("FEATURE_NOT_AVAILABLE", "加密功能暂未实现", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 3. 查询所有数据
            Map<String, Object> allData = new HashMap<>();

            // 3.1 用户基本信息
            String userInfoSql = "SELECT user_id, username, email, nickname, avatar_url, " +
                    "role, is_verified, created_at, last_login_at FROM users " +
                    "WHERE user_id = ?";
            List<Map<String, Object>> userInfo = jdbcTemplate.queryForList(userInfoSql, userId);
            allData.put("user_info", userInfo.isEmpty() ? new HashMap<>() : userInfo.get(0));
            printQueryResult("用户信息查询完成");

            // 3.2 文档数据
            String documentsSql = "SELECT document_id, title, author, description, file_path, " +
                    "file_size, page_count, reading_progress, created_at, updated_at " +
                    "FROM documents WHERE user_id = ?";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(documentsSql, userId);
            allData.put("documents", documents);
            printQueryResult("文档数据查询完成: " + documents.size() + "条记录");

            // 3.3 词汇数据
            String vocabularySql = "SELECT uv.user_vocabulary_id, w.word, w.phonetic, wd.definition as meaning, " +
                    "w.part_of_speech, uv.mastery_level, uv.study_count, uv.correct_count, " +
                    "uv.wrong_count, uv.last_studied_at, uv.created_at " +
                    "FROM user_vocabulary uv " +
                    "JOIN words w ON uv.word_id = w.word_id " +
                    "LEFT JOIN word_definitions wd ON w.word_id = wd.word_id AND wd.is_primary = 1 " +
                    "WHERE uv.user_id = ?";
            List<Map<String, Object>> vocabulary = jdbcTemplate.queryForList(vocabularySql, userId);
            allData.put("vocabulary", vocabulary);
            printQueryResult("词汇数据查询完成: " + vocabulary.size() + "条记录");

            // 3.4 复习记录
            String reviewsSql = "SELECT review_session_id, start_time, end_time, total_items, " +
                    "correct_count, wrong_count, accuracy, session_type, created_at " +
                    "FROM review_sessions WHERE user_id = ?";
            List<Map<String, Object>> reviews = jdbcTemplate.queryForList(reviewsSql, userId);
            allData.put("reviews", reviews);
            printQueryResult("复习记录查询完成: " + reviews.size() + "条记录");

            // 3.5 学习统计
            String statsSql = "SELECT date, total_study_time, words_studied, documents_read, " +
                    "notes_created, highlights_created FROM daily_learning_stats " +
                    "WHERE user_id = ? ORDER BY date DESC";
            List<Map<String, Object>> learningStats = jdbcTemplate.queryForList(statsSql, userId);
            allData.put("learning_stats", learningStats);
            printQueryResult("学习统计查询完成: " + learningStats.size() + "条记录");

            // 3.6 笔记数据
            String notesSql = "SELECT note_id, document_id, page_number, content, " +
                    "highlight_id, created_at, updated_at " +
                    "FROM document_notes WHERE user_id = ?";
            List<Map<String, Object>> notes = jdbcTemplate.queryForList(notesSql, userId);
            allData.put("notes", notes);
            printQueryResult("笔记数据查询完成: " + notes.size() + "条记录");

            // 3.7 高亮数据
            String highlightsSql = "SELECT highlight_id, document_id, page_number, text_content, " +
                    "color, start_position, end_position, created_at " +
                    "FROM document_highlights WHERE user_id = ?";
            List<Map<String, Object>> highlights = jdbcTemplate.queryForList(highlightsSql, userId);
            allData.put("highlights", highlights);
            printQueryResult("高亮数据查询完成: " + highlights.size() + "条记录");

            // 3.8 阅读历史
            String historySql = "SELECT history_id, document_id, start_time, end_time, " +
                    "duration_seconds, pages_read, created_at " +
                    "FROM reading_history WHERE user_id = ? ORDER BY start_time DESC";
            List<Map<String, Object>> readingHistory = jdbcTemplate.queryForList(historySql, userId);
            allData.put("reading_history", readingHistory);
            printQueryResult("阅读历史查询完成: " + readingHistory.size() + "条记录");

            // 3.9 用户设置
            String settingsSql = "SELECT setting_key, setting_value FROM user_settings WHERE user_id = ?";
            List<Map<String, Object>> settings = jdbcTemplate.queryForList(settingsSql, userId);
            allData.put("settings", settings);
            printQueryResult("用户设置查询完成: " + settings.size() + "条记录");

            // 3.10 学习成就
            String achievementsSql = "SELECT a.achievement_id, a.name, a.description, a.icon_url, " +
                    "a.points, ua.unlocked_at FROM user_achievements ua " +
                    "JOIN learning_achievements a ON ua.achievement_id = a.achievement_id " +
                    "WHERE ua.user_id = ?";
            List<Map<String, Object>> achievements = jdbcTemplate.queryForList(achievementsSql, userId);
            allData.put("achievements", achievements);
            printQueryResult("学习成就查询完成: " + achievements.size() + "条记录");

            // 4. 添加元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("exported_at", LocalDateTime.now().toString());
            metadata.put("user_id", userId);
            metadata.put("format", "json");
            metadata.put("version", "1.0");
            metadata.put("data_sections", Arrays.asList(
                    "user_info", "documents", "vocabulary", "reviews",
                    "learning_stats", "notes", "highlights", "reading_history",
                    "settings", "achievements"
            ));

            Map<String, Object> finalData = new HashMap<>();
            finalData.put("metadata", metadata);
            finalData.put("data", allData);

            // 5. 生成JSON内容
            String jsonContent = generateJsonContent(finalData);
            byte[] fileBytes = jsonContent.getBytes(StandardCharsets.UTF_8);

            // 6. 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = "all_data_" + timestamp + ".json";

            // 7. 记录导出历史
            String exportId = "export_all_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String insertHistorySql = "INSERT INTO sync_logs (user_id, operation_type, entity_type, " +
                    "status, details, created_at) VALUES (?, ?, ?, ?, ?, ?)";

            Map<String, Object> exportDetails = new HashMap<>();
            exportDetails.put("export_id", exportId);
            exportDetails.put("format", request.getFormat());
            exportDetails.put("encrypt", request.isEncrypt());
            exportDetails.put("filename", filename);
            exportDetails.put("file_size", fileBytes.length);
            exportDetails.put("data_sections", metadata.get("data_sections"));

            String detailsJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_OBJECT(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", String.class,
                    "export_id", exportId,
                    "format", request.getFormat(),
                    "encrypt", request.isEncrypt(),
                    "filename", filename,
                    "file_size", fileBytes.length,
                    "data_sections", String.join(",", (List<String>) metadata.get("data_sections"))
            );

            jdbcTemplate.update(insertHistorySql,
                    userId,
                    "EXPORT",
                    "ALL_DATA",
                    "COMPLETED",
                    detailsJson,
                    LocalDateTime.now()
            );

            // 8. 准备文件响应
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(fileBytes.length);

            // 打印返回信息
            Map<String, Object> responseInfo = new HashMap<>();
            responseInfo.put("filename", filename);
            responseInfo.put("file_size", fileBytes.length);
            responseInfo.put("format", request.getFormat());
            responseInfo.put("data_sections_count", ((List<?>) metadata.get("data_sections")).size());
            responseInfo.put("total_records",
                    documents.size() + vocabulary.size() + reviews.size() +
                            learningStats.size() + notes.size() + highlights.size() +
                            readingHistory.size() + settings.size() + achievements.size()
            );
            printResponse(responseInfo);

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("所有数据导出过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("ALL_DATA_EXPORT_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 生成JSON内容
    private String generateJsonContent(Map<String, Object> data) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        // 添加metadata
        json.append("  \"metadata\": {\n");
        Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");

        int metaCount = 0;
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            json.append("    \"").append(entry.getKey()).append("\": ");

            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escapeJson(value.toString())).append("\"");
            } else if (value instanceof List) {
                json.append("[");
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    json.append("\"").append(escapeJson(list.get(i).toString())).append("\"");
                    if (i < list.size() - 1) json.append(", ");
                }
                json.append("]");
            } else {
                json.append(value);
            }

            if (++metaCount < metadata.size()) json.append(",");
            json.append("\n");
        }
        json.append("  },\n");

        // 添加data
        json.append("  \"data\": {\n");
        Map<String, Object> allData = (Map<String, Object>) data.get("data");

        int dataCount = 0;
        for (Map.Entry<String, Object> entry : allData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            json.append("    \"").append(key).append("\": ");

            if (value instanceof List) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) value;
                json.append("[\n");

                for (int i = 0; i < list.size(); i++) {
                    Map<String, Object> item = list.get(i);
                    json.append("      {\n");

                    int itemCount = 0;
                    for (Map.Entry<String, Object> itemEntry : item.entrySet()) {
                        json.append("        \"").append(itemEntry.getKey()).append("\": ");

                        Object itemValue = itemEntry.getValue();
                        if (itemValue instanceof String) {
                            json.append("\"").append(escapeJson(itemValue.toString())).append("\"");
                        } else if (itemValue instanceof java.sql.Timestamp) {
                            json.append("\"").append(itemValue.toString()).append("\"");
                        } else {
                            json.append(itemValue);
                        }

                        if (++itemCount < item.size()) json.append(",");
                        json.append("\n");
                    }

                    json.append("      }");
                    if (i < list.size() - 1) json.append(",");
                    json.append("\n");
                }

                json.append("    ]");
            } else if (value instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) value;
                json.append("{\n");

                int mapCount = 0;
                for (Map.Entry<String, Object> mapEntry : map.entrySet()) {
                    json.append("      \"").append(mapEntry.getKey()).append("\": ");

                    Object mapValue = mapEntry.getValue();
                    if (mapValue instanceof String) {
                        json.append("\"").append(escapeJson(mapValue.toString())).append("\"");
                    } else if (mapValue instanceof java.sql.Timestamp) {
                        json.append("\"").append(mapValue.toString()).append("\"");
                    } else {
                        json.append(mapValue);
                    }

                    if (++mapCount < map.size()) json.append(",");
                    json.append("\n");
                }

                json.append("    }");
            } else {
                json.append(value);
            }

            if (++dataCount < allData.size()) json.append(",");
            json.append("\n");
        }

        json.append("  }\n");
        json.append("}");

        return json.toString();
    }

    // 转义JSON字符串
    private String escapeJson(String str) {
        if (str == null) return "";
        return str
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}