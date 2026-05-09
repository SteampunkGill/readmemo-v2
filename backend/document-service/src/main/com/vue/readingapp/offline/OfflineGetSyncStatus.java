
package com.vue.readingapp.offline;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/offline")
public class OfflineGetSyncStatus {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取同步状态请求 ===");
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
    public static class SyncStatusResponse {
        private boolean success;
        private String message;
        private SyncStatusData data;

        public SyncStatusResponse(boolean success, String message, SyncStatusData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SyncStatusData getData() { return data; }
        public void setData(SyncStatusData data) { this.data = data; }
    }

    public static class SyncStatusData {
        private boolean isSyncing;
        private String lastSyncTime;
        private int syncProgress;
        private String syncError;
        private int pendingOperations;
        private String syncSpeed;
        private int syncedItems;
        private int failedItems;

        public SyncStatusData(boolean isSyncing, String lastSyncTime, int syncProgress,
                              String syncError, int pendingOperations, String syncSpeed,
                              int syncedItems, int failedItems) {
            this.isSyncing = isSyncing;
            this.lastSyncTime = lastSyncTime;
            this.syncProgress = syncProgress;
            this.syncError = syncError;
            this.pendingOperations = pendingOperations;
            this.syncSpeed = syncSpeed;
            this.syncedItems = syncedItems;
            this.failedItems = failedItems;
        }

        // Getters and Setters
        public boolean isSyncing() { return isSyncing; }
        public void setSyncing(boolean syncing) { isSyncing = syncing; }

        public String getLastSyncTime() { return lastSyncTime; }
        public void setLastSyncTime(String lastSyncTime) { this.lastSyncTime = lastSyncTime; }

        public int getSyncProgress() { return syncProgress; }
        public void setSyncProgress(int syncProgress) { this.syncProgress = syncProgress; }

        public String getSyncError() { return syncError; }
        public void setSyncError(String syncError) { this.syncError = syncError; }

        public int getPendingOperations() { return pendingOperations; }
        public void setPendingOperations(int pendingOperations) { this.pendingOperations = pendingOperations; }

        public String getSyncSpeed() { return syncSpeed; }
        public void setSyncSpeed(String syncSpeed) { this.syncSpeed = syncSpeed; }

        public int getSyncedItems() { return syncedItems; }
        public void setSyncedItems(int syncedItems) { this.syncedItems = syncedItems; }

        public int getFailedItems() { return failedItems; }
        public void setFailedItems(int failedItems) { this.failedItems = failedItems; }
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

    @GetMapping("/sync/status")
    public ResponseEntity<SyncStatusResponse> getSyncStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "type", defaultValue = "all") String type,
            @RequestParam(value = "status", defaultValue = "pending") String status) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("type", type);
        requestInfo.put("status", status);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SyncStatusResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SyncStatusResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 查询同步状态
            // 查询待同步操作数量
            String pendingSql = "SELECT COUNT(*) as count FROM offline_documents WHERE user_id = ? AND is_synced = FALSE";
            List<Map<String, Object>> pendingResults = jdbcTemplate.queryForList(pendingSql, userId);
            int pendingOperations = pendingResults.isEmpty() ? 0 : ((Number) pendingResults.get(0).get("count")).intValue();

            // 查询最近同步时间
            String lastSyncSql = "SELECT MAX(updated_at) as last_sync FROM offline_documents " +
                    "WHERE user_id = ? AND is_synced = TRUE";
            List<Map<String, Object>> lastSyncResults = jdbcTemplate.queryForList(lastSyncSql, userId);
            String lastSyncTime = null;
            if (!lastSyncResults.isEmpty() && lastSyncResults.get(0).get("last_sync") != null) {
                lastSyncTime = lastSyncResults.get(0).get("last_sync").toString();
            }

            // 查询同步统计
            String statsSql = "SELECT " +
                    "SUM(CASE WHEN is_synced = TRUE THEN 1 ELSE 0 END) as synced_items, " +
                    "SUM(CASE WHEN is_synced = FALSE THEN 1 ELSE 0 END) as failed_items " +
                    "FROM offline_documents WHERE user_id = ?";
            List<Map<String, Object>> statsResults = jdbcTemplate.queryForList(statsSql, userId);

            int syncedItems = 0;
            int failedItems = 0;
            if (!statsResults.isEmpty()) {
                Map<String, Object> row = statsResults.get(0);
                syncedItems = row.get("synced_items") != null ? ((Number) row.get("synced_items")).intValue() : 0;
                failedItems = row.get("failed_items") != null ? ((Number) row.get("failed_items")).intValue() : 0;
            }

            // 计算同步进度
            int totalItems = syncedItems + failedItems + pendingOperations;
            int syncProgress = totalItems > 0 ? (int) ((double) syncedItems / totalItems * 100) : 0;

            // 3. 准备响应数据
            SyncStatusData data = new SyncStatusData(
                    false, // 假设当前没有正在进行的同步
                    lastSyncTime,
                    syncProgress,
                    null, // 假设没有错误
                    pendingOperations,
                    "0 KB/s", // 假设没有正在进行的同步
                    syncedItems,
                    failedItems
            );

            printQueryResult(data);

            SyncStatusResponse response = new SyncStatusResponse(true, "获取同步状态成功", data);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取同步状态过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SyncStatusResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}