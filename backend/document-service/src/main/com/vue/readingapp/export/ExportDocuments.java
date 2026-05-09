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
public class ExportDocuments {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到文档导出请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=====================");
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
    public static class ExportDocumentsRequest {
        private List<Integer> document_ids;
        private String format = "pdf";
        private Map<String, Object> template;
        private boolean include_notes = true;
        private boolean include_highlights = true;

        public List<Integer> getDocument_ids() { return document_ids; }
        public void setDocument_ids(List<Integer> document_ids) { this.document_ids = document_ids; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public Map<String, Object> getTemplate() { return template; }
        public void setTemplate(Map<String, Object> template) { this.template = template; }

        public boolean isInclude_notes() { return include_notes; }
        public void setInclude_notes(boolean include_notes) { this.include_notes = include_notes; }

        public boolean isInclude_highlights() { return include_highlights; }
        public void setInclude_highlights(boolean include_highlights) { this.include_highlights = include_highlights; }
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

    // 导出历史记录DTO
    public static class ExportHistory {
        private String id;
        private String type;
        private String format;
        private int itemCount;
        private String filename;
        private String fileSize;
        private boolean isBackup;
        private int userId;
        private LocalDateTime createdAt;
        private String status;

        // 构造函数、getter和setter
        public ExportHistory(String id, String type, String format, int itemCount, String filename,
                             String fileSize, boolean isBackup, int userId, LocalDateTime createdAt, String status) {
            this.id = id;
            this.type = type;
            this.format = format;
            this.itemCount = itemCount;
            this.filename = filename;
            this.fileSize = fileSize;
            this.isBackup = isBackup;
            this.userId = userId;
            this.createdAt = createdAt;
            this.status = status;
        }

        // getter和setter方法
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public int getItemCount() { return itemCount; }
        public void setItemCount(int itemCount) { this.itemCount = itemCount; }

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }

        public String getFileSize() { return fileSize; }
        public void setFileSize(String fileSize) { this.fileSize = fileSize; }

        public boolean isBackup() { return isBackup; }
        public void setBackup(boolean backup) { isBackup = backup; }

        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // 支持的格式
    private static final Set<String> SUPPORTED_FORMATS = new HashSet<>(Arrays.asList(
            "pdf", "docx", "xlsx", "csv", "json", "html", "txt"
    ));

    @PostMapping("/documents/batch")
    public ResponseEntity<?> exportDocuments(@RequestBody ExportDocumentsRequest request,
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
            if (request.getDocument_ids() == null || request.getDocument_ids().isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("document_ids", "文档ID列表不能为空");
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (request.getDocument_ids().size() > 100) {
                Map<String, Object> details = new HashMap<>();
                details.put("document_ids", "最多支持导出100个文档");
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

            // 3. 验证文档是否存在且属于当前用户
            List<Integer> invalidDocIds = new ArrayList<>();
            List<Integer> validDocIds = new ArrayList<>();

            for (Integer docId : request.getDocument_ids()) {
                String docSql = "SELECT document_id FROM documents WHERE document_id = ? AND user_id = ?";
                List<Map<String, Object>> docs = jdbcTemplate.queryForList(docSql, docId, userId);

                if (docs.isEmpty()) {
                    invalidDocIds.add(docId);
                } else {
                    validDocIds.add(docId);
                }
            }

            if (!invalidDocIds.isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("documentIds", invalidDocIds);
                ErrorResponse errorResponse = new ErrorResponse("DOCUMENT_NOT_FOUND", "部分文档不存在或无权限访问", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // 4. 查询文档详细信息
            String placeholders = String.join(",", Collections.nCopies(validDocIds.size(), "?"));
            String querySql = "SELECT d.document_id, d.title, d.author, d.description, d.file_path, " +
                    "d.file_size, d.page_count, d.reading_progress, d.created_at, d.updated_at " +
                    "FROM documents d WHERE d.document_id IN (" + placeholders + ") AND d.user_id = ?";

            List<Object> params = new ArrayList<>(validDocIds);
            params.add(userId);

            List<Map<String, Object>> documents = jdbcTemplate.queryForList(querySql, params.toArray());
            printQueryResult(documents);

            if (documents.isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("documentIds", validDocIds);
                ErrorResponse errorResponse = new ErrorResponse("EXPORT_ERROR", "没有找到可导出的文档", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // 5. 查询文档笔记（如果需要）
            List<Map<String, Object>> notes = new ArrayList<>();
            if (request.isInclude_notes()) {
                String notesSql = "SELECT n.note_id, n.document_id, n.page_number, n.content, " +
                        "n.highlight_id, n.created_at, n.updated_at " +
                        "FROM document_notes n WHERE n.document_id IN (" + placeholders + ") AND n.user_id = ?";
                notes = jdbcTemplate.queryForList(notesSql, params.toArray());
                printQueryResult("笔记查询结果: " + notes.size() + "条记录");
            }

            // 6. 查询文档高亮（如果需要）
            List<Map<String, Object>> highlights = new ArrayList<>();
            if (request.isInclude_highlights()) {
                String highlightsSql = "SELECT h.highlight_id, h.document_id, h.page_number, h.text_content, " +
                        "h.color, h.start_position, h.end_position, h.created_at " +
                        "FROM document_highlights h WHERE h.document_id IN (" + placeholders + ") AND h.user_id = ?";
                highlights = jdbcTemplate.queryForList(highlightsSql, params.toArray());
                printQueryResult("高亮查询结果: " + highlights.size() + "条记录");
            }

            // 7. 生成导出文件内容（模拟）
            String fileContent = generateExportContent(documents, notes, highlights, request.getFormat());
            byte[] fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);

            // 8. 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String extension = getFileExtension(request.getFormat());
            String filename = "documents_" + timestamp + "." + extension;

            // 9. 记录导出历史
            String exportId = "export_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String insertHistorySql = "INSERT INTO sync_logs (user_id, operation_type, entity_type, entity_ids, " +
                    "status, details, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

            String entityIdsJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_ARRAY(?)", String.class,
                    String.join(",", validDocIds.stream().map(String::valueOf).toArray(String[]::new))
            );

            Map<String, Object> exportDetails = new HashMap<>();
            exportDetails.put("export_id", exportId);
            exportDetails.put("format", request.getFormat());
            exportDetails.put("item_count", documents.size());
            exportDetails.put("filename", filename);
            exportDetails.put("file_size", fileBytes.length);
            exportDetails.put("include_notes", request.isInclude_notes());
            exportDetails.put("include_highlights", request.isInclude_highlights());

            String detailsJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_OBJECT(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", String.class,
                    "export_id", exportId,
                    "format", request.getFormat(),
                    "item_count", documents.size(),
                    "filename", filename,
                    "file_size", fileBytes.length,
                    "include_notes", request.isInclude_notes(),
                    "include_highlights", request.isInclude_highlights()
            );

            jdbcTemplate.update(insertHistorySql,
                    userId,
                    "EXPORT",
                    "DOCUMENTS",
                    entityIdsJson,
                    "COMPLETED",
                    detailsJson,
                    LocalDateTime.now()
            );

            // 10. 准备文件响应
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(getMediaType(request.getFormat()));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(fileBytes.length);

            // 打印返回信息
            Map<String, Object> responseInfo = new HashMap<>();
            responseInfo.put("filename", filename);
            responseInfo.put("file_size", fileBytes.length);
            responseInfo.put("format", request.getFormat());
            responseInfo.put("document_count", documents.size());
            printResponse(responseInfo);

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("文档导出过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("EXPORT_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 生成导出内容（模拟）
    private String generateExportContent(List<Map<String, Object>> documents,
                                         List<Map<String, Object>> notes,
                                         List<Map<String, Object>> highlights,
                                         String format) {
        StringBuilder content = new StringBuilder();

        if (format.equalsIgnoreCase("json")) {
            // JSON格式
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("documents", documents);
            exportData.put("notes", notes);
            exportData.put("highlights", highlights);
            exportData.put("exported_at", LocalDateTime.now().toString());
            exportData.put("total_documents", documents.size());
            exportData.put("total_notes", notes.size());
            exportData.put("total_highlights", highlights.size());

            // 简单JSON序列化（实际项目中应使用Jackson等库）
            content.append("{\n");
            content.append("  \"exported_at\": \"").append(LocalDateTime.now()).append("\",\n");
            content.append("  \"total_documents\": ").append(documents.size()).append(",\n");
            content.append("  \"total_notes\": ").append(notes.size()).append(",\n");
            content.append("  \"total_highlights\": ").append(highlights.size()).append(",\n");
            content.append("  \"documents\": [\n");

            for (int i = 0; i < documents.size(); i++) {
                Map<String, Object> doc = documents.get(i);
                content.append("    {\n");
                content.append("      \"id\": ").append(doc.get("document_id")).append(",\n");
                content.append("      \"title\": \"").append(escapeJson(doc.get("title"))).append("\",\n");
                content.append("      \"author\": \"").append(escapeJson(doc.get("author"))).append("\",\n");
                content.append("      \"description\": \"").append(escapeJson(doc.get("description"))).append("\",\n");
                content.append("      \"page_count\": ").append(doc.get("page_count")).append(",\n");
                content.append("      \"created_at\": \"").append(doc.get("created_at")).append("\"\n");
                content.append("    }");
                if (i < documents.size() - 1) content.append(",");
                content.append("\n");
            }

            content.append("  ]\n");
            content.append("}");

        } else if (format.equalsIgnoreCase("csv")) {
            // CSV格式
            content.append("Document ID,Title,Author,Description,Page Count,Created At\n");
            for (Map<String, Object> doc : documents) {
                content.append(doc.get("document_id")).append(",");
                content.append("\"").append(escapeCsv(doc.get("title"))).append("\",");
                content.append("\"").append(escapeCsv(doc.get("author"))).append("\",");
                content.append("\"").append(escapeCsv(doc.get("description"))).append("\",");
                content.append(doc.get("page_count")).append(",");
                content.append("\"").append(doc.get("created_at")).append("\"\n");
            }

        } else if (format.equalsIgnoreCase("html")) {
            // HTML格式
            content.append("<!DOCTYPE html>\n");
            content.append("<html>\n");
            content.append("<head>\n");
            content.append("  <meta charset=\"UTF-8\">\n");
            content.append("  <title>文档导出报告</title>\n");
            content.append("  <style>\n");
            content.append("    body { font-family: Arial, sans-serif; margin: 20px; }\n");
            content.append("    h1 { color: #333; }\n");
            content.append("    table { border-collapse: collapse; width: 100%; }\n");
            content.append("    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
            content.append("    th { background-color: #f2f2f2; }\n");
            content.append("  </style>\n");
            content.append("</head>\n");
            content.append("<body>\n");
            content.append("  <h1>文档导出报告</h1>\n");
            content.append("  <p>导出时间: ").append(LocalDateTime.now()).append("</p>\n");
            content.append("  <p>文档数量: ").append(documents.size()).append("</p>\n");
            content.append("  <h2>文档列表</h2>\n");
            content.append("  <table>\n");
            content.append("    <tr>\n");
            content.append("      <th>ID</th>\n");
            content.append("      <th>标题</th>\n");
            content.append("      <th>作者</th>\n");
            content.append("      <th>描述</th>\n");
            content.append("      <th>页数</th>\n");
            content.append("      <th>创建时间</th>\n");
            content.append("    </tr>\n");

            for (Map<String, Object> doc : documents) {
                content.append("    <tr>\n");
                content.append("      <td>").append(doc.get("document_id")).append("</td>\n");
                content.append("      <td>").append(escapeHtml(doc.get("title"))).append("</td>\n");
                content.append("      <td>").append(escapeHtml(doc.get("author"))).append("</td>\n");
                content.append("      <td>").append(escapeHtml(doc.get("description"))).append("</td>\n");
                content.append("      <td>").append(doc.get("page_count")).append("</td>\n");
                content.append("      <td>").append(doc.get("created_at")).append("</td>\n");
                content.append("    </tr>\n");
            }

            content.append("  </table>\n");
            content.append("</body>\n");
            content.append("</html>\n");

        } else {
            // 默认文本格式
            content.append("文档导出报告\n");
            content.append("============\n\n");
            content.append("导出时间: ").append(LocalDateTime.now()).append("\n");
            content.append("文档数量: ").append(documents.size()).append("\n\n");

            for (Map<String, Object> doc : documents) {
                content.append("文档ID: ").append(doc.get("document_id")).append("\n");
                content.append("标题: ").append(doc.get("title")).append("\n");
                content.append("作者: ").append(doc.get("author")).append("\n");
                content.append("描述: ").append(doc.get("description")).append("\n");
                content.append("页数: ").append(doc.get("page_count")).append("\n");
                content.append("创建时间: ").append(doc.get("created_at")).append("\n");
                content.append("------------------------\n");
            }
        }

        return content.toString();
    }

    // 获取文件扩展名
    private String getFileExtension(String format) {
        switch (format.toLowerCase()) {
            case "pdf": return "pdf";
            case "docx": return "docx";
            case "xlsx": return "xlsx";
            case "csv": return "csv";
            case "json": return "json";
            case "html": return "html";
            case "txt": return "txt";
            default: return "txt";
        }
    }

    // 获取媒体类型
    private MediaType getMediaType(String format) {
        switch (format.toLowerCase()) {
            case "pdf": return MediaType.APPLICATION_PDF;
            case "docx": return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "xlsx": return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "csv": return MediaType.parseMediaType("text/csv");
            case "json": return MediaType.APPLICATION_JSON;
            case "html": return MediaType.TEXT_HTML;
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
            return str.replace("\"", "\"\"");
        }
        return str;
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