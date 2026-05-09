package com.vue.readingapp.export;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/export")
public class CleanupExportFiles {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到清理导出文件请求 ===");
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
    public static class CleanupExportFilesResponse {
        private boolean success = true;
        private String message = "清理成功";
        private CleanupData data;

        public CleanupExportFilesResponse(CleanupData data) {
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public CleanupData getData() { return data; }
        public void setData(CleanupData data) { this.data = data; }
    }

    public static class CleanupData {
        private int deletedCount;
        private long freedSpace;
        private Map<String, Integer> deletedByType;
        private LocalDateTime cleanupTime;

        public CleanupData(int deletedCount, long freedSpace, Map<String, Integer> deletedByType, LocalDateTime cleanupTime) {
            this.deletedCount = deletedCount;
            this.freedSpace = freedSpace;
            this.deletedByType = deletedByType;
            this.cleanupTime = cleanupTime;
        }

        public int getDeletedCount() { return deletedCount; }
        public void setDeletedCount(int deletedCount) { this.deletedCount = deletedCount; }

        public long getFreedSpace() { return freedSpace; }
        public void setFreedSpace(long freedSpace) { this.freedSpace = freedSpace; }

        public Map<String, Integer> getDeletedByType() { return deletedByType; }
        public void setDeletedByType(Map<String, Integer> deletedByType) { this.deletedByType = deletedByType; }

        public LocalDateTime getCleanupTime() { return cleanupTime; }
        public void setCleanupTime(LocalDateTime cleanupTime) { this.cleanupTime = cleanupTime; }
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

    @DeleteMapping("/cleanup")
    public ResponseEntity<?> cleanupExportFiles(@RequestParam(required = false, defaultValue = "30") int daysOld,
                                                @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest("daysOld=" + daysOld);

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
            if (daysOld < 1 || daysOld > 365) {
                Map<String, Object> details = new HashMap<>();
                details.put("daysOld", daysOld);
                details.put("valid_range", "1-365天");
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "清理天数应在1-365之间", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 3. 查询要清理的记录
            String findOldSql = "SELECT log_id, entity_type, details FROM sync_logs " +
                    "WHERE user_id = ? AND operation_type = 'EXPORT' " +
                    "AND created_at < DATE_SUB(NOW(), INTERVAL ? DAY) " +
                    "ORDER BY created_at";

            List<Map<String, Object>> oldRecords = jdbcTemplate.queryForList(findOldSql, userId, daysOld);
            printQueryResult("找到需要清理的记录: " + oldRecords.size() + "条");

            if (oldRecords.isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("daysOld", daysOld);
                details.put("recordsFound", 0);
                ErrorResponse errorResponse = new ErrorResponse("NO_RECORDS_TO_CLEANUP", "没有找到需要清理的记录", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // 4. 计算要释放的空间
            long totalFreedSpace = 0;
            Map<String, Integer> deletedByType = new HashMap<>();

            for (Map<String, Object> record : oldRecords) {
                String entityType = (String) record.get("entity_type");
                String detailsJson = (String) record.get("details");

                // 从details中提取文件大小
                if (detailsJson != null && detailsJson.contains("\"file_size\":")) {
                    try {
                        String sizePart = detailsJson.split("\"file_size\":")[1].split(",")[0].replace("\"", "").trim();
                        long fileSize = Long.parseLong(sizePart);
                        totalFreedSpace += fileSize;
                    } catch (Exception e) {
                        System.out.println("无法解析文件大小: " + detailsJson);
                    }
                }

                // 统计按类型删除的数量
                deletedByType.put(entityType, deletedByType.getOrDefault(entityType, 0) + 1);
            }

            // 5. 执行清理
            String deleteSql = "DELETE FROM sync_logs WHERE user_id = ? AND operation_type = 'EXPORT' " +
                    "AND created_at < DATE_SUB(NOW(), INTERVAL ? DAY)";

            int deletedCount = jdbcTemplate.update(deleteSql, userId, daysOld);

            if (deletedCount != oldRecords.size()) {
                System.out.println("警告: 预期删除 " + oldRecords.size() + " 条记录，实际删除 " + deletedCount + " 条");
            }

            // 6. 构建响应数据
            CleanupData cleanupData = new CleanupData(
                    deletedCount,
                    totalFreedSpace,
                    deletedByType,
                    LocalDateTime.now()
            );

            CleanupExportFilesResponse response = new CleanupExportFilesResponse(cleanupData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("清理导出文件过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            details.put("daysOld", daysOld);
            ErrorResponse errorResponse = new ErrorResponse("CLEANUP_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}