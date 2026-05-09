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
public class OcrGetTaskStatus {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取OCR任务状态请求 ===");
        System.out.println("请求参数: " + request);
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
    public static class GetTaskStatusResponse {
        private boolean success;
        private String message;
        private TaskStatusData data;

        public GetTaskStatusResponse(boolean success, String message, TaskStatusData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public TaskStatusData getData() { return data; }
        public void setData(TaskStatusData data) { this.data = data; }
    }

    public static class TaskStatusData {
        private String taskId;
        private String status;
        private Integer progress;
        private String currentDocument;
        private Integer currentPage;
        private Integer totalPages;
        private Integer estimatedTimeRemaining;
        private TaskResult result;

        public TaskStatusData(String taskId, String status, Integer progress, String currentDocument,
                              Integer currentPage, Integer totalPages, Integer estimatedTimeRemaining,
                              TaskResult result) {
            this.taskId = taskId;
            this.status = status;
            this.progress = progress;
            this.currentDocument = currentDocument;
            this.currentPage = currentPage;
            this.totalPages = totalPages;
            this.estimatedTimeRemaining = estimatedTimeRemaining;
            this.result = result;
        }

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Integer getProgress() { return progress; }
        public void setProgress(Integer progress) { this.progress = progress; }

        public String getCurrentDocument() { return currentDocument; }
        public void setCurrentDocument(String currentDocument) { this.currentDocument = currentDocument; }

        public Integer getCurrentPage() { return currentPage; }
        public void setCurrentPage(Integer currentPage) { this.currentPage = currentPage; }

        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }

        public Integer getEstimatedTimeRemaining() { return estimatedTimeRemaining; }
        public void setEstimatedTimeRemaining(Integer estimatedTimeRemaining) { this.estimatedTimeRemaining = estimatedTimeRemaining; }

        public TaskResult getResult() { return result; }
        public void setResult(TaskResult result) { this.result = result; }
    }

    public static class TaskResult {
        private Integer processedPages;
        private Integer successfulPages;
        private Integer failedPages;
        private Double averageConfidence;

        public TaskResult(Integer processedPages, Integer successfulPages, Integer failedPages, Double averageConfidence) {
            this.processedPages = processedPages;
            this.successfulPages = successfulPages;
            this.failedPages = failedPages;
            this.averageConfidence = averageConfidence;
        }

        public Integer getProcessedPages() { return processedPages; }
        public void setProcessedPages(Integer processedPages) { this.processedPages = processedPages; }

        public Integer getSuccessfulPages() { return successfulPages; }
        public void setSuccessfulPages(Integer successfulPages) { this.successfulPages = successfulPages; }

        public Integer getFailedPages() { return failedPages; }
        public void setFailedPages(Integer failedPages) { this.failedPages = failedPages; }

        public Double getAverageConfidence() { return averageConfidence; }
        public void setAverageConfidence(Double averageConfidence) { this.averageConfidence = averageConfidence; }
    }

