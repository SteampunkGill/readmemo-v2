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
public class ExportNotes {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到笔记导出请求 ===");
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
    public static class ExportNotesRequest {
        private List<Integer> note_ids;
        private String format = "pdf";
        private Map<String, Object> template;

        public List<Integer> getNote_ids() { return note_ids; }
        public void setNote_ids(List<Integer> note_ids) { this.note_ids = note_ids; }

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

    // 支持的格式
    private static final Set<String> SUPPORTED_FORMATS = new HashSet<>(Arrays.asList(
            "pdf", "html", "txt", "docx", "json"
    ));

    @PostMapping("/notes/batch")
    public ResponseEntity<?> exportNotes(@RequestBody ExportNotesRequest request,
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
            if (request.getNote_ids() == null || request.getNote_ids().isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("note_ids", "笔记ID列表不能为空");
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (request.getNote_ids().size() > 200) {
                Map<String, Object> details = new HashMap<>();
                details.put("note_ids", "最多支持导出200个笔记");
                details.put("limit", 200);
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (!SUPPORTED_FORMATS.contains(request.getFormat().toLowerCase())) {
                Map<String, Object> details = new HashMap<>();
                details.put("format", "不支持的导出格式: " + request.getFormat());
                details.put("supported_formats", SUPPORTED_FORMATS);
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 3. 验证笔记是否存在且属于当前用户
            List<Integer> invalidNoteIds = new ArrayList<>();
            List<Integer> validNoteIds = new ArrayList<>();

            for (Integer noteId : request.getNote_ids()) {
                String noteSql = "SELECT note_id FROM document_notes WHERE note_id = ? AND user_id = ?";
                List<Map<String, Object>> notes = jdbcTemplate.queryForList(noteSql, noteId, userId);

                if (notes.isEmpty()) {
                    invalidNoteIds.add(noteId);
                } else {
                    validNoteIds.add(noteId);
                }
            }

            if (!invalidNoteIds.isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("noteIds", invalidNoteIds);
                ErrorResponse errorResponse = new ErrorResponse("NOTES_NOT_FOUND", "部分笔记不存在或无权限访问", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // 4. 查询笔记详细信息
            String placeholders = String.join(",", Collections.nCopies(validNoteIds.size(), "?"));
            String notesSql = "SELECT n.note_id, n.document_id, d.title as document_title, " +
                    "n.page_number, n.content, n.highlight_id, " +
                    "n.created_at, n.updated_at " +
                    "FROM document_notes n " +
                    "LEFT JOIN documents d ON n.document_id = d.document_id " +
                    "WHERE n.note_id IN (" + placeholders + ") AND n.user_id = ? " +
                    "ORDER BY n.created_at DESC";

            List<Object> params = new ArrayList<>(validNoteIds);
            params.add(userId);

            List<Map<String, Object>> notes = jdbcTemplate.queryForList(notesSql, params.toArray());
            printQueryResult("笔记查询结果: " + notes.size() + "条记录");

            if (notes.isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("noteIds", validNoteIds);
                ErrorResponse errorResponse = new ErrorResponse("NOTES_EXPORT_ERROR", "没有找到可导出的笔记", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // 5. 生成导出文件内容
            String fileContent = generateNotesContent(notes, request.getFormat());
            byte[] fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);

            // 6. 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String extension = getFileExtension(request.getFormat());
            String filename = "notes_" + timestamp + "." + extension;

            // 7. 记录导出历史
            String exportId = "export_notes_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String insertHistorySql = "INSERT INTO sync_logs (user_id, operation_type, entity_type, entity_ids, " +
                    "status, details, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

            String entityIdsJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_ARRAY(?)", String.class,
                    String.join(",", validNoteIds.stream().map(String::valueOf).toArray(String[]::new))
            );

            Map<String, Object> exportDetails = new HashMap<>();
            exportDetails.put("export_id", exportId);
            exportDetails.put("format", request.getFormat());
            exportDetails.put("item_count", notes.size());
            exportDetails.put("filename", filename);
            exportDetails.put("file_size", fileBytes.length);

            String detailsJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_OBJECT(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", String.class,
                    "export_id", exportId,
                    "format", request.getFormat(),
                    "item_count", notes.size(),
                    "filename", filename,
                    "file_size", fileBytes.length
            );

            jdbcTemplate.update(insertHistorySql,
                    userId,
                    "EXPORT",
                    "NOTES",
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
            responseInfo.put("note_count", notes.size());
            printResponse(responseInfo);

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("笔记导出过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("NOTES_EXPORT_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 生成笔记内容
    private String generateNotesContent(List<Map<String, Object>> notes, String format) {
        StringBuilder content = new StringBuilder();

        if (format.equalsIgnoreCase("json")) {
            // JSON格式
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("metadata", new HashMap<String, Object>() {{
                put("exportedAt", LocalDateTime.now().toString());
                put("type", "notes");
                put("count", notes.size());
                put("format", "json");
                put("version", "1.0");
            }});

            exportData.put("notes", notes);

            // 简单JSON序列化
            content.append("{\n");
            content.append("  \"metadata\": {\n");
            content.append("    \"exportedAt\": \"").append(LocalDateTime.now()).append("\",\n");
            content.append("    \"type\": \"notes\",\n");
            content.append("    \"count\": ").append(notes.size()).append(",\n");
            content.append("    \"format\": \"json\",\n");
            content.append("    \"version\": \"1.0\"\n");
            content.append("  },\n");
            content.append("  \"notes\": [\n");

            for (int i = 0; i < notes.size(); i++) {
                Map<String, Object> note = notes.get(i);
                content.append("    {\n");
                content.append("      \"note_id\": ").append(note.get("note_id")).append(",\n");
                content.append("      \"document_id\": ").append(note.get("document_id")).append(",\n");
                content.append("      \"document_title\": \"").append(escapeJson(note.get("document_title"))).append("\",\n");
                content.append("      \"page_number\": ").append(note.get("page_number")).append(",\n");
                content.append("      \"content\": \"").append(escapeJson(note.get("content"))).append("\",\n");
                content.append("      \"created_at\": \"").append(note.get("created_at")).append("\",\n");
                content.append("      \"updated_at\": \"").append(note.get("updated_at")).append("\"\n");
                content.append("    }");
                if (i < notes.size() - 1) content.append(",");
                content.append("\n");
            }

            content.append("  ]\n");
            content.append("}");

        } else if (format.equalsIgnoreCase("html")) {
            // HTML格式
            content.append("<!DOCTYPE html>\n");
            content.append("<html>\n");
            content.append("<head>\n");
            content.append("  <meta charset=\"UTF-8\">\n");
            content.append("  <title>笔记导出</title>\n");
            content.append("  <style>\n");
            content.append("    body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; }\n");
            content.append("    h1 { color: #333; border-bottom: 2px solid #eee; padding-bottom: 10px; }\n");
            content.append("    .note { border: 1px solid #ddd; margin: 15px 0; padding: 15px; border-radius: 5px; }\n");
            content.append("    .note-header { background-color: #f5f5f5; padding: 10px; margin: -15px -15px 15px -15px; border-radius: 5px 5px 0 0; }\n");
            content.append("    .note-title { font-weight: bold; color: #555; }\n");
            content.append("    .note-meta { color: #888; font-size: 0.9em; margin-top: 5px; }\n");
            content.append("    .note-content { margin-top: 15px; }\n");
            content.append("  </style>\n");
            content.append("</head>\n");
            content.append("<body>\n");
            content.append("  <h1>笔记导出</h1>\n");
            content.append("  <p>导出时间: ").append(LocalDateTime.now()).append("</p>\n");
            content.append("  <p>笔记数量: ").append(notes.size()).append("</p>\n");
            content.append("  <hr>\n");

            for (Map<String, Object> note : notes) {
                content.append("  <div class=\"note\">\n");
                content.append("    <div class=\"note-header\">\n");
                content.append("      <div class=\"note-title\">").append(escapeHtml(note.get("document_title"))).append("</div>\n");
                content.append("      <div class=\"note-meta\">\n");
                content.append("        第").append(note.get("page_number")).append("页 | ");
                content.append("        创建: ").append(note.get("created_at")).append(" | ");
                content.append("        更新: ").append(note.get("updated_at")).append("\n");
                content.append("      </div>\n");
                content.append("    </div>\n");
                content.append("    <div class=\"note-content\">\n");
                content.append("      ").append(escapeHtml(note.get("content"))).append("\n");
                content.append("    </div>\n");
                content.append("  </div>\n");
            }

            content.append("</body>\n");
            content.append("</html>\n");

        } else {
            // 默认文本格式
            content.append("笔记导出报告\n");
            content.append("============\n\n");
            content.append("导出时间: ").append(LocalDateTime.now()).append("\n");
            content.append("笔记数量: ").append(notes.size()).append("\n");
            content.append("导出格式: ").append(format).append("\n\n");

            content.append("笔记列表:\n");
            content.append("--------\n");

            for (int i = 0; i < notes.size(); i++) {
                Map<String, Object> note = notes.get(i);
                content.append(i + 1).append(". ").append(note.get("document_title")).append("\n");
                content.append("   页码: 第").append(note.get("page_number")).append("页\n");
                content.append("   内容: ").append(note.get("content")).append("\n");
                content.append("   创建时间: ").append(note.get("created_at")).append("\n");
                content.append("   更新时间: ").append(note.get("updated_at")).append("\n");
                content.append("   -------------------------\n\n");
            }
        }

        return content.toString();
    }

    // 获取文件扩展名
    private String getFileExtension(String format) {
        switch (format.toLowerCase()) {
            case "pdf": return "pdf";
            case "html": return "html";
            case "txt": return "txt";
            case "docx": return "docx";
            case "json": return "json";
            default: return "txt";
        }
    }

    // 获取媒体类型
    private MediaType getMediaType(String format) {
        switch (format.toLowerCase()) {
            case "pdf": return MediaType.APPLICATION_PDF;
            case "html": return MediaType.TEXT_HTML;
            case "txt": return MediaType.TEXT_PLAIN;
            case "docx": return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
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

    // 转义HTML字符串
    private String escapeHtml(Object value) {
        if (value == null) return "";
        return value.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}