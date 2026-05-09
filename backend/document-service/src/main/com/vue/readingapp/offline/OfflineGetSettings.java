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
public class OfflineGetSettings {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取离线设置请求 ===");
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
    public static class OfflineSettingsResponse {
        private boolean success;
        private String message;
        private OfflineSettingsData data;

        public OfflineSettingsResponse(boolean success, String message, OfflineSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public OfflineSettingsData getData() { return data; }
        public void setData(OfflineSettingsData data) { this.data = data; }
    }

    public static class OfflineSettingsData {
        private boolean offlineMode;
        private boolean autoSync;
        private int syncInterval;
        private int storageLimit;
        private boolean wifiOnly;
        private String lastSyncTime;
        private String updatedAt;

        public OfflineSettingsData(boolean offlineMode, boolean autoSync, int syncInterval,
                                   int storageLimit, boolean wifiOnly, String lastSyncTime,
                                   String updatedAt) {
            this.offlineMode = offlineMode;
            this.autoSync = autoSync;
            this.syncInterval = syncInterval;
            this.storageLimit = storageLimit;
            this.wifiOnly = wifiOnly;
            this.lastSyncTime = lastSyncTime;
            this.updatedAt = updatedAt;
        }

        // Getters and Setters
        public boolean isOfflineMode() { return offlineMode; }
        public void setOfflineMode(boolean offlineMode) { this.offlineMode = offlineMode; }

        public boolean isAutoSync() { return autoSync; }
        public void setAutoSync(boolean autoSync) { this.autoSync = autoSync; }

        public int getSyncInterval() { return syncInterval; }
        public void setSyncInterval(int syncInterval) { this.syncInterval = syncInterval; }

        public int getStorageLimit() { return storageLimit; }
        public void setStorageLimit(int storageLimit) { this.storageLimit = storageLimit; }

        public boolean isWifiOnly() { return wifiOnly; }
        public void setWifiOnly(boolean wifiOnly) { this.wifiOnly = wifiOnly; }

        public String getLastSyncTime() { return lastSyncTime; }
        public void setLastSyncTime(String lastSyncTime) { this.lastSyncTime = lastSyncTime; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
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

    @GetMapping("/settings")
    public ResponseEntity<OfflineSettingsResponse> getOfflineSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new OfflineSettingsResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new OfflineSettingsResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 查询所有设置
            String querySql = "SELECT setting_key, setting_value, updated_at FROM offline_settings WHERE user_id = ?";
            List<Map<String, Object>> settings = jdbcTemplate.queryForList(querySql, userId);
            printQueryResult(settings);

            // 3. 解析设置值
            boolean offlineMode = false;
            boolean autoSync = true;
            int syncInterval = 5;
            int storageLimit = 1024;
            boolean wifiOnly = true;
            String lastSyncTime = null;
            String updatedAt = null;

            for (Map<String, Object> row : settings) {
                String key = (String) row.get("setting_key");
                String value = (String) row.get("setting_value");

                switch (key) {
                    case "offlineMode":
                        offlineMode = Boolean.parseBoolean(value);
                        break;
                    case "autoSync":
                        autoSync = Boolean.parseBoolean(value);
                        break;
                    case "syncInterval":
                        syncInterval = Integer.parseInt(value);
                        break;
                    case "storageLimit":
                        storageLimit = Integer.parseInt(value);
                        break;
                    case "wifiOnly":
                        wifiOnly = Boolean.parseBoolean(value);
                        break;
                    case "lastSyncTime":
                        lastSyncTime = value;
                        break;
                }

                if (updatedAt == null) {
                    updatedAt = row.get("updated_at").toString();
                }
            }

            // 4. 如果没有设置，使用默认值
            if (settings.isEmpty()) {
                // 可以在这里插入默认设置
                String insertSql = "INSERT INTO offline_settings (user_id, setting_key, setting_value) VALUES " +
                        "(?, 'offlineMode', 'false'), " +
                        "(?, 'autoSync', 'true'), " +
                        "(?, 'syncInterval', '5'), " +
                        "(?, 'storageLimit', '1024'), " +
                        "(?, 'wifiOnly', 'true')";
                jdbcTemplate.update(insertSql, userId, userId, userId, userId, userId);

                updatedAt = "刚刚创建";
            }

            // 5. 准备响应数据
            OfflineSettingsData data = new OfflineSettingsData(
                    offlineMode,
                    autoSync,
                    syncInterval,
                    storageLimit,
                    wifiOnly,
                    lastSyncTime,
                    updatedAt
            );

            OfflineSettingsResponse response = new OfflineSettingsResponse(true, "获取离线设置成功", data);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取离线设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new OfflineSettingsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
