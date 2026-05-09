
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
public class OfflineUpdateSettings {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到更新离线设置请求 ===");
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

    // 请求DTO
    public static class UpdateSettingsRequest {
        private boolean offlineMode;
        private boolean autoSync;
        private int syncInterval;
        private int storageLimit;
        private boolean wifiOnly;

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
    }

    // 响应DTO
    public static class UpdateSettingsResponse {
        private boolean success;
        private String message;
        private UpdateSettingsData data;

        public UpdateSettingsResponse(boolean success, String message, UpdateSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UpdateSettingsData getData() { return data; }
        public void setData(UpdateSettingsData data) { this.data = data; }
    }

    public static class UpdateSettingsData {
        private boolean offlineMode;
        private boolean autoSync;
        private int syncInterval;
        private int storageLimit;
        private boolean wifiOnly;
        private String updatedAt;

        public UpdateSettingsData(boolean offlineMode, boolean autoSync, int syncInterval,
                                  int storageLimit, boolean wifiOnly, String updatedAt) {
            this.offlineMode = offlineMode;
            this.autoSync = autoSync;
            this.syncInterval = syncInterval;
            this.storageLimit = storageLimit;
            this.wifiOnly = wifiOnly;
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

    @PutMapping("/settings")
    public ResponseEntity<UpdateSettingsResponse> updateOfflineSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateSettingsRequest request) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("requestBody", request);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateSettingsResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateSettingsResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 验证请求数据
            if (request == null) {
                return ResponseEntity.badRequest().body(
                        new UpdateSettingsResponse(false, "请求数据不能为空", null)
                );
            }

            // 验证同步间隔
            if (request.getSyncInterval() < 1 || request.getSyncInterval() > 1440) {
                return ResponseEntity.badRequest().body(
                        new UpdateSettingsResponse(false, "同步间隔必须在1-1440分钟之间", null)
                );
            }

            // 验证存储限制
            if (request.getStorageLimit() < 1 || request.getStorageLimit() > 10240) {
                return ResponseEntity.badRequest().body(
                        new UpdateSettingsResponse(false, "存储限制必须在1-10240MB之间", null)
                );
            }

            // 3. 更新或插入设置
            String[] settingKeys = {"offlineMode", "autoSync", "syncInterval", "storageLimit", "wifiOnly"};
            String[] settingValues = {
                    String.valueOf(request.isOfflineMode()),
                    String.valueOf(request.isAutoSync()),
                    String.valueOf(request.getSyncInterval()),
                    String.valueOf(request.getStorageLimit()),
                    String.valueOf(request.isWifiOnly())
            };

            int totalRowsAffected = 0;

            for (int i = 0; i < settingKeys.length; i++) {
                // 检查是否已有设置
                String checkSql = "SELECT setting_key FROM offline_settings WHERE user_id = ? AND setting_key = ?";
                List<Map<String, Object>> existing = jdbcTemplate.queryForList(checkSql, userId, settingKeys[i]);

                int rowsAffected;
                if (existing.isEmpty()) {
                    // 插入新设置
                    String insertSql = "INSERT INTO offline_settings (user_id, setting_key, setting_value) VALUES (?, ?, ?)";
                    rowsAffected = jdbcTemplate.update(insertSql, userId, settingKeys[i], settingValues[i]);
                } else {
                    // 更新现有设置
                    String updateSql = "UPDATE offline_settings SET setting_value = ?, updated_at = NOW() " +
                            "WHERE user_id = ? AND setting_key = ?";
                    rowsAffected = jdbcTemplate.update(updateSql, settingValues[i], userId, settingKeys[i]);
                }

                totalRowsAffected += rowsAffected;
            }

            printQueryResult("更新行数: " + totalRowsAffected);

            if (totalRowsAffected > 0) {
                // 4. 查询更新后的设置
                String querySql = "SELECT setting_key, setting_value, updated_at FROM offline_settings " +
                        "WHERE user_id = ? AND setting_key IN ('offlineMode', 'autoSync', 'syncInterval', 'storageLimit', 'wifiOnly') " +
                        "ORDER BY updated_at DESC LIMIT 1";

                List<Map<String, Object>> updatedSettings = jdbcTemplate.queryForList(querySql, userId);

                String updatedAt = "刚刚更新";
                if (!updatedSettings.isEmpty()) {
                    updatedAt = updatedSettings.get(0).get("updated_at").toString();
                }

                // 5. 准备响应数据
                UpdateSettingsData data = new UpdateSettingsData(
                        request.isOfflineMode(),
                        request.isAutoSync(),
                        request.getSyncInterval(),
                        request.getStorageLimit(),
                        request.isWifiOnly(),
                        updatedAt
                );

                UpdateSettingsResponse response = new UpdateSettingsResponse(true, "离线设置更新成功", data);
                printResponse(response);

                return ResponseEntity.ok(response);
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateSettingsResponse(false, "更新失败", null)
            );

        } catch (Exception e) {
            System.err.println("更新离线设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateSettingsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}