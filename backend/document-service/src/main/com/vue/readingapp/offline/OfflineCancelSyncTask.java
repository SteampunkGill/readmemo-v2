
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
public class OfflineCancelSyncTask {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到取消同步任务请求 ===");
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
    public static class CancelSyncTaskResponse {
        private boolean success;
        private String message;

        public CancelSyncTaskResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
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

    @DeleteMapping("/sync/task/{taskId}")
    public ResponseEntity<CancelSyncTaskResponse> cancelSyncTask(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String taskId) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("taskId", taskId);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new CancelSyncTaskResponse(false, "请先登录")
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new CancelSyncTaskResponse(false, "登录已过期，请重新登录")
                );
            }

            // 2. 检查同步任务是否存在且属于该用户
            String checkSql = "SELECT task_id, status FROM offline_sync_tasks WHERE task_id = ? AND user_id = ?";
            List<Map<String, Object>> tasks = jdbcTemplate.queryForList(checkSql, taskId, userId);

            if (tasks.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new CancelSyncTaskResponse(false, "同步任务不存在或无权取消")
                );
            }

            Map<String, Object> task = tasks.get(0);
            String status = (String) task.get("status");

            // 3. 检查是否可以取消（只有running状态可以取消）
            if ("completed".equals(status) || "failed".equals(status) || "cancelled".equals(status)) {
                return ResponseEntity.badRequest().body(
                        new CancelSyncTaskResponse(false, "同步任务已结束，无法取消")
                );
            }

            // 4. 取消同步任务
            String updateSql = "UPDATE offline_sync_tasks SET status = 'cancelled', end_time = NOW(), " +
                    "error = '用户取消' WHERE task_id = ? AND user_id = ?";
            int rowsAffected = jdbcTemplate.update(updateSql, taskId, userId);

            printQueryResult("取消行数: " + rowsAffected);

            if (rowsAffected > 0) {
                // 5. 添加取消记录到历史表
                addSyncHistory(userId, taskId, "cancelled", "用户取消");

                CancelSyncTaskResponse response = new CancelSyncTaskResponse(true, "同步任务已取消");
                printResponse(response);

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new CancelSyncTaskResponse(false, "取消失败")
                );
            }

        } catch (Exception e) {
            System.err.println("取消同步任务过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CancelSyncTaskResponse(false, "服务器内部错误: " + e.getMessage())
            );
        }
    }

    // 添加同步历史记录
    private void addSyncHistory(Integer userId, String taskId, String status, String error) {
        try {
            // 创建 offline_sync_history 表（如果不存在）
            String createTableSql = "CREATE TABLE IF NOT EXISTS offline_sync_history (" +
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
            jdbcTemplate.execute(createTableSql);

            // 查询任务信息
            String querySql = "SELECT task_type, start_time FROM offline_sync_tasks WHERE task_id = ? AND user_id = ?";
            List<Map<String, Object>> taskInfo = jdbcTemplate.queryForList(querySql, taskId, userId);

            if (!taskInfo.isEmpty()) {
                Map<String, Object> row = taskInfo.get(0);
                String taskType = (String) row.get("task_type");
                String startTime = row.get("start_time").toString();

                // 计算持续时间（秒）
                long duration = (System.currentTimeMillis() - java.sql.Timestamp.valueOf(startTime).getTime()) / 1000;

                // 插入历史记录
                String syncHistId = "sync_hist_" + System.currentTimeMillis() + "_" + userId;
                String insertSql = "INSERT INTO offline_sync_history (sync_hist_id, user_id, task_id, task_type, " +
                        "status, synced_items, failed_items, duration, error, start_time, end_time, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, 0, 0, ?, ?, ?, NOW(), NOW())";
                jdbcTemplate.update(insertSql, syncHistId, userId, taskId, taskType, status,
                        duration, error, startTime);
            }

        } catch (Exception e) {
            System.err.println("添加同步历史记录失败: " + e.getMessage());
        }
    }
}