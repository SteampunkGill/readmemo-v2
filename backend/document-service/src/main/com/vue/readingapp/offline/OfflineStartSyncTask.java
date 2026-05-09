
package com.vue.readingapp.offline;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/offline")
public class OfflineStartSyncTask {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到开始同步任务请求 ===");
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
    public static class StartSyncTaskRequest {
        private String taskType;

        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }
    }

    // 响应DTO
    public static class StartSyncTaskResponse {
        private boolean success;
        private String message;
        private StartSyncTaskData data;

        public StartSyncTaskResponse(boolean success, String message, StartSyncTaskData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public StartSyncTaskData getData() { return data; }
        public void setData(StartSyncTaskData data) { this.data = data; }
    }

    public static class StartSyncTaskData {
        private String taskId;
        private String taskType;
        private String status;
        private String startTime;

        public StartSyncTaskData(String taskId, String taskType, String status, String startTime) {
            this.taskId = taskId;
            this.taskType = taskType;
            this.status = status;
            this.startTime = startTime;
        }

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }

        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
    }

    // 创建 offline_sync_tasks 表（如果不存在）
    private void createOfflineSyncTasksTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS offline_sync_tasks (" +
                "task_id VARCHAR(50) PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "task_type VARCHAR(50) NOT NULL, " +
                "status VARCHAR(20) DEFAULT 'running', " +
                "progress INT DEFAULT 0, " +
                "total_operations INT DEFAULT 0, " +
                "completed_operations INT DEFAULT 0, " +
                "failed_operations INT DEFAULT 0, " +
                "estimated_end_time TIMESTAMP, " +
                "start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "end_time TIMESTAMP, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                ")";

        try {
            jdbcTemplate.execute(sql);
            System.out.println("offline_sync_tasks 表已创建或已存在");
        } catch (Exception e) {
            System.err.println("创建 offline_sync_tasks 表失败: " + e.getMessage());
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

    @PostMapping("/sync/task")
    public ResponseEntity<StartSyncTaskResponse> startSyncTask(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody StartSyncTaskRequest request) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("requestBody", request);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new StartSyncTaskResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new StartSyncTaskResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 验证请求数据
            if (request == null || request.getTaskType() == null || request.getTaskType().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new StartSyncTaskResponse(false, "任务类型不能为空", null)
                );
            }

            String taskType = request.getTaskType();

            // 3. 创建相关表
            createOfflineSyncTasksTableIfNotExists();

            // 4. 检查是否有正在进行的同步任务
            String checkRunningSql = "SELECT task_id FROM offline_sync_tasks WHERE user_id = ? AND status = 'running'";
            List<Map<String, Object>> runningTasks = jdbcTemplate.queryForList(checkRunningSql, userId);

            if (!runningTasks.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new StartSyncTaskResponse(false, "已有正在进行的同步任务", null)
                );
            }

            // 5. 创建同步任务
            String taskId = "sync_task_" + UUID.randomUUID().toString();
            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime estimatedEndTime = startTime.plusMinutes(5); // 假设5分钟完成

            String insertSql = "INSERT INTO offline_sync_tasks (task_id, user_id, task_type, status, " +
                    "start_time, estimated_end_time, created_at) VALUES (?, ?, ?, 'running', ?, ?, NOW())";
            int rowsAffected = jdbcTemplate.update(insertSql, taskId, userId, taskType, startTime, estimatedEndTime);

            printQueryResult("创建任务行数: " + rowsAffected);

            if (rowsAffected > 0) {
                // 6. 启动异步同步任务（简化处理）
                new Thread(() -> {
                    try {
                        // 模拟同步过程
                        Thread.sleep(2000);

                        // 更新任务进度
                        String updateProgressSql = "UPDATE offline_sync_tasks SET progress = 50, " +
                                "completed_operations = 5, total_operations = 10 " +
                                "WHERE task_id = ? AND user_id = ?";
                        jdbcTemplate.update(updateProgressSql, taskId, userId);

                        Thread.sleep(2000);

                        // 完成任务
                        String completeSql = "UPDATE offline_sync_tasks SET status = 'completed', progress = 100, " +
                                "completed_operations = 10, failed_operations = 0, " +
                                "end_time = NOW() WHERE task_id = ? AND user_id = ?";
                        jdbcTemplate.update(completeSql, taskId, userId);

                        // 将同步记录添加到历史表
                        addSyncHistory(userId, taskId, taskType, "completed", 10, 0, 4.0);

                    } catch (InterruptedException e) {
                        // 任务失败
                        String failSql = "UPDATE offline_sync_tasks SET status = 'failed', end_time = NOW(), " +
                                "error = '同步任务被中断' WHERE task_id = ? AND user_id = ?";
                        jdbcTemplate.update(failSql, taskId, userId);

                        addSyncHistory(userId, taskId, taskType, "failed", 0, 10, 4.0);
                    }
                }).start();

                // 7. 准备响应数据
                StartSyncTaskData data = new StartSyncTaskData(
                        taskId,
                        taskType,
                        "running",
                        startTime.toString()
                );

                StartSyncTaskResponse response = new StartSyncTaskResponse(true, "同步任务已开始", data);
                printResponse(response);

                return ResponseEntity.ok(response);
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new StartSyncTaskResponse(false, "创建同步任务失败", null)
            );

        } catch (Exception e) {
            System.err.println("开始同步任务过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new StartSyncTaskResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 添加同步历史记录
    private void addSyncHistory(Integer userId, String taskId, String taskType,
                                String status, int syncedItems, int failedItems, double duration) {
        try {
            // 创建 offline_sync_history 表（如果不存在）
            String createTableSql = "CREATE TABLE IF NOT EXISTS offline_sync_history (" +
                    "sync_hist_id VARCHAR(50) PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "task_id VARCHAR(50) NOT NULL, " +
                    "task_type VARCHAR(50) NOT NULL, " +
                    "status VARCHAR(20) NOT NULL, " +
                    "synced_items INT DEFAULT 0, " +
                    "failed_items INT DEFAULT 0, " +
                    "duration DOUBLE DEFAULT 0, " +
                    "start_time TIMESTAMP, " +
                    "end_time TIMESTAMP, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                    ")";
            jdbcTemplate.execute(createTableSql);

            // 插入历史记录
            String syncHistId = "sync_hist_" + UUID.randomUUID().toString();
            String insertSql = "INSERT INTO offline_sync_history (sync_hist_id, user_id, task_id, task_type, " +
                    "status, synced_items, failed_items, duration, start_time, end_time, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW() - INTERVAL ? SECOND, NOW(), NOW())";
            jdbcTemplate.update(insertSql, syncHistId, userId, taskId, taskType, status,
                    syncedItems, failedItems, duration, (int)duration);

        } catch (Exception e) {
            System.err.println("添加同步历史记录失败: " + e.getMessage());
        }
    }
}