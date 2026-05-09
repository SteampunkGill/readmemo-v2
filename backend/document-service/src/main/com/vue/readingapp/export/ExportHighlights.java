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
public class ExportHighlights {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到高亮导出请求 ===");
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
    public static class ExportHighlightsRequest {
        private int document_id;
        private String format = "json";

        public int getDocument_id() { return document_id; }
        public void setDocument_id(int document_id) { this.document_id = document_id; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
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
            "json", "csv", "pdf", "txt"
    ));

    @GetMapping("/documents/{documentId}/highlights")
    public ResponseEntity<?> exportHighlights(@PathVariable int documentId,
                                              @RequestParam(required = false, defaultValue = "json") String format,
                                              @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 构建请求对象
        ExportHighlightsRequest request = new ExportHighlightsRequest();
        request.setDocument_id(documentId);
        request.setFormat(format);

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

            // 3. 验证文档是否存在且属于当前用户
            String docSql = "SELECT document_id, title FROM documents WHERE document_id = ? AND user_id = ?";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(docSql, documentId, userId);

            if (documents.isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("documentId", documentId);
                ErrorResponse errorResponse = new ErrorResponse("DOCUMENT_NOT_FOUND", "文档不存在或无权限访问", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            Map<String, Object> document = documents.get(0);
            String documentTitle = (String) document.get("title");

            // 4. 查询文档高亮
            String highlightsSql = "SELECT highlight_id, document_id, page_number, text_content, " +
                    "color, start_position, end_position, created_at " +
                    "FROM document_highlights " +
                    "WHERE document_id = ? AND user_id = ? " +
                    "ORDER BY page_number, start_position";

            List<Map<String, Object>> highlights = jdbcTemplate.queryForList(highlightsSql, documentId, userId);
            printQueryResult("高亮查询结果: " + highlights.size() + "条记录");

            // 5. 生成导出文件内容
            String fileContent = generateHighlightsContent(highlights, documentTitle, request.getFormat());
            byte[] fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);

            // 6. 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String extension = getFileExtension(request.getFormat());
            String filename = "highlights_" + documentId + "_" + timestamp + "." + extension;

            // 7. 记录导出历史
            String exportId = "export_highlights_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String insertHistorySql = "INSERT INTO sync_logs (user_id, operation_type, entity_type, entity_ids, " +
                    "status, details, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

            String entityIdsJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_ARRAY(?)", String.class, String.valueOf(documentId)
            );

            Map<String, Object> exportDetails = new HashMap<>();
            exportDetails.put("export_id", exportId);
            exportDetails.put("format", request.getFormat());
            exportDetails.put("document_id", documentId);
            exportDetails.put("document_title", documentTitle);
            exportDetails.put("highlight_count", highlights.size());
            exportDetails.put("filename", filename);
            exportDetails.put("file_size", fileBytes.length);

            String detailsJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_OBJECT(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", String.class,
                    "export_id", exportId,
                    "format", request.getFormat(),
                    "document_id", documentId,
                    "document_title", documentTitle,
                    "highlight_count", highlights.size(),
                    "filename", filename,
                    "file_size", fileBytes.length
            );

            jdbcTemplate.update(insertHistorySql,
                    userId,
                    "EXPORT",
                    "HIGHLIGHTS",
                    entityIdsJson,
                    "COMPLETED",
                    detailsJson,
                    LocalDateTime.now()
            );

            // 8. 准备文件响应
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(getMediaType(request.getFormat()));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(fileBytes.length);

            // 打印返回信息
            Map<String, Object> responseInfo = new HashMap<>();
            responseInfo.put("filename", filename);
            responseInfo.put("file_size", fileBytes.length);
            responseInfo.put("format", request.getFormat());
            responseInfo.put("document_id", documentId);
            responseInfo.put("document_title", documentTitle);
            responseInfo.put("highlight_count", highlights.size());
            printResponse(responseInfo);

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("高亮导出过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            details.put("documentId", documentId);
            ErrorResponse errorResponse = new ErrorResponse("HIGHLIGHTS_EXPORT_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 生成高亮内容
    private String generateHighlightsContent(List<Map<String, Object>> highlights, String documentTitle, String format) {
        StringBuilder content = new StringBuilder();

        if (format.equalsIgnoreCase("json")) {
            // JSON格式
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("metadata", new HashMap<String, Object>() {{
                put("exportedAt", LocalDateTime.now().toString());
                put("type", "highlights");
                put("document_title", documentTitle);
                put("highlight_count", highlights.size());
                put("format", "json");
                put("version", "1.0");
            }});

            exportData.put("highlights", highlights);

            // 简单JSON序列化
            content.append("{\n");
            content.append("  \"metadata\": {\n");
            content.append("    \"exportedAt\": \"").append(LocalDateTime.now()).append("\",\n");
            content.append("    \"type\": \"highlights\",\n");
            content.append("    \"document_title\": \"").append(escapeJson(documentTitle)).append("\",\n");
            content.append("    \"highlight_count\": ").append(highlights.size()).append(",\n");
            content.append("    \"format\": \"json\",\n");
            content.append("    \"version\": \"1.0\"\n");
            content.append("  },\n");
            content.append("  \"highlights\": [\n");

            for (int i = 0; i < highlights.size(); i++) {
                Map<String, Object> highlight = highlights.get(i);
                content.append("    {\n");
                content.append("      \"highlight_id\": ").append(highlight.get("highlight_id")).append(",\n");
                content.append("      \"document_id\": ").append(highlight.get("document_id")).append(",\n");
                content.append("      \"page_number\": ").append(highlight.get("page_number")).append(",\n");
                content.append("      \"text_content\": \"").append(escapeJson(highlight.get("text_content"))).append("\",\n");
                content.append("      \"color\": \"").append(escapeJson(highlight.get("color"))).append("\",\n");
                content.append("      \"start_position\": ").append(highlight.get("start_position")).append(",\n");
                content.append("      \"end_position\": ").append(highlight.get("end_position")).append(",\n");
                content.append("      \"created_at\": \"").append(highlight.get("created_at")).append("\"\n");
                content.append("    }");
                if (i < highlights.size() - 1) content.append(",");
                content.append("\n");
            }

            content.append("  ]\n");
            content.append("}");

        } else if (format.equalsIgnoreCase("csv")) {
            // CSV格式
            content.append("文档标题: ").append(documentTitle).append("\n");
            content.append("导出时间: ").append(LocalDateTime.now()).append("\n");
            content.append("高亮数量: ").append(highlights.size()).append("\n\n");

            content.append("高亮ID,页码,文本内容,颜色,起始位置,结束位置,创建时间\n");

            for (Map<String, Object> highlight : highlights) {
                content.append(highlight.get("highlight_id")).append(",");
                content.append(highlight.get("page_number")).append(",");
                content.append(escapeCsv(highlight.get("text_content"))).append(",");
                content.append(escapeCsv(highlight.get("color"))).append(",");
                content.append(highlight.get("start_position")).append(",");
                content.append(highlight.get("end_position")).append(",");
                content.append(escapeCsv(highlight.get("created_at"))).append("\n");
            }

        } else {
            // 默认文本格式
            content.append("文档高亮导出报告\n");
            content.append("================\n\n");
            content.append("文档标题: ").append(documentTitle).append("\n");
            content.append("导出时间: ").append(LocalDateTime.now()).append("\n");
            content.append("高亮数量: ").append(highlights.size()).append("\n");
            content.append("导出格式: ").append(format).append("\n\n");

            content.append("高亮列表:\n");
            content.append("--------\n");

            // 按页码分组
            Map<Integer, List<Map<String, Object>>> highlightsByPage = new HashMap<>();
            for (Map<String, Object> highlight : highlights) {
                int pageNumber = ((Number) highlight.get("page_number")).intValue();
                highlightsByPage.computeIfAbsent(pageNumber, k -> new ArrayList<>()).add(highlight);
            }

            // 按页码排序
            List<Integer> sortedPages = new ArrayList<>(highlightsByPage.keySet());
            Collections.sort(sortedPages);

            for (int pageNumber : sortedPages) {
                List<Map<String, Object>> pageHighlights = highlightsByPage.get(pageNumber);

                content.append("第 ").append(pageNumber).append(" 页 (").append(pageHighlights.size()).append(" 处高亮):\n");

                for (int i = 0; i < pageHighlights.size(); i++) {
                    Map<String, Object> highlight = pageHighlights.get(i);

                    content.append("  ").append(i + 1).append(". ");
                    content.append("[").append(highlight.get("color")).append("] ");
                    content.append(highlight.get("text_content")).append("\n");

                    if (highlight.get("created_at") != null) {
                        content.append("     创建时间: ").append(highlight.get("created_at")).append("\n");
                    }
                }

                content.append("\n");
            }

            // 添加颜色统计
            Map<String, Integer> colorStats = new HashMap<>();
            for (Map<String, Object> highlight : highlights) {
                String color = (String) highlight.get("color");
                colorStats.put(color, colorStats.getOrDefault(color, 0) + 1);
            }

            content.append("颜色统计:\n");
            content.append("--------\n");
            for (Map.Entry<String, Integer> entry : colorStats.entrySet()) {
                content.append(entry.getKey()).append(": ").append(entry.getValue()).append(" 处\n");
            }
        }

        return content.toString();
    }

    // 获取文件扩展名
    private String getFileExtension(String format) {
        switch (format.toLowerCase()) {
            case "json": return "json";
            case "csv": return "csv";
            case "pdf": return "pdf";
            case "txt": return "txt";
            default: return "txt";
        }
    }

    // 获取媒体类型
    private MediaType getMediaType(String format) {
        switch (format.toLowerCase()) {
            case "json": return MediaType.APPLICATION_JSON;
            case "csv": return MediaType.parseMediaType("text/csv");
            case "pdf": return MediaType.APPLICATION_PDF;
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
}