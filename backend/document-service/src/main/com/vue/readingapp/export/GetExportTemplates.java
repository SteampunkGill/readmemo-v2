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
public class GetExportTemplates {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到导出模板查询请求 ===");
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

    // 响应DTO
    public static class GetExportTemplatesResponse {
        private boolean success = true;
        private String message = "查询成功";
        private List<ExportTemplate> data;

        public GetExportTemplatesResponse(List<ExportTemplate> data) {
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<ExportTemplate> getData() { return data; }
        public void setData(List<ExportTemplate> data) { this.data = data; }
    }

    public static class ExportTemplate {
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

        // 构造函数
        public ExportTemplate(String id, String name, String type, String format,
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

        // getter和setter方法
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

    @GetMapping("/templates")
    public ResponseEntity<?> getExportTemplates(@RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest("查询所有导出模板");

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

            // 2. 查询导出模板
            // 首先查询系统默认模板和公共模板
            List<ExportTemplate> templates = new ArrayList<>();

            // 2.1 添加系统默认模板
            templates.addAll(getSystemDefaultTemplates());

            // 2.2 查询用户自定义模板（如果有对应的表）
            try {
                String userTemplatesSql = "SELECT template_id, template_name, template_type, " +
                        "export_format, description, config, is_default, " +
                        "is_public, created_at, updated_at FROM export_templates " +
                        "WHERE (user_id = ? OR is_public = true) " +
                        "ORDER BY is_default DESC, created_at DESC";

                List<Map<String, Object>> userTemplateResults = jdbcTemplate.queryForList(
                        userTemplatesSql, userId
                );

                for (Map<String, Object> templateRecord : userTemplateResults) {
                    String id = "template_" + templateRecord.get("template_id");
                    String name = (String) templateRecord.get("template_name");
                    String type = (String) templateRecord.get("template_type");
                    String format = (String) templateRecord.get("export_format");
                    String description = (String) templateRecord.get("description");

                    // 解析config字段
                    Map<String, Object> config = new HashMap<>();
                    String configJson = (String) templateRecord.get("config");
                    if (configJson != null && configJson.startsWith("{")) {
                        // 简单解析JSON
                        String content = configJson.substring(1, configJson.length() - 1);
                        String[] pairs = content.split(",");

                        for (String pair : pairs) {
                            String[] keyValue = pair.split(":");
                            if (keyValue.length == 2) {
                                String key = keyValue[0].trim().replace("\"", "");
                                String value = keyValue[1].trim().replace("\"", "");
                                config.put(key, value);
                            }
                        }
                    }

                    boolean isDefault = Boolean.parseBoolean(templateRecord.getOrDefault("is_default", "false").toString());
                    boolean isPublic = Boolean.parseBoolean(templateRecord.getOrDefault("is_public", "false").toString());
                    LocalDateTime createdAt = ((java.sql.Timestamp) templateRecord.get("created_at")).toLocalDateTime();
                    LocalDateTime updatedAt = templateRecord.get("updated_at") != null ?
                            ((java.sql.Timestamp) templateRecord.get("updated_at")).toLocalDateTime() : createdAt;

                    ExportTemplate template = new ExportTemplate(
                            id, name, type, format, description, config,
                            isDefault, isPublic, createdAt, updatedAt
                    );

                    templates.add(template);
                }

                printQueryResult("用户自定义模板查询结果: " + userTemplateResults.size() + "条记录");

            } catch (Exception e) {
                System.out.println("导出模板表不存在，仅返回系统默认模板");
            }

            // 3. 构建响应
            GetExportTemplatesResponse response = new GetExportTemplatesResponse(templates);

            // 打印返回数据
            printResponse("返回模板数量: " + templates.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("查询导出模板过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("TEMPLATES_QUERY_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 获取系统默认模板
    private List<ExportTemplate> getSystemDefaultTemplates() {
        List<ExportTemplate> systemTemplates = new ArrayList<>();

        // 1. 文档导出PDF模板
        Map<String, Object> docPdfConfig = new HashMap<>();
        docPdfConfig.put("pageSize", "A4");
        docPdfConfig.put("margin", "2cm");
        docPdfConfig.put("fontSize", "12pt");
        docPdfConfig.put("includeHeader", true);
        docPdfConfig.put("includeFooter", true);
        docPdfConfig.put("watermark", false);
        docPdfConfig.put("pageNumbers", true);

        systemTemplates.add(new ExportTemplate(
                "system_doc_pdf_default",
                "PDF标准模板",
                "documents",
                "pdf",
                "标准PDF文档导出模板，适合打印和阅读",
                docPdfConfig,
                true,
                true,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().minusDays(10)
        ));

        // 2. 文档导出Word模板
        Map<String, Object> docDocxConfig = new HashMap<>();
        docDocxConfig.put("fontFamily", "Calibri");
        docDocxConfig.put("fontSize", "11pt");
        docDocxConfig.put("lineSpacing", "1.5");
        docDocxConfig.put("includeToc", true);
        docDocxConfig.put("headerLevels", 3);

        systemTemplates.add(new ExportTemplate(
                "system_doc_docx_default",
                "Word标准模板",
                "documents",
                "docx",
                "标准Word文档导出模板，适合编辑和修改",
                docDocxConfig,
                false,
                true,
                LocalDateTime.now().minusDays(25),
                LocalDateTime.now().minusDays(8)
        ));

        // 3. 词汇导出Excel模板
        Map<String, Object> vocabXlsxConfig = new HashMap<>();
        vocabXlsxConfig.put("includePhonetic", true);
        vocabXlsxConfig.put("includeExamples", true);
        vocabXlsxConfig.put("includeTags", true);
        vocabXlsxConfig.put("includeStatistics", false);
        vocabXlsxConfig.put("autoFilter", true);
        vocabXlsxConfig.put("freezeHeader", true);
        vocabXlsxConfig.put("columnWidth", new HashMap<String, Object>() {{
            put("word", 15);
            put("phonetic", 12);
            put("meaning", 25);
            put("partOfSpeech", 10);
        }});

        systemTemplates.add(new ExportTemplate(
                "system_vocab_xlsx_default",
                "Excel词汇表模板",
                "vocabulary",
                "xlsx",
                "标准Excel词汇导出模板，包含单词、音标、释义等信息",
                vocabXlsxConfig,
                true,
                true,
                LocalDateTime.now().minusDays(20),
                LocalDateTime.now().minusDays(5)
        ));

        // 4. 复习记录导出CSV模板
        Map<String, Object> reviewCsvConfig = new HashMap<>();
        reviewCsvConfig.put("delimiter", ",");
        reviewCsvConfig.put("includeHeader", true);
        reviewCsvConfig.put("dateFormat", "yyyy-MM-dd HH:mm:ss");
        reviewCsvConfig.put("encoding", "UTF-8");

        systemTemplates.add(new ExportTemplate(
                "system_review_csv_default",
                "CSV复习记录模板",
                "reviews",
                "csv",
                "标准CSV复习记录导出模板，适合数据分析和导入",
                reviewCsvConfig,
                true,
                true,
                LocalDateTime.now().minusDays(15),
                LocalDateTime.now().minusDays(3)
        ));

        // 5. 学习统计导出PDF模板
        Map<String, Object> statsPdfConfig = new HashMap<>();
        statsPdfConfig.put("pageSize", "A4");
        statsPdfConfig.put("orientation", "portrait");
        statsPdfConfig.put("includeCharts", true);
        statsPdfConfig.put("chartType", "line");
        statsPdfConfig.put("colorScheme", "blue");

        systemTemplates.add(new ExportTemplate(
                "system_stats_pdf_default",
                "PDF学习报告模板",
                "statistics",
                "pdf",
                "标准PDF学习统计报告模板，包含图表和趋势分析",
                statsPdfConfig,
                true,
                true,
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(1)
        ));

        // 6. 笔记导出HTML模板
        Map<String, Object> notesHtmlConfig = new HashMap<>();
        notesHtmlConfig.put("theme", "light");
        notesHtmlConfig.put("fontSize", "14px");
        notesHtmlConfig.put("lineHeight", "1.6");
        notesHtmlConfig.put("includeCss", true);
        notesHtmlConfig.put("responsive", true);

        systemTemplates.add(new ExportTemplate(
                "system_notes_html_default",
                "HTML笔记模板",
                "notes",
                "html",
                "标准HTML笔记导出模板，适合网页浏览和分享",
                notesHtmlConfig,
                true,
                true,
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now()
        ));

        return systemTemplates;
    }
}