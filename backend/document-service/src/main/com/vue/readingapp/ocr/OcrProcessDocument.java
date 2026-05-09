package com.vue.readingapp.ocr;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/documents")
public class OcrProcessDocument {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OcrService ocrService;

    private ObjectMapper objectMapper = new ObjectMapper();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // --- 打印方法 (用于调试) ---
    private void printRequest(Object request) {
        System.out.println("=== 收到文档OCR处理请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=======================");
    }

    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // --- DTO 类 ---
    public static class ProcessDocumentRequest {
        private Integer documentId;
        private Integer page;
        private String options;

        public Integer getDocumentId() { return documentId; }
        public void setDocumentId(Integer documentId) { this.documentId = documentId; }

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }

        public String getOptions() { return options; }
        public void setOptions(String options) { this.options = options; }

        @Override
        public String toString() {
            return "ProcessDocumentRequest{" +
                    "documentId='" + documentId + '\'' +
                    ", page=" + page +
                    ", options='" + options + '\'' +
                    '}';
        }
    }

    public static class ProcessDocumentResponse {
        private boolean success;
        private String message;
        private TaskData data;

        public ProcessDocumentResponse(boolean success, String message, TaskData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public TaskData getData() { return data; }
        public void setData(TaskData data) { this.data = data; }
    }

    public static class TaskData {
        private String taskId;
        private String status;
        private Integer progress;
        private Integer estimatedTime;

        public TaskData(String taskId, String status, Integer progress, Integer estimatedTime) {
            this.taskId = taskId;
            this.status = status;
            this.progress = progress;
            this.estimatedTime = estimatedTime;
        }

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Integer getProgress() { return progress; }
        public void setProgress(Integer progress) { this.progress = progress; }

        public Integer getEstimatedTime() { return estimatedTime; }
        public void setEstimatedTime(Integer estimatedTime) { this.estimatedTime = estimatedTime; }
    }

    @PostMapping("/{documentId}/pages/{page}/ocr")
    public ResponseEntity<ProcessDocumentResponse> processDocumentPage(
            @PathVariable String documentId,
            @PathVariable Integer page,
            @RequestBody ProcessDocumentRequest request) {

        // 打印接收到的请求
        System.out.println("DEBUG: 收到请求 documentId=" + documentId + " (类型: " + (documentId != null ? documentId.getClass().getName() : "null") + "), page=" + page);
        printRequest(request);

        try {
            // 1. 验证请求数据
            if (documentId == null || documentId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ProcessDocumentResponse(false, "文档ID不能为空", null)
                );
            }

            if (page == null || page <= 0) {
                return ResponseEntity.badRequest().body(
                        new ProcessDocumentResponse(false, "页码必须大于0", null)
                );
            }

            // 2. 检查文档是否存在
            String checkDocumentSql = "SELECT COUNT(*) FROM documents WHERE document_id = ?";
            Integer documentCount = jdbcTemplate.queryForObject(checkDocumentSql, Integer.class, documentId);

            if (documentCount == null || documentCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ProcessDocumentResponse(false, "文档不存在", null)
                );
            }

            // 3. 检查文档是否已经处理过
            String checkProcessedSql = "SELECT COUNT(*) FROM document_ocr_results WHERE document_id = ? AND page_number = ?";
            Integer processedCount = jdbcTemplate.queryForObject(checkProcessedSql, Integer.class, documentId, page);

            if (processedCount != null && processedCount > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        new ProcessDocumentResponse(false, "该文档页面已经处理过", null)
                );
            }

            // 4. 检查是否有正在处理的任务
            String checkProcessingSql = "SELECT COUNT(*) FROM ocr_tasks WHERE document_id = ? AND page_number = ? AND status IN ('pending', 'processing')";
            Integer processingCount = jdbcTemplate.queryForObject(checkProcessingSql, Integer.class, documentId, page);

            if (processingCount != null && processingCount > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        new ProcessDocumentResponse(false, "该文档页面正在处理中，请稍后", null)
                );
            }

            // 5. 检查队列中是否有该文档的待处理任务
            // 首先确保队列表存在
            ensureProcessingQueueTableExists();

            String checkQueueSql = "SELECT COUNT(*) FROM document_processing_queue WHERE document_id = ? AND status IN ('pending', 'processing')";
            Integer queueCount = jdbcTemplate.queryForObject(checkQueueSql, Integer.class, documentId);

            // 6. 如果队列中没有该文档的待处理任务，则加入队列
            if (queueCount == null || queueCount == 0) {
                String insertQueueSql = "INSERT INTO document_processing_queue (document_id, status, priority, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
                LocalDateTime now = LocalDateTime.now();

                jdbcTemplate.update(insertQueueSql,
                        documentId,
                        "pending",  // 初始状态
                        1,          // 默认优先级
                        now,        // created_at
                        now         // updated_at
                );

                System.out.println("INFO: 文档已添加到处理队列: " + documentId);
            } else {
                System.out.println("INFO: 文档已在处理队列中，跳过添加: " + documentId);
            }

            // 7. 生成任务ID
            String taskId = "ocr_task_" + UUID.randomUUID().toString().substring(0, 8);
            LocalDateTime now = LocalDateTime.now();

            // 8. 确保 ocr_tasks 表存在
            ensureOcrTasksTableExists();

            // 9. 插入 OCR 任务记录到 ocr_tasks 表
            String insertTaskSql = "INSERT INTO ocr_tasks (task_id, document_id, page_number, status, progress, options_json, estimated_time, started_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(insertTaskSql,
                    taskId,
                    documentId,
                    page,
                    "pending", // 初始状态为 pending，等待调度器处理
                    0,         // 初始进度
                    request.getOptions() != null ? request.getOptions() : "{}", // 存储选项
                    30,        // 预估时间
                    null,      // started_at 为空，等待调度器开始处理时设置
                    now,       // Created at
                    now        // Updated at
            );

            System.out.println("INFO: OCR 任务已创建: " + taskId + " (文档ID: " + documentId + ", 页码: " + page + ")");

            // 10. 准备响应数据
            TaskData responseData = new TaskData(
                    taskId,
                    "pending", // 任务已创建，等待处理
                    0,         // 初始进度
                    30         // 预估时间
            );

            ProcessDocumentResponse response = new ProcessDocumentResponse(
                    true,
                    "OCR 处理任务已创建并添加到队列，请稍后查询状态",
                    responseData
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("文档OCR处理请求处理时发生内部错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ProcessDocumentResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    @GetMapping("/{documentId}/pages/{page}/ocr/status")
    public ResponseEntity<ProcessDocumentResponse> getOcrStatus(
            @PathVariable String documentId,
            @PathVariable Integer page) {

        try {
            // 1. 查询任务状态
            String sql = "SELECT task_id, status, progress, estimated_time FROM ocr_tasks WHERE document_id = ? AND page_number = ? ORDER BY created_at DESC LIMIT 1";

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, documentId, page);

            if (results.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ProcessDocumentResponse(false, "未找到OCR任务", null)
                );
            }

            Map<String, Object> task = results.get(0);
            String taskId = (String) task.get("task_id");
            String status = (String) task.get("status");
            Integer progress = (Integer) task.get("progress");
            Integer estimatedTime = (Integer) task.get("estimated_time");

            // 2. 准备响应数据
            TaskData responseData = new TaskData(taskId, status, progress, estimatedTime);

            String message = "OCR任务状态查询成功";
            if ("completed".equals(status)) {
                message = "OCR处理已完成";
            } else if ("processing".equals(status)) {
                message = "OCR处理中，当前进度: " + progress + "%";
            } else if ("failed".equals(status)) {
                message = "OCR处理失败";
            }

            ProcessDocumentResponse response = new ProcessDocumentResponse(true, message, responseData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("查询OCR状态时发生错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ProcessDocumentResponse(false, "查询OCR状态时发生错误: " + e.getMessage(), null)
            );
        }
    }

    @GetMapping("/{documentId}/pages/{page}/ocr/result")
    public ResponseEntity<Map<String, Object>> getOcrResult(
            @PathVariable String documentId,
            @PathVariable Integer page) {

        try {
            // 1. 检查OCR任务是否完成
            String checkTaskSql = "SELECT status, result_json FROM ocr_tasks WHERE document_id = ? AND page_number = ? AND status = 'completed' ORDER BY completed_at DESC LIMIT 1";

            List<Map<String, Object>> taskResults = jdbcTemplate.queryForList(checkTaskSql, documentId, page);

            if (taskResults.isEmpty()) {
                // 检查是否在 document_ocr_results 表中
                String checkResultSql = "SELECT ocr_text, confidence FROM document_ocr_results WHERE document_id = ? AND page_number = ? LIMIT 1";
                List<Map<String, Object>> resultRows = jdbcTemplate.queryForList(checkResultSql, documentId, page);

                if (resultRows.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                            Map.of("success", false, "message", "未找到OCR结果")
                    );
                }

                Map<String, Object> result = resultRows.get(0);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "OCR结果查询成功",
                        "data", Map.of(
                                "text", result.get("ocr_text"),
                                "confidence", result.get("confidence")
                        )
                ));
            }

            // 2. 解析OCR任务结果
            Map<String, Object> task = taskResults.get(0);
            String resultJson = (String) task.get("result_json");

            if (resultJson == null || resultJson.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        Map.of("success", false, "message", "OCR结果为空")
                );
            }

            // 3. 解析JSON结果
            Map<String, Object> ocrResult = objectMapper.readValue(resultJson, Map.class);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "OCR结果查询成功",
                    "data", ocrResult
            ));

        } catch (Exception e) {
            System.err.println("获取OCR结果时发生错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "message", "获取OCR结果时发生错误: " + e.getMessage())
            );
        }
    }

    private void ensureOcrTasksTableExists() {
        try {
            String checkTaskTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'ocr_tasks'";
            Integer taskTableCount = jdbcTemplate.queryForObject(checkTaskTableSql, Integer.class);

            if (taskTableCount == null || taskTableCount == 0) {
                String createTaskTableSql = "CREATE TABLE ocr_tasks (" +
                        "task_id VARCHAR(50) PRIMARY KEY," +
                        "document_id VARCHAR(50) NOT NULL," +
                        "page_number INT," +
                        "status VARCHAR(20) NOT NULL," +
                        "progress INT DEFAULT 0," +
                        "options_json TEXT," +
                        "result_json TEXT," +
                        "error_message TEXT," +
                        "estimated_time INT," +
                        "started_at TIMESTAMP," +
                        "completed_at TIMESTAMP," +
                        "created_at TIMESTAMP," +
                        "updated_at TIMESTAMP" +
                        ")";
                jdbcTemplate.execute(createTaskTableSql);
                System.out.println("INFO: 已创建 ocr_tasks 表");
            }
        } catch (Exception e) {
            System.err.println("检查/创建 ocr_tasks 表时发生错误: " + e.getMessage());
        }
    }

    private void ensureProcessingQueueTableExists() {
        try {
            String checkQueueTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'document_processing_queue'";
            Integer queueTableCount = jdbcTemplate.queryForObject(checkQueueTableSql, Integer.class);

            if (queueTableCount == null || queueTableCount == 0) {
                String createQueueTableSql = "CREATE TABLE document_processing_queue (" +
                        "queue_id INT AUTO_INCREMENT PRIMARY KEY," +
                        "document_id VARCHAR(50) NOT NULL," +
                        "status VARCHAR(20) NOT NULL DEFAULT 'pending'," +
                        "priority INT DEFAULT 1," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "INDEX idx_document_id (document_id)," +
                        "INDEX idx_status (status)" +
                        ")";
                jdbcTemplate.execute(createQueueTableSql);
                System.out.println("INFO: 已创建 document_processing_queue 表");
            }
        } catch (Exception e) {
            System.err.println("检查/创建 document_processing_queue 表时发生错误: " + e.getMessage());
        }
    }
}