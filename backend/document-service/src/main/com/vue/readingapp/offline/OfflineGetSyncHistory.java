package com.vue.readingapp.offline;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/offline")
public class OfflineGetSyncHistory {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取同步历史请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("==========================");
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
    public static class SyncHistoryResponse {
        private boolean success;
        private String message;
        private SyncHistoryData data;

        public SyncHistoryResponse(boolean success, String message, SyncHistoryData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SyncHistoryData getData() { return data; }
        public void setData(SyncHistoryData data) { this.data = data; }
    }

    public static class SyncHistoryData {
        private int total;
        private int page;
        private int pageSize;
        private int totalPages;
        private List<SyncHistoryItem> items;

        public SyncHistoryData(int total, int page, int pageSize, int totalPages, List<SyncHistoryItem> items) {
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
            this.totalPages = totalPages;
            this.items = items;
        }

        // Getters and Setters
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

        public List<SyncHistoryItem> getItems() { return items; }
        public void setItems(List<SyncHistoryItem> items) { this.items = items; }
    }

    public static class SyncHistoryItem {
        private String syncHistId;
        private String taskId;
        private String taskType;
        private String status;
        private int syncedItems;
        private int failedItems;
        private double duration;
        private String error;
        private String startTime;
        private String endTime;
        private String createdAt;

        public SyncHistoryItem(String syncHistId, String taskId, String taskType, String status,
                               int syncedItems, int failedItems, double duration, String error,
                               String startTime, String endTime, String createdAt) {
            this.syncHistId = syncHistId;
            this.taskId = taskId;
            this.taskType = taskType;
            this.status = status;
            this.syncedItems = syncedItems;
            this.failedItems = failedItems;
            this.duration = duration;
            this.error = error;
            this.startTime = startTime;
            this.endTime = endTime;
            this.createdAt = createdAt;
        }

        // Getters and Setters
        public String getSyncHistId() { return syncHistId; }
        public void setSyncHistId(String syncHistId) { this.syncHistId = syncHistId; }

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }

        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public int getSyncedItems() { return syncedItems; }
        public void setSyncedItems(int syncedItems) { this.syncedItems = syncedItems; }

        public int getFailedItems() { return failedItems; }
        public void setFailedItems(int failedItems) { this.failedItems = failedItems; }

        public double getDuration() { return duration; }
        public void setDuration(double duration) { this.duration = duration; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }

        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    // 验证用户token
    private Integer validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            String sql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sql, token);

            if (sessions.isEmpty()) {
                return null;
            }

            return (Integer) sessions.get(0).get("user_id");
        } catch (Exception e) {
            System.err.println("验证token失败: " + e.getMessage());
            return null;
        }
    }

    @GetMapping("/sync/history")
    public ResponseEntity<SyncHistoryResponse> getSyncHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "type", defaultValue = "all") String type,
            @RequestParam(value = "status", required = false) String status) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("page", page);
        requestInfo.put("pageSize", pageSize);
        requestInfo.put("startDate", startDate);
        requestInfo.put("endDate", endDate);
        requestInfo.put("type", type);
        requestInfo.put("status", status);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SyncHistoryResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SyncHistoryResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 创建相关表（如果不存在）
            createOfflineSyncHistoryTableIfNotExists();

            // 3. 计算分页参数
            int offset = (page - 1) * pageSize;

            // 4. 构建查询条件
            StringBuilder whereClause = new StringBuilder("WHERE user_id = ?");
            List<Object> params = new ArrayList<>();
            params.add(userId);

            if (startDate != null && !startDate.trim().isEmpty()) {
                whereClause.append(" AND DATE(start_time) >= ?");
                params.add(startDate);
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                whereClause.append(" AND DATE(end_time) <= ?");
                params.add(endDate);
            }

            if (type != null && !type.equals("all")) {
                whereClause.append(" AND task_type = ?");
                params.add(type);
            }

            if (status != null && !status.trim().isEmpty()) {
                whereClause.append(" AND status = ?");
                params.add(status);
            }

            // 5. 查询总记录数
            String countSql = "SELECT COUNT(*) as total FROM offline_sync_history " + whereClause;
            List<Map<String, Object>> countResults = jdbcTemplate.queryForList(countSql, params.toArray());

            int total = 0;
            if (!countResults.isEmpty()) {
                total = ((Number) countResults.get(0).get("total")).intValue();
            }

            // 6. 查询历史记录
            String querySql = "SELECT * FROM offline_sync_history " + whereClause +
                    " ORDER BY created_at DESC LIMIT ? OFFSET ?";

            // 添加分页参数
            List<Object> queryParams = new ArrayList<>(params);
            queryParams.add(pageSize);
            queryParams.add(offset);

            List<Map<String, Object>> historyRecords = jdbcTemplate.queryForList(querySql, queryParams.toArray());
            printQueryResult(historyRecords);

            // 7. 转换为DTO对象
            List<SyncHistoryItem> items = new ArrayList<>();

            for (Map<String, Object> row : historyRecords) {
                SyncHistoryItem item = new SyncHistoryItem(
                        (String) row.get("sync_hist_id"),
                        (String) row.get("task_id"),
                        (String) row.get("task_type"),
                        (String) row.get("status"),
                        row.get("synced_items") != null ? ((Number) row.get("synced_items")).intValue() : 0,
                        row.get("failed_items") != null ? ((Number) row.get("failed_items")).intValue() : 0,
                        row.get("duration") != null ? ((Number) row.get("duration")).doubleValue() : 0.0,
                        (String) row.get("error"),
                        row.get("start_time") != null ? row.get("start_time").toString() : null,
                        row.get("end_time") != null ? row.get("end_time").toString() : null,
                        row.get("created_at") != null ? row.get("created_at").toString() : null
                );

                items.add(item);
            }

            // 8. 计算总页数
            int totalPages = (int) Math.ceil((double) total / pageSize);

            // 9. 准备响应数据
            SyncHistoryData data = new SyncHistoryData(total, page, pageSize, totalPages, items);
            SyncHistoryResponse response = new SyncHistoryResponse(true, "获取同步历史成功", data);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取同步历史过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SyncHistoryResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 创建 offline_sync_history 表（如果不存在）
    private void createOfflineSyncHistoryTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS offline_sync_history (" +
                "sync_hist_id VARCHAR(50) PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "task_id VARCHAR(50) NOT NULL, " +
                "task_type VARCHAR(50), " +
                "status VARCHAR(20) NOT NULL, " +
                "synced_items INT DEFAULT 0, " +
                "failed_items INT DEFAULT 0, " +
                "duration DOUBLE DEFAULT 0, " +
                "error TEXT, " +
                "start_time TIMESTAMP, " +
                "end_time TIMESTAMP, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                ")";

        try {
            jdbcTemplate.execute(sql);
            System.out.println("offline_sync_history 表已创建或已存在");
        } catch (Exception e) {
            System.err.println("创建 offline_sync_history 表失败: " + e.getMessage());
        }
    }
}