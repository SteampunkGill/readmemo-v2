package com.vue.readingapp.ocr;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/documents/ocr/tasks")
public class OcrCancelTask {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到取消OCR任务请求 ===");
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
    public static class CancelTaskRequest {
        private String taskId;
        private String reason;

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    // 响应DTO
    public static class CancelTaskResponse {
        private boolean success;
        private String message;
        private CancelTaskData data;

        public CancelTaskResponse(boolean success, String message, CancelTaskData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public CancelTaskData getData() { return data; }
        public void setData(CancelTaskData data) { this.data = data; }
    }

    public static class CancelTaskData {
        private String taskId;
        private String status;
        private String cancelledAt;
        private String reason;

        public CancelTaskData(String taskId, String status, String cancelledAt, String reason) {
            this.taskId = taskId;
            this.status = status;
            this.cancelledAt = cancelledAt;
            this.reason = reason;
        }

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getCancelledAt() { return cancelledAt; }
        public void setCancelledAt(String cancelledAt) { this.cancelledAt = cancelledAt; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<CancelTaskResponse> cancelTask(
            @PathVariable String taskId,
            @RequestBody CancelTaskRequest request) {

        // 设置任务ID
        request.setTaskId(taskId);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证请求数据
            if (taskId == null || taskId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new CancelTaskResponse(false, "任务ID不能为空", null)
                );
            }

            // 2. 先检查批量任务表
            String checkBatchTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'batch_ocr_tasks'";
            Integer batchTableCount = jdbcTemplate.queryForObject(checkBatchTableSql, Integer.class);

            if (batchTableCount != null && batchTableCount > 0) {
                // 查询批量任务
                String queryBatchSql = "SELECT batch_id, status FROM batch_ocr_tasks WHERE batch_id = ?";
                List<Map<String, Object>> batchTasks = jdbcTemplate.queryForList(queryBatchSql, taskId);

                if (!batchTasks.isEmpty()) {
                    Map<String, Object> batchTask = batchTasks.get(0);
                    String currentStatus = (String) batchTask.get("status");

                    // 检查任务是否可以取消
                    if ("completed".equals(currentStatus) || "failed".equals(currentStatus) || "cancelled".equals(currentStatus)) {
                        return ResponseEntity.badRequest().body(
                                new CancelTaskResponse(false, "任务已处于" + currentStatus + "状态，无法取消", null)
                        );
                    }

                    // 取消批量任务
                    LocalDateTime now = LocalDateTime.now();
                    String cancelBatchSql = "UPDATE batch_ocr_tasks SET status = 'cancelled', updated_at = ?, cancelled_at = ?, cancel_reason = ? WHERE batch_id = ?";

                    jdbcTemplate.update(cancelBatchSql,
                            now,
                            now,
                            request.getReason() != null ? request.getReason() : "用户取消",
                            taskId
                    );

                    System.out.println("批量OCR任务已取消: " + taskId);

                    // 准备响应数据
                    CancelTaskData responseData = new CancelTaskData(
                            taskId,
                            "cancelled",
                            now.format(formatter),
                            request.getReason()
                    );

                    CancelTaskResponse response = new CancelTaskResponse(true, "批量OCR任务取消成功", responseData);

                    // 打印返回数据
                    printResponse(response);

                    return ResponseEntity.ok(response);
                }
            }

            // 3. 检查普通OCR任务表
            String checkTaskTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'ocr_tasks'";
            Integer taskTableCount = jdbcTemplate.queryForObject(checkTaskTableSql, Integer.class);

            if (taskTableCount == null || taskTableCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new CancelTaskResponse(false, "任务不存在", null)
                );
            }

            // 4. 查询OCR任务
            String queryTaskSql = "SELECT task_id, status FROM ocr_tasks WHERE task_id = ?";
            List<Map<String, Object>> tasks = jdbcTemplate.queryForList(queryTaskSql, taskId);
            printQueryResult(tasks);

            if (tasks.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new CancelTaskResponse(false, "任务不存在", null)
                );
            }

            Map<String, Object> task = tasks.get(0);
            String currentStatus = (String) task.get("status");

            // 5. 检查任务是否可以取消
            if ("completed".equals(currentStatus) || "failed".equals(currentStatus) || "cancelled".equals(currentStatus)) {
                return ResponseEntity.badRequest().body(
                        new CancelTaskResponse(false, "任务已处于" + currentStatus + "状态，无法取消", null)
                );
            }

            // 6. 取消OCR任务
            LocalDateTime now = LocalDateTime.now();
            String cancelTaskSql = "UPDATE ocr_tasks SET status = 'cancelled', updated_at = ?, cancelled_at = ?, cancel_reason = ? WHERE task_id = ?";

            jdbcTemplate.update(cancelTaskSql,
                    now,
                    now,
                    request.getReason() != null ? request.getReason() : "用户取消",
                    taskId
            );

            System.out.println("OCR任务已取消: " + taskId);

            // 7. 准备响应数据
            CancelTaskData responseData = new CancelTaskData(
                    taskId,
                    "cancelled",
                    now.format(formatter),
                    request.getReason()
            );

            CancelTaskResponse response = new CancelTaskResponse(true, "OCR任务取消成功", responseData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("取消OCR任务过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CancelTaskResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}