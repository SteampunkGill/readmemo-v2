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
public class DeleteExportHistory {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到删除导出历史请求 ===");
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
    public static class DeleteExportHistoryRequest {
        private String historyId;

        public String getHistoryId() { return historyId; }
        public void setHistoryId(String historyId) { this.historyId = historyId; }
    }

    // 响应DTO
    public static class DeleteExportHistoryResponse {
        private boolean success = true;
        private String message = "删除成功";
        private Map<String, Object> data;

        public DeleteExportHistoryResponse(Map<String, Object> data) {
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

    @DeleteMapping("/history/{historyId}")
    public ResponseEntity<?> deleteExportHistory(@PathVariable String historyId,
                                                 @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 构建请求对象
        DeleteExportHistoryRequest request = new DeleteExportHistoryRequest();
        request.setHistoryId(historyId);

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
            if (historyId == null || historyId.trim().isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("historyId", "历史记录ID不能为空");
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 3. 解析historyId，获取实际的log_id
            int logId = -1;

            if (historyId.startsWith("export_")) {
                // 尝试从historyId中提取数字部分
                try {
                    // 格式可能是: export_1234567890_abc123 或 export_123
                    String[] parts = historyId.split("_");
                    if (parts.length >= 2) {
                        // 尝试解析数字
                        String numericPart = parts[1];
                        logId = Integer.parseInt(numericPart);
                    }
                } catch (NumberFormatException e) {
                    // 如果解析失败，尝试查询数据库
                    System.out.println("无法从historyId解析log_id，尝试查询数据库");
                }
            }

            // 4. 查询要删除的记录是否存在且属于当前用户
            String checkSql;
            List<Object> checkParams = new ArrayList<>();

            if (logId > 0) {
                checkSql = "SELECT log_id, operation_type, entity_type, details FROM sync_logs " +
                        "WHERE log_id = ? AND user_id = ? AND operation_type = 'EXPORT'";
                checkParams.add(logId);
                checkParams.add(userId);
            } else {
                // 如果无法解析log_id，尝试通过details中的export_id查找
                checkSql = "SELECT log_id, operation_type, entity_type, details FROM sync_logs " +
                        "WHERE user_id = ? AND operation_type = 'EXPORT' " +
                        "AND details LIKE ?";
                checkParams.add(userId);
                checkParams.add("%\"" + historyId + "\"%");
            }

            List<Map<String, Object>> records = jdbcTemplate.queryForList(checkSql, checkParams.toArray());

            if (records.isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("historyId", historyId);
                details.put("userId", userId);
                ErrorResponse errorResponse = new ErrorResponse("HISTORY_NOT_FOUND", "未找到指定的导出历史记录", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // 5. 获取要删除的log_id
            Map<String, Object> record = records.get(0);
            int actualLogId = (int) record.get("log_id");

            // 6. 删除记录
            String deleteSql = "DELETE FROM sync_logs WHERE log_id = ? AND user_id = ?";
            int deletedCount = jdbcTemplate.update(deleteSql, actualLogId, userId);

            if (deletedCount == 0) {
                Map<String, Object> details = new HashMap<>();
                details.put("logId", actualLogId);
                details.put("userId", userId);
                ErrorResponse errorResponse = new ErrorResponse("DELETE_FAILED", "删除失败，记录可能已被删除", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }

            // 7. 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("deletedId", historyId);
            responseData.put("logId", actualLogId);
            responseData.put("deletedAt", LocalDateTime.now().toString());
            responseData.put("operationType", record.get("operation_type"));
            responseData.put("entityType", record.get("entity_type"));

            // 解析details字段
            String detailsJson = (String) record.get("details");
            if (detailsJson != null && detailsJson.startsWith("{")) {
                // 简单解析details
                Map<String, Object> detailsMap = new HashMap<>();
                String content = detailsJson.substring(1, detailsJson.length() - 1);
                String[] pairs = content.split(",");

                for (String pair : pairs) {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replace("\"", "");
                        String value = keyValue[1].trim().replace("\"", "");
                        detailsMap.put(key, value);
                    }
                }

                responseData.put("details", detailsMap);
            }

            DeleteExportHistoryResponse response = new DeleteExportHistoryResponse(responseData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("删除导出历史过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            details.put("historyId", historyId);
            ErrorResponse errorResponse = new ErrorResponse("HISTORY_DELETION_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}