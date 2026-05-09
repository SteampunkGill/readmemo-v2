package com.vue.readingapp.vocabulary;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/vocabulary")
public class VocabularyExport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到导出生词本请求 ===");
        System.out.println("请求参数: " + request);
        System.out.println("=========================");
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
    public static class ExportVocabularyResponse {
        private boolean success;
        private String message;
        private ExportData data;

        public ExportVocabularyResponse(boolean success, String message, ExportData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ExportData getData() { return data; }
        public void setData(ExportData data) { this.data = data; }
    }

    public static class ExportData {
        private String format;
        private String content;
        private String fileName;
        private String fileSize;
        private String downloadUrl;
        private String expiresAt;
        private List<String> fields;

        public ExportData(String format, String content, String fileName, String fileSize,
                          String downloadUrl, String expiresAt, List<String> fields) {
            this.format = format;
            this.content = content;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.downloadUrl = downloadUrl;
            this.expiresAt = expiresAt;
            this.fields = fields;
        }

        // Getters and Setters
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getFileSize() { return fileSize; }
        public void setFileSize(String fileSize) { this.fileSize = fileSize; }

        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

        public String getExpiresAt() { return expiresAt; }
        public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

        public List<String> getFields() { return fields; }
        public void setFields(List<String> fields) { this.fields = fields; }
    }

    @GetMapping("/export")
    public ResponseEntity<ExportVocabularyResponse> exportVocabulary(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false, defaultValue = "csv") String format,
            @RequestParam(required = false) List<String> fields,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) List<String> tags) {

        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("format", format);
        requestParams.put("fields", fields);
        requestParams.put("status", status);
        requestParams.put("language", language);
        requestParams.put("tags", tags);
        printRequest(requestParams);

        try {
            // 1. 验证用户身份
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ExportVocabularyResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > ?";
            LocalDateTime now = LocalDateTime.now();
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(userSql, token, now);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ExportVocabularyResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Long userId = ((Number) sessions.get(0).get("user_id")).longValue();

            // 2. 验证导出格式
            List<String> validFormats = Arrays.asList("csv", "json", "txt", "xlsx");
            if (!validFormats.contains(format.toLowerCase())) {
                return ResponseEntity.badRequest().body(
                        new ExportVocabularyResponse(false, "不支持的导出格式: " + format, null)
                );
            }

            // 3. 设置默认字段
            List<String> exportFields;
            if (fields == null || fields.isEmpty()) {
                exportFields = Arrays.asList("word", "language", "definition", "example",
                        "status", "mastery_level", "review_count",
                        "last_reviewed_at", "next_review_at",
                        "source", "source_page", "notes", "tags",
                        "created_at", "updated_at");
            } else {
                exportFields = fields;
            }

            // 4. 构建查询条件
            StringBuilder whereClause = new StringBuilder("WHERE uv.user_id = ?");
            List<Object> params = new ArrayList<>();
            params.add(userId);

            if (status != null && !status.trim().isEmpty()) {
                whereClause.append(" AND uv.status = ?");
                params.add(status.trim());
            }

            if (language != null && !language.trim().isEmpty()) {
                whereClause.append(" AND uv.language = ?");
                params.add(language.trim());
            }

            // 5. 查询生词数据
            String dataSql = "SELECT uv.user_vocab_id, uv.word, uv.language, uv.definition, uv.example, uv.notes, " +
                    "uv.status, uv.mastery_level, uv.review_count, uv.last_reviewed_at, uv.next_review_at, " +
                    "uv.source, uv.source_page, uv.created_at, uv.updated_at, " +
                    "GROUP_CONCAT(DISTINCT vt.tag_name) as tags " +
                    "FROM user_vocabulary uv " +
                    "LEFT JOIN user_vocabulary_tags uvt ON uv.user_vocab_id = uvt.user_vocab_id " +
                    "LEFT JOIN vocabulary_tags vt ON uvt.tag_id = vt.tag_id " +
                    whereClause.toString();

            if (tags != null && !tags.isEmpty()) {
                dataSql += " AND vt.tag_name IN (" + String.join(",", Collections.nCopies(tags.size(), "?")) + ")";
                params.addAll(tags);
            }

            dataSql += " GROUP BY uv.user_vocab_id ORDER BY uv.created_at DESC";

            List<Map<String, Object>> results = jdbcTemplate.queryForList(dataSql, params.toArray());
            printQueryResult("查询到 " + results.size() + " 条记录用于导出");

            // 6. 根据格式生成内容
            String content;
            switch (format.toLowerCase()) {
                case "csv":
                    content = generateCsvContent(results, exportFields);
                    break;
                case "json":
                    content = generateJsonContent(results, exportFields);
                    break;
                case "txt":
                    content = generateTxtContent(results, exportFields);
                    break;
                case "xlsx":
                    // 对于xlsx格式，我们通常返回文件下载URL而不是内容
                    content = "xlsx格式需要生成文件，请使用下载URL";
                    break;
                default:
                    content = "";
            }

            // 7. 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "vocabulary_export_" + timestamp + "." + format.toLowerCase();

            // 8. 计算文件大小（字节数）
            int fileSizeBytes = content.getBytes("UTF-8").length;
            String fileSize = formatFileSize(fileSizeBytes);

            // 9. 生成下载URL（这里简单模拟，实际项目中可能需要生成实际文件）
            String downloadUrl = "/api/vocabulary/download/" + fileName;

            // 10. 设置过期时间（24小时后）
            String expiresAt = LocalDateTime.now().plusHours(24).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // 11. 组装响应数据
            ExportData exportData = new ExportData(format, content, fileName, fileSize, downloadUrl, expiresAt, exportFields);
            ExportVocabularyResponse response = new ExportVocabularyResponse(true, "生词本导出成功", exportData);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("导出生词本过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ExportVocabularyResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private String generateCsvContent(List<Map<String, Object>> data, List<String> fields) {
        StringBuilder csv = new StringBuilder();

        // 添加表头
        for (int i = 0; i < fields.size(); i++) {
            csv.append(fields.get(i));
            if (i < fields.size() - 1) {
                csv.append(",");
            }
        }
        csv.append("\n");

        // 添加数据行
        for (Map<String, Object> row : data) {
            for (int i = 0; i < fields.size(); i++) {
                String field = fields.get(i);
                Object value = row.get(field);

                if (value != null) {
                    String strValue = value.toString();
                    // 处理CSV特殊字符（逗号、引号、换行符）
                    if (strValue.contains(",") || strValue.contains("\"") || strValue.contains("\n")) {
                        strValue = "\"" + strValue.replace("\"", "\"\"") + "\"";
                    }
                    csv.append(strValue);
                }

                if (i < fields.size() - 1) {
                    csv.append(",");
                }
            }
            csv.append("\n");
        }

        return csv.toString();
    }

    private String generateJsonContent(List<Map<String, Object>> data, List<String> fields) {
        List<Map<String, Object>> filteredData = new ArrayList<>();

        for (Map<String, Object> row : data) {
            Map<String, Object> filteredRow = new HashMap<>();
            for (String field : fields) {
                if (row.containsKey(field)) {
                    filteredRow.put(field, row.get(field));
                }
            }
            filteredData.add(filteredRow);
        }

        // 使用简单的JSON序列化
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"export_date\": \"").append(LocalDateTime.now().toString()).append("\",\n");
        json.append("  \"total_items\": ").append(filteredData.size()).append(",\n");
        json.append("  \"items\": [\n");

        for (int i = 0; i < filteredData.size(); i++) {
            Map<String, Object> row = filteredData.get(i);
            json.append("    {\n");

            int fieldCount = 0;
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                json.append("      \"").append(entry.getKey()).append("\": ");

                Object value = entry.getValue();
                if (value instanceof String) {
                    json.append("\"").append(escapeJsonString(value.toString())).append("\"");
                } else if (value instanceof Number) {
                    json.append(value);
                } else if (value instanceof Boolean) {
                    json.append(value);
                } else if (value == null) {
                    json.append("null");
                } else {
                    json.append("\"").append(escapeJsonString(value.toString())).append("\"");
                }

                fieldCount++;
                if (fieldCount < row.size()) {
                    json.append(",");
                }
                json.append("\n");
            }

            json.append("    }");
            if (i < filteredData.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}");

        return json.toString();
    }

    private String generateTxtContent(List<Map<String, Object>> data, List<String> fields) {
        StringBuilder txt = new StringBuilder();

        txt.append("Vocabulary Export\n");
        txt.append("=================\n");
        txt.append("Export Date: ").append(LocalDateTime.now().toString()).append("\n");
        txt.append("Total Items: ").append(data.size()).append("\n\n");

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            txt.append("Item ").append(i + 1).append(":\n");

            for (String field : fields) {
                Object value = row.get(field);
                txt.append("  ").append(field).append(": ");

                if (value != null) {
                    txt.append(value.toString());
                } else {
                    txt.append("(null)");
                }

                txt.append("\n");
            }

            txt.append("\n");
        }

        return txt.toString();
    }

    private String escapeJsonString(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
