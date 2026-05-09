
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
public class OfflineSetStorageLimit {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到设置离线存储限制请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("==============================");
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
    public static class SetStorageLimitRequest {
        private int limit_mb;

        public int getLimit_mb() { return limit_mb; }
        public void setLimit_mb(int limit_mb) { this.limit_mb = limit_mb; }
    }

    // 响应DTO
    public static class SetStorageLimitResponse {
        private boolean success;
        private String message;
        private SetStorageLimitData data;

        public SetStorageLimitResponse(boolean success, String message, SetStorageLimitData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SetStorageLimitData getData() { return data; }
        public void setData(SetStorageLimitData data) { this.data = data; }
    }

    public static class SetStorageLimitData {
        private int limit_mb;
        private String updatedAt;

        public SetStorageLimitData(int limit_mb, String updatedAt) {
            this.limit_mb = limit_mb;
            this.updatedAt = updatedAt;
        }

        // Getters and Setters
        public int getLimit_mb() { return limit_mb; }
        public void setLimit_mb(int limit_mb) { this.limit_mb = limit_mb; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    // 创建 offline_settings 表（如果不存在）
    private void createOfflineSettingsTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS offline_settings (" +
                "user_id INT NOT NULL, " +
                "setting_key VARCHAR(50) NOT NULL, " +
                "setting_value TEXT, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (user_id, setting_key), " +
                "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                ")";

        try {
            jdbcTemplate.execute(sql);
            System.out.println("offline_settings 表已创建或已存在");
        } catch (Exception e) {
            System.err.println("创建 offline_settings 表失败: " + e.getMessage());
        }
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

    @PutMapping("/storage-limit")
    public ResponseEntity<SetStorageLimitResponse> setOfflineStorageLimit(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SetStorageLimitRequest request) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("requestBody", request);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SetStorageLimitResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SetStorageLimitResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 验证请求数据
            if (request == null) {
                return ResponseEntity.badRequest().body(
                        new SetStorageLimitResponse(false, "请求数据不能为空", null)
                );
            }

            int limitMb = request.getLimit_mb();
            if (limitMb < 1 || limitMb > 10240) {
                return ResponseEntity.badRequest().body(
                        new SetStorageLimitResponse(false, "存储限制必须在1-10240MB之间", null)
                );
            }

            // 3. 创建设置表
            createOfflineSettingsTableIfNotExists();

            // 4. 检查是否已有设置
            String checkSql = "SELECT setting_value FROM offline_settings WHERE user_id = ? AND setting_key = 'storage_limit'";
            List<Map<String, Object>> existingSettings = jdbcTemplate.queryForList(checkSql, userId);

            int rowsAffected;
            if (existingSettings.isEmpty()) {
                // 插入新设置
                String insertSql = "INSERT INTO offline_settings (user_id, setting_key, setting_value) VALUES (?, 'storage_limit', ?)";
                rowsAffected = jdbcTemplate.update(insertSql, userId, String.valueOf(limitMb));
            } else {
                // 更新现有设置
                String updateSql = "UPDATE offline_settings SET setting_value = ?, updated_at = NOW() " +
                        "WHERE user_id = ? AND setting_key = 'storage_limit'";
                rowsAffected = jdbcTemplate.update(updateSql, String.valueOf(limitMb), userId);
            }

            printQueryResult("设置行数: " + rowsAffected);

            if (rowsAffected > 0) {
                // 5. 查询更新后的设置
                String querySql = "SELECT setting_value, updated_at FROM offline_settings " +
                        "WHERE user_id = ? AND setting_key = 'storage_limit'";
                List<Map<String, Object>> settings = jdbcTemplate.queryForList(querySql, userId);

                if (!settings.isEmpty()) {
                    Map<String, Object> row = settings.get(0);

                    // 6. 准备响应数据
                    SetStorageLimitData data = new SetStorageLimitData(
                            limitMb,
                            row.get("updated_at").toString()
                    );

                    SetStorageLimitResponse response = new SetStorageLimitResponse(true,
                            String.format("离线存储限制已设置为 %d MB", limitMb), data);

                    printResponse(response);

                    return ResponseEntity.ok(response);
                }
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SetStorageLimitResponse(false, "设置失败", null)
            );

        } catch (Exception e) {
            System.err.println("设置离线存储限制过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SetStorageLimitResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}