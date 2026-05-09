package com.vue.readingapp.ocr;

// 引入 OcrResult 和 OcrProcessingService
import com.vue.readingapp.ocr.core.OcrResult;
import com.vue.readingapp.ocr.service.OcrProcessingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List; // Not directly used in this snippet, but kept as it was in original imports
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/documents")
public class OcrProcessPage {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // OcrProcessingService 仅在 isImageContent 为 true 时使用
    @Autowired
    private OcrProcessingService ocrProcessingService;

    private ObjectMapper objectMapper = new ObjectMapper();
    // DateTimeFormatter is declared but not used in the final code, can be removed if not needed elsewhere
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // --- 打印方法 (用于调试) ---
    private void printRequest(Object request) {
        System.out.println("=== 收到页面OCR处理请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=======================");
    }

    private void printQueryResult(Object result) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("查询结果: " + result);
        System.out.println("===================");
    }

    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // --- DTO 类 ---
    public static class ProcessPageRequest {
        // These fields are redundant if they are already in the URL path, but kept for consistency with provided code.
        // They will be set by the controller using @PathVariable.
        private String documentId;
        private Integer page;

        private String content; // The actual content to process (base64 image or HTML string)
        private Boolean isImageContent; // Flag to distinguish between image and HTML content
        private PageOcrOptions options; // OCR processing options

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public Boolean getIsImageContent() { return isImageContent; }
        public void setIsImageContent(Boolean isImageContent) { this.isImageContent = isImageContent; }

        public PageOcrOptions getOptions() { return options; }
        public void setOptions(PageOcrOptions options) { this.options = options; }

        @Override
        public String toString() {
            return "ProcessPageRequest{" +
                    "documentId='" + documentId + '\'' +
                    ", page=" + page +
                    ", content='" + (content != null ? content.substring(0, Math.min(content.length(), 50)) + (content.length() > 50 ? "..." : "") : "null") + '\'' + // Truncate content for logging
                    ", isImageContent=" + isImageContent +
                    ", options=" + options +
                    '}';
        }
    }

    public static class PageOcrOptions {
        private String language;
        private Integer confidence; // Confidence threshold (e.g., 75)
        private Boolean preprocess; // Whether to preprocess the image
        private String format;      // Expected output format (e.g., "text", "json")

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public Integer getConfidence() { return confidence; }
        public void setConfidence(Integer confidence) { this.confidence = confidence; }

        public Boolean getPreprocess() { return preprocess; }
        public void setPreprocess(Boolean preprocess) { this.preprocess = preprocess; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        @Override
        public String toString() {
            return "PageOcrOptions{" +
                    "language='" + language + '\'' +
                    ", confidence=" + confidence +
                    ", preprocess=" + preprocess +
                    ", format='" + format + '\'' +
                    '}';
        }
    }

    // Response DTO
    public static class ProcessPageResponse {
        private boolean success;
        private String message;
        private PageOcrResultData data; // Contains task details

        public ProcessPageResponse(boolean success, String message, PageOcrResultData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public PageOcrResultData getData() { return data; }
        public void setData(PageOcrResultData data) { this.data = data; }
    }

    public static class PageOcrResultData {
        private String taskId;
        private String status;      // e.g., "processing", "completed", "failed"
        private Integer progress;   // 0-100
        private Integer estimatedTime; // Estimated time in seconds for the task

        public PageOcrResultData(String taskId, String status, Integer progress, Integer estimatedTime) {
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

    @PostMapping("/{documentId}/pages/{page}/ocr-process")
    public ResponseEntity<ProcessPageResponse> processPage(
            @PathVariable String documentId,
            @PathVariable Integer page,
            @RequestBody ProcessPageRequest request) {

        // Set documentId and page from path variables to the request object.
        // This assumes the request body might not contain these if they are always in the URL.
        request.setDocumentId(documentId);
        request.setPage(page);

        // Print the received request for debugging
        printRequest(request);

        try {
            // 1. Validate input data
            if (documentId == null || documentId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ProcessPageResponse(false, "文档ID不能为空", null)
                );
            }

            if (page == null || page <= 0) {
                return ResponseEntity.badRequest().body(
                        new ProcessPageResponse(false, "页码必须大于0", null)
                );
            }

            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ProcessPageResponse(false, "内容不能为空", null)
                );
            }

            // 2. Check if the document exists in the database
            // Assumes 'documents' table exists with 'document_id' column
            String checkDocumentSql = "SELECT COUNT(*) FROM documents WHERE document_id = ?";
            Integer documentCount = jdbcTemplate.queryForObject(checkDocumentSql, Integer.class, documentId);

            if (documentCount == null || documentCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ProcessPageResponse(false, "文档不存在", null)
                );
            }

            // 3. Parse and set default OCR options if not provided
            PageOcrOptions ocrOptions = request.getOptions();
            if (ocrOptions == null) {
                ocrOptions = new PageOcrOptions(); // Initialize if null
            }
            // Set default values if options are missing
            if (ocrOptions.getLanguage() == null) ocrOptions.setLanguage("zh"); // Default to Chinese
            if (ocrOptions.getConfidence() == null) ocrOptions.setConfidence(75); // Default confidence threshold
            if (ocrOptions.getPreprocess() == null) ocrOptions.setPreprocess(true); // Default to preprocess
            if (ocrOptions.getFormat() == null) ocrOptions.setFormat("text"); // Default output format

            // 4. Generate a unique task ID
            String taskId = "page_ocr_" + UUID.randomUUID().toString().substring(0, 8);
            LocalDateTime now = LocalDateTime.now();

            // 5. Check if the OCR task table exists, create it if not
            // This is a good practice for first-time deployment or testing
            String checkTaskTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'page_ocr_tasks'";
            Integer taskTableCount = jdbcTemplate.queryForObject(checkTaskTableSql, Integer.class);

            if (taskTableCount == null || taskTableCount == 0) {
                String createTaskTableSql = "CREATE TABLE page_ocr_tasks (" +
                        "task_id VARCHAR(50) PRIMARY KEY," +
                        "document_id VARCHAR(50) NOT NULL," +
                        "page_number INT NOT NULL," +
                        "content_type VARCHAR(20)," + // e.g., 'image', 'html'
                        "content_length INT," +        // Length of the content string
                        "status VARCHAR(20) NOT NULL," + // e.g., 'processing', 'completed', 'failed'
                        "progress INT DEFAULT 0," +       // Progress percentage (0-100)
                        "options_json TEXT," +            // JSON string of OCR options
                        "result_json TEXT," +             // JSON string of OCR result
                        "error_message TEXT," +           // Error message if status is 'failed'
                        "estimated_time INT," +           // Estimated processing time in seconds
                        "started_at TIMESTAMP," +
                        "completed_at TIMESTAMP," +
                        "created_at TIMESTAMP," +
                        "updated_at TIMESTAMP," +
                        "FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE" +
                        ")";
                jdbcTemplate.execute(createTaskTableSql);
                System.out.println("INFO: Created table 'page_ocr_tasks'");
            }

            // 6. Insert a new record for the OCR task into the 'page_ocr_tasks' table
            String insertTaskSql = "INSERT INTO page_ocr_tasks (task_id, document_id, page_number, content_type, content_length, status, progress, options_json, estimated_time, started_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            String optionsJson = objectMapper.writeValueAsString(ocrOptions);
            String contentType = (request.getIsImageContent() != null && request.getIsImageContent()) ? "image" : "html";
            int contentLength = request.getContent().length();
            int estimatedTime = 15; // Example estimated time in seconds

            jdbcTemplate.update(insertTaskSql,
                    taskId,
                    documentId,
                    page,
                    contentType,
                    contentLength,
                    "processing", // Initial status
                    0,            // Initial progress
                    optionsJson,
                    estimatedTime,
                    now,          // Started at
                    now,          // Created at
                    now           // Updated at
            );

            // 7. Prepare final variables for use in the lambda expression (thread)
            final String finalDocumentId = documentId;
            final Integer finalPage = page;
            final String finalTaskId = taskId;
            final String finalContent = request.getContent();
            final Boolean finalIsImageContent = request.getIsImageContent();
            final PageOcrOptions finalOptions = ocrOptions; // Use the parsed/defaulted options

            // 8. Start an asynchronous thread to process the page content
            new Thread(() -> {
                try {
                    // Delegate the actual processing to a dedicated method
                    processPageContent(finalTaskId, finalDocumentId, finalPage, finalContent,
                            finalIsImageContent, finalOptions);
                } catch (Exception e) {
                    System.err.println("Page OCR processing thread encountered an error: " + e.getMessage());
                    e.printStackTrace(); // Log the stack trace
                    // Update task status to 'failed' if any exception occurs during processing
                    String failTaskSql = "UPDATE page_ocr_tasks SET status = 'failed', error_message = ?, updated_at = ? WHERE task_id = ?";
                    jdbcTemplate.update(failTaskSql, e.getMessage(), LocalDateTime.now(), finalTaskId);
                }
            }).start(); // Start the new thread

            // 9. Prepare the response data for the client
            PageOcrResultData responseData = new PageOcrResultData(
                    taskId,
                    "processing", // Task has been initiated, status is 'processing'
                    0,            // Initial progress
                    estimatedTime // The estimated time for this task
            );

            ProcessPageResponse response = new ProcessPageResponse(true, "页面OCR处理任务已启动", responseData);

            // Print the response before sending
            printResponse(response);

            // Return a successful response with the task details
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Catch any unexpected exceptions during the initial request handling
            System.err.println("An unexpected error occurred during page OCR processing request: " + e.getMessage());
            e.printStackTrace(); // Log the stack trace
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ProcessPageResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Performs the actual OCR or HTML content processing asynchronously.
     * This method is called by the new thread.
     */
    private void processPageContent(String taskId, String documentId, Integer page,
                                    String content, Boolean isImageContent, PageOcrOptions options) {
        try {
            LocalDateTime startTime = LocalDateTime.now();
            System.out.println("Starting page content processing for TaskID: " + taskId + ", DocumentID: " + documentId + ", Page: " + page);
            System.out.println("Content Type: " + (isImageContent != null && isImageContent ? "Image" : "HTML"));
            System.out.println("Content Length: " + content.length());
            System.out.println("OCR Options: Language=" + options.getLanguage() + ", Confidence=" + options.getConfidence() + ", Preprocess=" + options.getPreprocess());

            // --- Simulate processing progress ---
            // This loop simulates a long-running OCR process by updating progress in steps.
            int totalSteps = 10; // Number of progress update steps
            int sleepIntervalMillis = 750; // Time to sleep between progress updates

            for (int i = 1; i <= totalSteps; i++) {
                Thread.sleep(sleepIntervalMillis);
                int currentProgress = (i * 100) / totalSteps; // Calculate progress percentage

                // Update task progress in the database
                String updateProgressSql = "UPDATE page_ocr_tasks SET progress = ?, updated_at = ? WHERE task_id = ?";
                jdbcTemplate.update(updateProgressSql, currentProgress, LocalDateTime.now(), taskId);

                System.out.println("Page OCR Task Progress Update: " + taskId + " - " + currentProgress + "%");

                // --- Simulate completion when progress reaches 100% ---
                if (currentProgress == 100) {
                    // Generate simulated OCR result
                    Map<String, Object> ocrResultMap = new HashMap<>();
                    Map<String, Object> metadata = new HashMap<>();

                    if (isImageContent != null && isImageContent) {
                        // Process as image content using OcrProcessingService
                        // The actual call to ocrProcessingService.processBase64Image happens here.
                        // For simulation, we create a mock OcrResult.
                        // In a real scenario, this would be:
                        // OcrResult ocrResult = ocrProcessingService.processBase64Image(content, extractOcrServiceOptions(options));

                        // Mock OcrResult for simulation
                        ocrResultMap.put("text", "这是从图片中识别出的文本内容。\nDocument ID: " + documentId + "\nPage: " + page +
                                "\nLanguage: " + options.getLanguage() + "\nConfidence Threshold: " + options.getConfidence());
                        ocrResultMap.put("confidence", 88.5f); // Example confidence value
                        ocrResultMap.put("words", List.of(Map.of("text", "图片", "boundingBox", "1,1,10,10"))); // Example words
                        ocrResultMap.put("lines", List.of(Map.of("text", "这是从图片中识别出的文本内容。", "boundingBox", "1,1,100,10"))); // Example lines

                        metadata.put("engine", "simulated_tesseract"); // Simulated engine name
                        metadata.put("language", options.getLanguage());
                        metadata.put("processingTime", 7500); // Simulated processing time in ms
                        metadata.put("contentType", "image");

                        // Note: OcrResult from service would typically have these fields.
                        // Here we populate them directly for the simulation.

                    } else {
                        // Process as HTML content
                        ocrResultMap.put("text", "这是从HTML内容中提取的文本。\nDocument ID: " + documentId + "\nPage: " + page +
                                "\nOriginal Content Length: " + content.length() + " characters");
                        ocrResultMap.put("confidence", 95.2f); // High confidence for direct text
                        ocrResultMap.put("words", List.of()); // No words if it's HTML text directly
                        ocrResultMap.put("lines", List.of()); // No lines if it's HTML text directly

                        metadata.put("engine", "html_parser"); // Simple parser as engine
                        metadata.put("language", options.getLanguage()); // Keep requested language
                        metadata.put("processingTime", 0); // Instantaneous for HTML
                        metadata.put("contentType", "html");
                    }
                    ocrResultMap.put("metadata", metadata);

                    // Convert the simulated OcrResult data to JSON string
                    String resultJson = objectMapper.writeValueAsString(ocrResultMap);

                    // Update the task to 'completed' status and save the result JSON
                    String completeTaskSql = "UPDATE page_ocr_tasks SET status = 'completed', progress = 100, result_json = ?, completed_at = ?, updated_at = ? WHERE task_id = ?";
                    jdbcTemplate.update(completeTaskSql,
                            resultJson,
                            LocalDateTime.now(), // Completed at
                            LocalDateTime.now(), // Updated at
                            taskId
                    );

                    System.out.println("Page OCR task completed successfully: " + taskId);

                    // --- Save OCR result to the 'document_ocr_results' table ---
                    // This part assumes 'document_ocr_results' table exists and has the specified columns.
                    String ocrResultId = "page_ocr_" + UUID.randomUUID().toString().substring(0, 8); // Unique ID for this specific OCR result
                    String insertOcrResultSql = "INSERT INTO document_ocr_results (ocr_id, document_id, page_number, ocr_text, confidence, metadata_json, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                    // Extract data for insertion into document_ocr_results
                    String extractedText = (String) ocrResultMap.get("text");
                    Double extractedConfidence = (Double) ocrResultMap.get("confidence");
                    String extractedMetadataJson = objectMapper.writeValueAsString(metadata); // Metadata already prepared

                    jdbcTemplate.update(insertOcrResultSql,
                            ocrResultId,
                            documentId,
                            page,
                            extractedText,
                            extractedConfidence,
                            extractedMetadataJson,
                            LocalDateTime.now(), // Created at
                            LocalDateTime.now()  // Updated at
                    );
                    System.out.println("Successfully saved OCR result to 'document_ocr_results' table for TaskID: " + taskId + ", OCR ID: " + ocrResultId);
                }
            }
        } catch (InterruptedException e) {
            // Handle thread interruption specifically
            Thread.currentThread().interrupt(); // Restore interrupted status
            System.err.println("Page content processing thread was interrupted: " + taskId + " - " + e.getMessage());
            // Update task status to 'failed' due to interruption
            String failTaskSql = "UPDATE page_ocr_tasks SET status = 'failed', error_message = ?, updated_at = ? WHERE task_id = ?";
            jdbcTemplate.update(failTaskSql, "Thread interrupted: " + e.getMessage(), LocalDateTime.now(), taskId);
        } catch (Exception e) {
            // Catch any other exceptions during the processing
            System.err.println("Error during page content processing for TaskID: " + taskId + " - " + e.getMessage());
            e.printStackTrace(); // Log the stack trace for detailed debugging
            // Update task status to 'failed' with the error message
            String failTaskSql = "UPDATE page_ocr_tasks SET status = 'failed', error_message = ?, updated_at = ? WHERE task_id = ?";
            jdbcTemplate.update(failTaskSql, e.getMessage(), LocalDateTime.now(), taskId);
        }
    }

    // Helper method to extract options for OcrProcessingService
    // This is necessary because PageOcrOptions might have fields not directly supported by OcrProcessingService
    // Or OcrProcessingService might expect a different structure for options.
    // Adjust this based on the actual signature and expected options of ocrProcessingService.processBase64Image
    private Map<String, Object> extractOcrServiceOptions(PageOcrOptions pageOptions) {
        Map<String, Object> serviceOptions = new HashMap<>();
        if (pageOptions.getLanguage() != null) {
            serviceOptions.put("language", pageOptions.getLanguage());
        }
        if (pageOptions.getConfidence() != null) {
            serviceOptions.put("confidence", pageOptions.getConfidence());
        }
        if (pageOptions.getPreprocess() != null) {
            serviceOptions.put("preprocess", pageOptions.getPreprocess());
        }
        // Add any other relevant options if needed by the service
        return serviceOptions;
    }
}