    @GetMapping("/{taskId}/status")
    public ResponseEntity<GetTaskStatusResponse> getTaskStatus(@PathVariable String taskId) {
        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("taskId", taskId);
        printRequest(requestInfo);

        try {
            // 1. 验证请求数据
            if (taskId == null || taskId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new GetTaskStatusResponse(false, "任务ID不能为空", null)
                );
            }

            // 2. 先检查批量任务表
            String checkBatchTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'batch_ocr_tasks'";
            Integer batchTableCount = jdbcTemplate.queryForObject(checkBatchTableSql, Integer.class);

            if (batchTableCount != null && batchTableCount > 0) {
                // 查询批量任务
                String queryBatchSql = "SELECT batch_id, status, progress, total_pages, processed_pages, successful_pages, failed_pages, average_confidence, estimated_time, started_at, completed_at FROM batch_ocr_tasks WHERE batch_id = ?";
                List<Map<String, Object>> batchTasks = jdbcTemplate.queryForList(queryBatchSql, taskId);

                if (!batchTasks.isEmpty()) {
                    Map<String, Object> batchTask = batchTasks.get(0);
                    printQueryResult(batchTask);

                    // 准备批量任务响应数据
                    String status = (String) batchTask.get("status");
                    Integer progress = batchTask.get("progress") != null ? ((Number) batchTask.get("progress")).intValue() : 0;
                    Integer totalPages = batchTask.get("total_pages") != null ? ((Number) batchTask.get("total_pages")).intValue() : 0;
                    Integer processedPages = batchTask.get("processed_pages") != null ? ((Number) batchTask.get("processed_pages")).intValue() : 0;

                    // 计算剩余时间
                    Integer estimatedTimeRemaining = 0;
                    if ("processing".equals(status) && progress > 0) {
                        LocalDateTime startedAt = (LocalDateTime) batchTask.get("started_at");
                        long elapsedSeconds = java.time.Duration.between(startedAt, LocalDateTime.now()).getSeconds();
                        int totalEstimatedSeconds = batchTask.get("estimated_time") != null ? ((Number) batchTask.get("estimated_time")).intValue() : 0;

                        if (progress > 0) {
                            int remainingSeconds = totalEstimatedSeconds - (int) elapsedSeconds;
                            estimatedTimeRemaining = Math.max(0, remainingSeconds);
                        }
                    }

                    // 准备结果数据
                    TaskResult result = null;
                    if ("completed".equals(status) || "failed".equals(status)) {
                        result = new TaskResult(
                                processedPages,
                                batchTask.get("successful_pages") != null ? ((Number) batchTask.get("successful_pages")).intValue() : 0,
                                batchTask.get("failed_pages") != null ? ((Number) batchTask.get("failed_pages")).intValue() : 0,
                                batchTask.get("average_confidence") != null ? ((Number) batchTask.get("average_confidence")).doubleValue() : 0.0
                        );
                    }

                    TaskStatusData responseData = new TaskStatusData(
                            taskId,
                            status,
                            progress,
                            null, // 当前文档
                            null, // 当前页码
                            totalPages,
                            estimatedTimeRemaining,
                            result
                    );

                    GetTaskStatusResponse response = new GetTaskStatusResponse(true, "获取批量OCR任务状态成功", responseData);

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
                        new GetTaskStatusResponse(false, "任务不存在", null)
                );
            }

            // 4. 查询OCR任务
            String queryTaskSql = "SELECT task_id, document_id, page_number, status, progress, estimated_time, started_at, completed_at, result_json FROM ocr_tasks WHERE task_id = ?";
            List<Map<String, Object>> tasks = jdbcTemplate.queryForList(queryTaskSql, taskId);
            printQueryResult(tasks);

            if (tasks.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new GetTaskStatusResponse(false, "任务不存在", null)
                );
            }

            Map<String, Object> task = tasks.get(0);

            // 5. 准备响应数据
            String status = (String) task.get("status");
            Integer progress = task.get("progress") != null ? ((Number) task.get("progress")).intValue() : 0;
            String currentDocument = (String) task.get("document_id");
            Integer currentPage = task.get("page_number") != null ? ((Number) task.get("page_number")).intValue() : null;
            Integer totalPages = 1; // 单页任务

            // 计算剩余时间
            Integer estimatedTimeRemaining = 0;
            if ("processing".equals(status) && progress > 0) {
                LocalDateTime startedAt = (LocalDateTime) task.get("started_at");
                long elapsedSeconds = java.time.Duration.between(startedAt, LocalDateTime.now()).getSeconds();
                int totalEstimatedSeconds = task.get("estimated_time") != null ? ((Number) task.get("estimated_time")).intValue() : 30;

                if (progress > 0) {
                    int remainingSeconds = totalEstimatedSeconds - (int) elapsedSeconds;
                    estimatedTimeRemaining = Math.max(0, remainingSeconds);
                }
            }

            // 准备结果数据
            TaskResult result = null;
            if ("completed".equals(status)) {
                try {
                    String resultJson = (String) task.get("result_json");
                    if (resultJson != null && !resultJson.trim().isEmpty()) {
                        Map<String, Object> resultMap = objectMapper.readValue(resultJson, Map.class);
                        Double confidence = resultMap.get("confidence") != null ? ((Number) resultMap.get("confidence")).doubleValue() : 0.0;

                        result = new TaskResult(1, 1, 0, confidence);
                    }
                } catch (Exception e) {
                    System.err.println("解析任务结果失败: " + e.getMessage());
                }
            } else if ("failed".equals(status)) {
                result = new TaskResult(1, 0, 1, 0.0);
            }

            TaskStatusData responseData = new TaskStatusData(
                    taskId,
                    status,
                    progress,
                    currentDocument,
                    currentPage,
                    totalPages,
                    estimatedTimeRemaining,
                    result
            );

            GetTaskStatusResponse response = new GetTaskStatusResponse(true, "获取OCR任务状态成功", responseData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取OCR任务状态过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetTaskStatusResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}