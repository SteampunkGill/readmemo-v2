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
public class GetExportHistory {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到导出历史查询请求 ===");
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
    public static class GetExportHistoryRequest {
        private int page = 1;
        private int page_size = 20;
        private String sort_by = "createdAt";
        private String sort_order = "desc";

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getPage_size() { return page_size; }
        public void setPage_size(int page_size) { this.page_size = page_size; }

        public String getSort_by() { return sort_by; }
        public void setSort_by(String sort_by) { this.sort_by = sort_by; }

        public String getSort_order() { return sort_order; }
        public void setSort_order(String sort_order) { this.sort_order = sort_order; }
    }

    // 响应DTO
    public static class GetExportHistoryResponse {
        private boolean success = true;
        private String message = "查询成功";
        private ExportHistoryData data;

        public GetExportHistoryResponse(ExportHistoryData data) {
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ExportHistoryData getData() { return data; }
        public void setData(ExportHistoryData data) { this.data = data; }
    }

    public static class ExportHistoryData {
        private List<ExportHistoryItem> items;
        private PaginationInfo pagination;

        public ExportHistoryData(List<ExportHistoryItem> items, PaginationInfo pagination) {
            this.items = items;
            this.pagination = pagination;
        }

        public List<ExportHistoryItem> getItems() { return items; }
        public void setItems(List<ExportHistoryItem> items) { this.items = items; }

        public PaginationInfo getPagination() { return pagination; }
        public void setPagination(PaginationInfo pagination) { this.pagination = pagination; }
    }

    public static class ExportHistoryItem {
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
        private Map<String, Object> metadata;

        // 构造函数
        public ExportHistoryItem(String id, String type, String format, int itemCount,
                                 String filename, String fileSize, boolean isBackup,
                                 int userId, LocalDateTime createdAt, String status) {
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
            this.metadata = new HashMap<>();
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

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    public static class PaginationInfo {
        private int page;
        private int pageSize;
        private int totalItems;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;

        public PaginationInfo(int page, int pageSize, int totalItems) {
            this.page = page;
            this.pageSize = pageSize;
            this.totalItems = totalItems;
            this.totalPages = (int) Math.ceil((double) totalItems / pageSize);
            this.hasNext = page < totalPages;
            this.hasPrevious = page > 1;
        }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public int getTotalItems() { return totalItems; }
        public void setTotalItems(int totalItems) { this.totalItems = totalItems; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

        public boolean isHasNext() { return hasNext; }
        public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }

        public boolean isHasPrevious() { return hasPrevious; }
        public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
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

    @GetMapping("/history")
    public ResponseEntity<?> getExportHistory(@RequestParam(required = false, defaultValue = "1") int page,
                                              @RequestParam(required = false, defaultValue = "20") int page_size,
                                              @RequestParam(required = false, defaultValue = "createdAt") String sort_by,
                                              @RequestParam(required = false, defaultValue = "desc") String sort_order,
                                              @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 构建请求对象
        GetExportHistoryRequest request = new GetExportHistoryRequest();
        request.setPage(page);
        request.setPage_size(page_size);
        request.setSort_by(sort_by);
        request.setSort_order(sort_order);

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
            if (page < 1) {
                page = 1;
            }

            if (page_size < 1 || page_size > 100) {
                page_size = 20;
            }

            // 验证排序字段
            Set<String> validSortFields = new HashSet<>(Arrays.asList(
                    "createdAt", "type", "format", "status", "itemCount"
            ));

            if (!validSortFields.contains(sort_by)) {
                sort_by = "createdAt";
            }

            if (!"asc".equalsIgnoreCase(sort_order) && !"desc".equalsIgnoreCase(sort_order)) {
                sort_order = "desc";
            }

            // 3. 计算分页参数
            int offset = (page - 1) * page_size;

            // 4. 查询总记录数
            String countSql = "SELECT COUNT(*) as total FROM sync_logs " +
                    "WHERE user_id = ? AND operation_type = 'EXPORT'";
            int totalItems = jdbcTemplate.queryForObject(countSql, Integer.class, userId);

            // 5. 查询导出历史记录
            String historySql = "SELECT log_id, operation_type, entity_type, entity_ids, " +
                    "status, details, created_at FROM sync_logs " +
                    "WHERE user_id = ? AND operation_type = 'EXPORT' " +
                    "ORDER BY " + sort_by + " " + sort_order + " " +
                    "LIMIT ? OFFSET ?";

            List<Map<String, Object>> historyResults = jdbcTemplate.queryForList(
                    historySql, userId, page_size, offset
            );

            printQueryResult("导出历史查询结果: " + historyResults.size() + "条记录");

            // 6. 转换结果
            List<ExportHistoryItem> historyItems = new ArrayList<>();

            for (Map<String, Object> record : historyResults) {
                int logId = (int) record.get("log_id");
                String entityType = (String) record.get("entity_type");
                String status = (String) record.get("status");
                LocalDateTime createdAt = ((java.sql.Timestamp) record.get("created_at")).toLocalDateTime();

                // 解析details字段
                Map<String, Object> details = new HashMap<>();
                String detailsJson = (String) record.get("details");

                // 简单解析details JSON（实际项目中应使用JSON库）
                if (detailsJson != null && detailsJson.startsWith("{")) {
                    // 移除花括号并分割键值对
                    String content = detailsJson.substring(1, detailsJson.length() - 1);
                    String[] pairs = content.split(",");

                    for (String pair : pairs) {
                        String[] keyValue = pair.split(":");
                        if (keyValue.length == 2) {
                            String key = keyValue[0].trim().replace("\"", "");
                            String value = keyValue[1].trim().replace("\"", "");
                            details.put(key, value);
                        }
                    }
                }

                // 从details中提取信息
                String exportId = (String) details.getOrDefault("export_id", "export_" + logId);
                String format = (String) details.getOrDefault("format", "unknown");
                int itemCount = Integer.parseInt(details.getOrDefault("item_count", "0").toString());
                String filename = (String) details.getOrDefault("filename", "unknown");
                String fileSize = (String) details.getOrDefault("file_size", "0");
                boolean isBackup = Boolean.parseBoolean(details.getOrDefault("is_backup", "false").toString());

                ExportHistoryItem item = new ExportHistoryItem(
                        exportId, entityType, format, itemCount, filename,
                        fileSize, isBackup, userId, createdAt, status
                );

                // 添加metadata
                item.setMetadata(details);

                historyItems.add(item);
            }

            // 7. 构建分页信息
            PaginationInfo pagination = new PaginationInfo(page, page_size, totalItems);

            // 8. 构建响应数据
            ExportHistoryData historyData = new ExportHistoryData(historyItems, pagination);
            GetExportHistoryResponse response = new GetExportHistoryResponse(historyData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("查询导出历史过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("HISTORY_QUERY_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}