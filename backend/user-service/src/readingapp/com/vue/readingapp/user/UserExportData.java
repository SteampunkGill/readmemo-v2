package com.vue.readingapp.user;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/user")
public class UserExportData {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到导出用户数据请求 ===");
        System.out.println("请求参数: " + request);
        System.out.println("=================");
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
    public static class ExportDataRequest {
        private String format;
        private List<String> dataTypes;

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public List<String> getDataTypes() { return dataTypes; }
        public void setDataTypes(List<String> dataTypes) { this.dataTypes = dataTypes; }
    }

    // 响应DTO
    public static class ExportDataResponse {
        private boolean success;
        private String message;
        private ExportData data;

        public ExportDataResponse(boolean success, String message, ExportData data) {
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
        private String requestId;
        private String status;
        private String format;
        private String estimatedSize;
        private String downloadUrl;
        private String expiresAt;
        private String createdAt;

        public ExportData(String requestId, String status, String format, String estimatedSize,
                          String downloadUrl, String expiresAt, String createdAt) {
            this.requestId = requestId;
            this.status = status;
            this.format = format;
            this.estimatedSize = estimatedSize;
            this.downloadUrl = downloadUrl;
            this.expiresAt = expiresAt;
            this.createdAt = createdAt;
        }

        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public String getEstimatedSize() { return estimatedSize; }
        public void setEstimatedSize(String estimatedSize) { this.estimatedSize = estimatedSize; }

        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

        public String getExpiresAt() { return expiresAt; }
        public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    @GetMapping("/export-data")
    public ResponseEntity<ExportDataResponse> exportUserData(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "format", defaultValue = "json") String format,
            @RequestParam(value = "dataTypes", required = false) List<String> dataTypes) {

        // 打印接收到的请求
        Map<String, Object> params = new HashMap<>();
        params.put("format", format);
        params.put("dataTypes", dataTypes);
        printRequest(params);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ExportDataResponse(false, "请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 验证token有效性
            String tokenSql = "SELECT user_id, expires_at FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ExportDataResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            int userId = (int) session.get("user_id");

            // 3. 验证导出格式
            String[] validFormats = {"json", "csv", "xlsx"};
            boolean validFormat = false;
            for (String f : validFormats) {
                if (f.equals(format)) {
                    validFormat = true;
                    break;
                }
            }
            if (!validFormat) {
                format = "json";
            }

            // 4. 验证数据类型
            if (dataTypes == null || dataTypes.isEmpty()) {
                dataTypes = new ArrayList<>();
                dataTypes.add("profile");
                dataTypes.add("documents");
                dataTypes.add("vocabulary");
                dataTypes.add("reviews");
            }

            // 5. 生成请求ID
            String requestId = "export_" + System.currentTimeMillis() + "_" + userId;

            // 6. 估算数据大小
            int estimatedSize = 0;

            // 估算用户信息大小
            if (dataTypes.contains("profile")) {
                estimatedSize += 1024; // 约1KB
            }

            // 估算文档数据大小
            if (dataTypes.contains("documents")) {
                String docsCountSql = "SELECT COUNT(*) as count FROM documents WHERE user_id = ?";
                Map<String, Object> docsCountResult = jdbcTemplate.queryForMap(docsCountSql, userId);
                int docsCount = ((Number) docsCountResult.get("count")).intValue();
                estimatedSize += docsCount * 5120; // 每个文档约5KB
            }

            // 估算词汇数据大小
            if (dataTypes.contains("vocabulary")) {
                String vocabCountSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ?";
                Map<String, Object> vocabCountResult = jdbcTemplate.queryForMap(vocabCountSql, userId);
                int vocabCount = ((Number) vocabCountResult.get("count")).intValue();
                estimatedSize += vocabCount * 1024; // 每个单词约1KB
            }

            // 估算复习数据大小
            if (dataTypes.contains("reviews")) {
                String reviewsCountSql = "SELECT COUNT(*) as count FROM review_sessions WHERE user_id = ?";
                Map<String, Object> reviewsCountResult = jdbcTemplate.queryForMap(reviewsCountSql, userId);
                int reviewsCount = ((Number) reviewsCountResult.get("count")).intValue();
                estimatedSize += reviewsCount * 2048; // 每次复习约2KB
            }

            // 格式化估算大小
            String formattedSize;
            if (estimatedSize < 1024) {
                formattedSize = estimatedSize + " B";
            } else if (estimatedSize < 1024 * 1024) {
                formattedSize = String.format("%.1f KB", estimatedSize / 1024.0);
            } else {
                formattedSize = String.format("%.1f MB", estimatedSize / (1024.0 * 1024.0));
            }

            // 7. 生成下载URL（简化处理，实际应该生成一个临时文件）
            String downloadUrl = "/api/user/download-export/" + requestId + "." + format;

            // 8. 设置过期时间（24小时后）
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusHours(24);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // 9. 构建导出数据
            ExportData exportData = new ExportData(
                    requestId,
                    "processing",
                    format,
                    formattedSize,
                    downloadUrl,
                    expiresAt.format(formatter),
                    now.format(formatter)
            );

            printQueryResult(exportData);

            // 10. 准备响应数据
            ExportDataResponse response = new ExportDataResponse(
                    true, "用户数据导出请求已提交，请稍后查看下载链接", exportData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("导出用户数据过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ExportDataResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
