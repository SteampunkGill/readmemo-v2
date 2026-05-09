package com.vue.readingapp.export;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/export")
public class GetSupportedFormats {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取支持格式请求 ===");
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
    public static class GetSupportedFormatsResponse {
        private boolean success = true;
        private String message = "查询成功";
        private Map<String, Object> data;

        public GetSupportedFormatsResponse(Map<String, Object> data) {
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
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

    @GetMapping("/formats")
    public ResponseEntity<?> getSupportedFormats(@RequestParam(required = false) String type,
                                                 @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest("type=" + type);

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

            // 2. 定义所有支持的格式
            Map<String, Object> formatsData = new HashMap<>();

            // 2.1 文档导出支持的格式
            Map<String, Object> documentsFormats = new HashMap<>();
            documentsFormats.put("pdf", createFormatInfo("PDF", "application/pdf", "便携式文档格式", true, "适合打印和阅读"));
            documentsFormats.put("docx", createFormatInfo("Word", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Microsoft Word文档", false, "适合编辑和修改"));
            documentsFormats.put("xlsx", createFormatInfo("Excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Microsoft Excel表格", false, "适合数据分析和统计"));
            documentsFormats.put("csv", createFormatInfo("CSV", "text/csv", "逗号分隔值文件", false, "适合导入其他系统"));
            documentsFormats.put("json", createFormatInfo("JSON", "application/json", "JavaScript对象表示法", false, "适合程序处理和数据交换"));
            documentsFormats.put("html", createFormatInfo("HTML", "text/html", "超文本标记语言", false, "适合网页浏览"));
            documentsFormats.put("txt", createFormatInfo("文本", "text/plain", "纯文本文件", false, "适合简单查看"));

            // 2.2 词汇导出支持的格式
            Map<String, Object> vocabularyFormats = new HashMap<>();
            vocabularyFormats.put("xlsx", createFormatInfo("Excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Microsoft Excel表格", true, "推荐格式，包含完整信息"));
            vocabularyFormats.put("csv", createFormatInfo("CSV", "text/csv", "逗号分隔值文件", false, "适合导入Anki等软件"));
            vocabularyFormats.put("json", createFormatInfo("JSON", "application/json", "JavaScript对象表示法", false, "适合程序处理"));
            vocabularyFormats.put("pdf", createFormatInfo("PDF", "application/pdf", "便携式文档格式", false, "适合打印和阅读"));

            // 2.3 复习记录导出支持的格式
            Map<String, Object> reviewsFormats = new HashMap<>();
            reviewsFormats.put("csv", createFormatInfo("CSV", "text/csv", "逗号分隔值文件", true, "推荐格式，适合数据分析"));
            reviewsFormats.put("json", createFormatInfo("JSON", "application/json", "JavaScript对象表示法", false, "适合程序处理"));
            reviewsFormats.put("pdf", createFormatInfo("PDF", "application/pdf", "便携式文档格式", false, "适合打印报告"));
            reviewsFormats.put("xlsx", createFormatInfo("Excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Microsoft Excel表格", false, "适合统计和图表"));

            // 2.4 学习统计导出支持的格式
            Map<String, Object> statisticsFormats = new HashMap<>();
            statisticsFormats.put("pdf", createFormatInfo("PDF", "application/pdf", "便携式文档格式", true, "推荐格式，包含图表"));
            statisticsFormats.put("xlsx", createFormatInfo("Excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Microsoft Excel表格", false, "适合进一步分析"));
            statisticsFormats.put("csv", createFormatInfo("CSV", "text/csv", "逗号分隔值文件", false, "适合导入其他系统"));
            statisticsFormats.put("json", createFormatInfo("JSON", "application/json", "JavaScript对象表示法", false, "适合程序处理"));

            // 2.5 笔记导出支持的格式
            Map<String, Object> notesFormats = new HashMap<>();
            notesFormats.put("pdf", createFormatInfo("PDF", "application/pdf", "便携式文档格式", true, "适合打印和阅读"));
            notesFormats.put("html", createFormatInfo("HTML", "text/html", "超文本标记语言", false, "适合网页浏览"));
            notesFormats.put("txt", createFormatInfo("文本", "text/plain", "纯文本文件", false, "适合简单查看"));
            notesFormats.put("docx", createFormatInfo("Word", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Microsoft Word文档", false, "适合编辑"));

            // 2.6 高亮导出支持的格式
            Map<String, Object> highlightsFormats = new HashMap<>();
            highlightsFormats.put("json", createFormatInfo("JSON", "application/json", "JavaScript对象表示法", true, "推荐格式，保持数据结构"));
            highlightsFormats.put("csv", createFormatInfo("CSV", "text/csv", "逗号分隔值文件", false, "适合导入其他系统"));
            highlightsFormats.put("pdf", createFormatInfo("PDF", "application/pdf", "便携式文档格式", false, "适合打印"));

            // 2.7 阅读历史导出支持的格式
            Map<String, Object> readingHistoryFormats = new HashMap<>();
            readingHistoryFormats.put("csv", createFormatInfo("CSV", "text/csv", "逗号分隔值文件", true, "推荐格式，适合数据分析"));
            readingHistoryFormats.put("json", createFormatInfo("JSON", "application/json", "JavaScript对象表示法", false, "适合程序处理"));
            readingHistoryFormats.put("pdf", createFormatInfo("PDF", "application/pdf", "便携式文档格式", false, "适合打印报告"));

            // 2.8 批量导出支持的格式
            Map<String, Object> batchFormats = new HashMap<>();
            batchFormats.put("zip", createFormatInfo("ZIP", "application/zip", "压缩文件格式", true, "推荐格式，包含多个文件"));

            // 2.9 所有数据导出支持的格式
            Map<String, Object> allDataFormats = new HashMap<>();
            allDataFormats.put("json", createFormatInfo("JSON", "application/json", "JavaScript对象表示法", true, "推荐格式，保持完整数据结构"));

            // 3. 根据请求类型返回相应的格式
            if (type == null || type.isEmpty()) {
                // 返回所有类型的格式
                Map<String, Object> allFormats = new HashMap<>();
                allFormats.put("documents", documentsFormats);
                allFormats.put("vocabulary", vocabularyFormats);
                allFormats.put("reviews", reviewsFormats);
                allFormats.put("statistics", statisticsFormats);
                allFormats.put("notes", notesFormats);
                allFormats.put("highlights", highlightsFormats);
                allFormats.put("reading_history", readingHistoryFormats);
                allFormats.put("batch", batchFormats);
                allFormats.put("all_data", allDataFormats);

                formatsData.put("all_formats", allFormats);
                formatsData.put("total_types", 9);
            } else {
                // 返回特定类型的格式
                switch (type.toLowerCase()) {
                    case "documents":
                        formatsData.put("type", "documents");
                        formatsData.put("formats", documentsFormats);
                        formatsData.put("count", documentsFormats.size());
                        break;
                    case "vocabulary":
                        formatsData.put("type", "vocabulary");
                        formatsData.put("formats", vocabularyFormats);
                        formatsData.put("count", vocabularyFormats.size());
                        break;
                    case "reviews":
                        formatsData.put("type", "reviews");
                        formatsData.put("formats", reviewsFormats);
                        formatsData.put("count", reviewsFormats.size());
                        break;
                    case "statistics":
                        formatsData.put("type", "statistics");
                        formatsData.put("formats", statisticsFormats);
                        formatsData.put("count", statisticsFormats.size());
                        break;
                    case "notes":
                        formatsData.put("type", "notes");
                        formatsData.put("formats", notesFormats);
                        formatsData.put("count", notesFormats.size());
                        break;
                    case "highlights":
                        formatsData.put("type", "highlights");
                        formatsData.put("formats", highlightsFormats);
                        formatsData.put("count", highlightsFormats.size());
                        break;
                    case "reading_history":
                        formatsData.put("type", "reading_history");
                        formatsData.put("formats", readingHistoryFormats);
                        formatsData.put("count", readingHistoryFormats.size());
                        break;
                    case "batch":
                        formatsData.put("type", "batch");
                        formatsData.put("formats", batchFormats);
                        formatsData.put("count", batchFormats.size());
                        break;
                    case "all_data":
                        formatsData.put("type", "all_data");
                        formatsData.put("formats", allDataFormats);
                        formatsData.put("count", allDataFormats.size());
                        break;
                    default:
                        Map<String, Object> details = new HashMap<>();
                        details.put("type", type);
                        details.put("supported_types", Arrays.asList(
                                "documents", "vocabulary", "reviews", "statistics",
                                "notes", "highlights", "reading_history", "batch", "all_data"
                        ));
                        ErrorResponse errorResponse = new ErrorResponse("INVALID_TYPE", "不支持的导出类型", details);
                        printResponse(errorResponse);
                        return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // 4. 构建响应
            GetSupportedFormatsResponse response = new GetSupportedFormatsResponse(formatsData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取支持格式过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("FORMATS_QUERY_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 创建格式信息
    private Map<String, Object> createFormatInfo(String displayName, String mimeType,
                                                 String description, boolean isRecommended,
                                                 String recommendationReason) {
        Map<String, Object> formatInfo = new HashMap<>();
        formatInfo.put("display_name", displayName);
        formatInfo.put("mime_type", mimeType);
        formatInfo.put("description", description);
        formatInfo.put("is_recommended", isRecommended);
        formatInfo.put("recommendation_reason", recommendationReason);
        formatInfo.put("file_extension", getExtensionFromMimeType(mimeType));

        return formatInfo;
    }

    // 从MIME类型获取文件扩展名
    private String getExtensionFromMimeType(String mimeType) {
        switch (mimeType) {
            case "application/pdf": return "pdf";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document": return "docx";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": return "xlsx";
            case "text/csv": return "csv";
            case "application/json": return "json";
            case "text/html": return "html";
            case "text/plain": return "txt";
            case "application/zip": return "zip";
            default: return "bin";
        }
    }
}