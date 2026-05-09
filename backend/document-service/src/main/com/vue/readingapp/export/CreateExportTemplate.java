package com.vue.readingapp.export;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/export")
public class CreateExportTemplate {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到创建导出模板请求 ===");
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
    public static class CreateExportTemplateRequest {
        private String name;
        private String type;
        private String format;
        private String description;
        private Map<String, Object> config;
        private boolean isDefault = false;
        private boolean isPublic = false;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }

        public boolean isDefault() { return isDefault; }
        public void setDefault(boolean aDefault) { isDefault = aDefault; }

        public boolean isPublic() { return isPublic; }
        public void setPublic(boolean aPublic) { isPublic = aPublic; }
    }

    // 响应DTO
    public static class CreateExportTemplateResponse {
        private boolean success = true;
        private String message = "创建成功";
        private TemplateData data;

        public CreateExportTemplateResponse(TemplateData data) {
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public TemplateData getData() { return data; }
        public void setData(TemplateData data) { this.data = data; }
    }

    public static class TemplateData {
        private String id;
        private String name;
        private String type;
        private String format;
        private String description;
        private Map<String, Object> config;
        private boolean isDefault;
        private boolean isPublic;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public TemplateData(String id, String name, String type, String format,
                            String description, Map<String, Object> config,
                            boolean isDefault, boolean isPublic,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.format = format;
            this.description = description;
            this.config = config;
            this.isDefault = isDefault;
            this.isPublic = isPublic;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }

        public boolean isDefault() { return isDefault; }
        public void setDefault(boolean aDefault) { isDefault = aDefault; }

        public boolean isPublic() { return isPublic; }
        public void setPublic(boolean aPublic) { isPublic = aPublic; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
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
            "documents", "vocabulary", "reviews", "statistics",
            "notes", "highlights", "reading_history", "batch", "all_data"
    ));

    // 支持的格式
    private static final Map<String, Set<String>> SUPPORTED_FORMATS_BY_TYPE = new HashMap<>();

    static {
        SUPPORTED_FORMATS_BY_TYPE.put("documents", new HashSet<>(Arrays.asList("pdf", "docx", "xlsx", "csv", "json", "html", "txt")));
        SUPPORTED_FORMATS_BY_TYPE.put("vocabulary", new HashSet<>(Arrays.asList("xlsx", "csv", "json", "pdf")));
        SUPPORTED_FORMATS_BY_TYPE.put("reviews", new HashSet<>(Arrays.asList("csv", "json", "pdf", "xlsx")));
        SUPPORTED_FORMATS_BY_TYPE.put("statistics", new HashSet<>(Arrays.asList("pdf", "xlsx", "csv", "json")));
        SUPPORTED_FORMATS_BY_TYPE.put("notes", new HashSet<>(Arrays.asList("pdf", "html", "txt", "docx")));
        SUPPORTED_FORMATS_BY_TYPE.put("highlights", new HashSet<>(Arrays.asList("json", "csv", "pdf")));
        SUPPORTED_FORMATS_BY_TYPE.put("reading_history", new HashSet<>(Arrays.asList("csv", "json", "pdf")));
        SUPPORTED_FORMATS_BY_TYPE.put("batch", new HashSet<>(Arrays.asList("zip")));
        SUPPORTED_FORMATS_BY_TYPE.put("all_data", new HashSet<>(Arrays.asList("json")));
    }

    @PostMapping("/templates")
    public ResponseEntity<?> createExportTemplate(@RequestBody CreateExportTemplateRequest request,
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
            Map<String, Object> validationErrors = new HashMap<>();

            if (request.getName() == null || request.getName().trim().isEmpty()) {
                validationErrors.put("name", "模板名称不能为空");
            } else if (request.getName().length() > 100) {
                validationErrors.put("name", "模板名称不能超过100个字符");
            }

            if (request.getType() == null || request.getType().trim().isEmpty()) {
                validationErrors.put("type", "模板类型不能为空");
            } else if (!SUPPORTED_TYPES.contains(request.getType().toLowerCase())) {
                validationErrors.put("type", "不支持的模板类型: " + request.getType());
                validationErrors.put("supported_types", SUPPORTED_TYPES);
            }

            if (request.getFormat() == null || request.getFormat().trim().isEmpty()) {
                validationErrors.put("format", "导出格式不能为空");
            } else if (request.getType() != null && SUPPORTED_FORMATS_BY_TYPE.containsKey(request.getType().toLowerCase())) {
                Set<String> supportedFormats = SUPPORTED_FORMATS_BY_TYPE.get(request.getType().toLowerCase());
                if (!supportedFormats.contains(request.getFormat().toLowerCase())) {
                    validationErrors.put("format", "不支持的导出格式: " + request.getFormat() + " 对于类型: " + request.getType());
                    validationErrors.put("supported_formats", supportedFormats);
                }
            }

            if (request.getDescription() != null && request.getDescription().length() > 500) {
                validationErrors.put("description", "描述不能超过500个字符");
            }

            if (request.getConfig() == null) {
                validationErrors.put("config", "配置不能为空");
            }

            if (!validationErrors.isEmpty()) {
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数验证失败", validationErrors);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 3. 检查是否已存在同名模板
            String checkSql = "SELECT COUNT(*) as count FROM export_templates WHERE user_id = ? AND template_name = ?";
            int existingCount = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, request.getName());

            if (existingCount > 0) {
                Map<String, Object> details = new HashMap<>();
                details.put("name", request.getName());
                details.put("user_id", userId);
                ErrorResponse errorResponse = new ErrorResponse("DUPLICATE_TEMPLATE", "已存在同名模板", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

            // 4. 如果设置为默认模板，先取消其他默认模板
            if (request.isDefault()) {
                String unsetDefaultSql = "UPDATE export_templates SET is_default = false WHERE user_id = ? AND template_type = ?";
                jdbcTemplate.update(unsetDefaultSql, userId, request.getType());
            }

            // 5. 创建模板
            LocalDateTime now = LocalDateTime.now();

            // 将config Map转换为JSON字符串
            String configJson = convertMapToJson(request.getConfig());

            String insertSql = "INSERT INTO export_templates (user_id, template_name, template_type, " +
                    "export_format, description, config, is_default, is_public, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(insertSql,
                    userId,
                    request.getName(),
                    request.getType(),
                    request.getFormat(),
                    request.getDescription() != null ? request.getDescription() : "",
                    configJson,
                    request.isDefault(),
                    request.isPublic(),
                    now,
                    now
            );

            // 6. 获取新创建的模板ID
            String getIdSql = "SELECT LAST_INSERT_ID() as template_id";
            int templateId = jdbcTemplate.queryForObject(getIdSql, Integer.class);

            // 7. 构建响应数据
            String templateIdStr = "template_" + templateId;
            TemplateData templateData = new TemplateData(
                    templateIdStr,
                    request.getName(),
                    request.getType(),
                    request.getFormat(),
                    request.getDescription(),
                    request.getConfig(),
                    request.isDefault(),
                    request.isPublic(),
                    now,
                    now
            );

            CreateExportTemplateResponse response = new CreateExportTemplateResponse(templateData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            System.err.println("创建导出模板过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("TEMPLATE_CREATION_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 将Map转换为JSON字符串（简化版）
    private String convertMapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }

        StringBuilder json = new StringBuilder();
        json.append("{");

        int count = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (count > 0) {
                json.append(",");
            }

            json.append("\"").append(entry.getKey()).append("\":");

            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escapeJson(value.toString())).append("\"");
            } else if (value instanceof Number) {
                json.append(value);
            } else if (value instanceof Boolean) {
                json.append(value);
            } else if (value instanceof Map) {
                json.append(convertMapToJson((Map<String, Object>) value));
            } else {
                json.append("\"").append(escapeJson(value.toString())).append("\"");
            }

            count++;
        }

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