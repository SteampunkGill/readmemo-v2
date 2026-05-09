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
import java.util.ArrayList;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException; // 添加这行导入
import com.fasterxml.jackson.core.type.TypeReference;

@RestController
@RequestMapping("/api/v1/documents")
public class OcrBatchProcess {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到批量OCR处理请求 ===");
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
    public static class BatchOcrRequest {
        private List<String> documentIds;
        private BatchOcrOptions options;
        private Map<String, List<Integer>> pages;

        public List<String> getDocumentIds() { return documentIds; }
        public void setDocumentIds(List<String> documentIds) { this.documentIds = documentIds; }

        public BatchOcrOptions getOptions() { return options; }
        public void setOptions(BatchOcrOptions options) { this.options = options; }

        public Map<String, List<Integer>> getPages() { return pages; }
        public void setPages(Map<String, List<Integer>> pages) { this.pages = pages; }
    }

    public static class BatchOcrOptions {
        private String language;
        private Integer confidence;
        private Boolean preprocess;
        private String format;
        private Integer pageSegMode;

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public Integer getConfidence() { return confidence; }
        public void setConfidence(Integer confidence) { this.confidence = confidence; }

        public Boolean getPreprocess() { return preprocess; }
        public void setPreprocess(Boolean preprocess) { this.preprocess = preprocess; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public Integer getPageSegMode() { return pageSegMode; }
        public void setPageSegMode(Integer pageSegMode) { this.pageSegMode = pageSegMode; }
    }

    // 响应DTO
    public static class BatchOcrResponse {
        private boolean success;
        private String message;
        private BatchTaskData data;

        public BatchOcrResponse(boolean success, String message, BatchTaskData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public BatchTaskData getData() { return data; }
        public void setData(BatchTaskData data) { this.data = data; }
    }

    public static class BatchTaskData {
        private String batchId;
        private Integer totalDocuments;
        private Integer totalPages;
        private Integer estimatedTime;

        public BatchTaskData(String batchId, Integer totalDocuments, Integer totalPages, Integer estimatedTime) {
            this.batchId = batchId;
            this.totalDocuments = totalDocuments;
            this.totalPages = totalPages;
            this.estimatedTime = estimatedTime;
        }

        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }

        public Integer getTotalDocuments() { return totalDocuments; }
        public void setTotalDocuments(Integer totalDocuments) { this.totalDocuments = totalDocuments; }

        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }

        public Integer getEstimatedTime() { return estimatedTime; }
        public void setEstimatedTime(Integer estimatedTime) { this.estimatedTime = estimatedTime; }
    }

    @PostMapping("/batch-ocr")
    public ResponseEntity<BatchOcrResponse> batchOcrProcess(@RequestBody BatchOcrRequest request) {
        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证请求数据
            if (request.getDocumentIds() == null || request.getDocumentIds().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchOcrResponse(false, "文档ID列表不能为空", null)
                );
            }

            // 2. 检查所有文档是否存在
            List<String> notFoundDocs = new ArrayList<>();
            for (String docId : request.getDocumentIds()) {
                String checkDocumentSql = "SELECT COUNT(*) FROM documents WHERE document_id = ?";
                Integer documentCount = jdbcTemplate.queryForObject(checkDocumentSql, Integer.class, docId);

                if (documentCount == null || documentCount == 0) {
                    notFoundDocs.add(docId);
                }
            }

            if (!notFoundDocs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new BatchOcrResponse(false, "以下文档不存在: " + String.join(", ", notFoundDocs), null)
                );
            }

            // 3. 解析选项
            BatchOcrOptions options = request.getOptions();
            if (options == null) {
                options = new BatchOcrOptions();
            }

            // 设置默认值
            if (options.getLanguage() == null) options.setLanguage("auto");
            if (options.getConfidence() == null) options.setConfidence(70);
            if (options.getPreprocess() == null) options.setPreprocess(true);
            if (options.getFormat() == null) options.setFormat("text");
            if (options.getPageSegMode() == null) options.setPageSegMode(6);

            // 4. 计算总页数
            int totalPages = 0;
            Map<String, List<Integer>> pagesMap = request.getPages();

            if (pagesMap != null && !pagesMap.isEmpty()) {
                for (List<Integer> pageList : pagesMap.values()) {
                    totalPages += pageList.size();
                }
            } else {
                // 如果没有指定页码，假设每个文档处理第一页
                totalPages = request.getDocumentIds().size();
            }

            // 5. 生成批量任务ID
            String batchId = "batch_ocr_" + UUID.randomUUID().toString().substring(0, 8);
            LocalDateTime now = LocalDateTime.now();

            // 6. 检查批量任务表是否存在
            String checkBatchTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'batch_ocr_tasks'";
            Integer batchTableCount = jdbcTemplate.queryForObject(checkBatchTableSql, Integer.class);

            if (batchTableCount == null || batchTableCount == 0) {
                // 创建批量任务表
                String createBatchTableSql = "CREATE TABLE batch_ocr_tasks (" +
                        "batch_id VARCHAR(50) PRIMARY KEY," +
                        "total_documents INT NOT NULL," +
                        "total_pages INT NOT NULL," +
                        "processed_documents INT DEFAULT 0," +
                        "processed_pages INT DEFAULT 0," +
                        "successful_pages INT DEFAULT 0," +
                        "failed_pages INT DEFAULT 0," +
                        "average_confidence DECIMAL(5,2) DEFAULT 0.0," +
                        "status VARCHAR(20) NOT NULL," +
                        "options_json TEXT," +
                        "results_json TEXT," +
                        "estimated_time INT," +
                        "started_at TIMESTAMP," +
                        "completed_at TIMESTAMP," +
                        "created_at TIMESTAMP," +
                        "updated_at TIMESTAMP" +
                        ")";
                jdbcTemplate.execute(createBatchTableSql);
                System.out.println("已创建 batch_ocr_tasks 表");
            }

            // 7. 创建批量任务记录
            String insertBatchSql = "INSERT INTO batch_ocr_tasks (batch_id, total_documents, total_pages, status, options_json, estimated_time, started_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            String optionsJson;
            try {
                optionsJson = objectMapper.writeValueAsString(options);
            } catch (JsonProcessingException e) {
                System.err.println("序列化OCR选项失败: " + e.getMessage());
                optionsJson = "{}";
            }

            int estimatedTime = totalPages * 10; // 假设每页10秒

            jdbcTemplate.update(insertBatchSql,
                    batchId,
                    request.getDocumentIds().size(),
                    totalPages,
                    "processing",
                    optionsJson,
                    estimatedTime,
                    now,
                    now,
                    now
            );

            // 8. 使用final变量传递给lambda表达式
            final List<String> finalDocumentIds = new ArrayList<>(request.getDocumentIds());
            final Map<String, List<Integer>> finalPagesMap = pagesMap != null ? new HashMap<>(pagesMap) : new HashMap<>();
            final BatchOcrOptions finalOptions = options;
            final String finalBatchId = batchId;

            // 9. 启动异步处理任务
            new Thread(() -> {
                try {
                    processBatchDocuments(finalBatchId, finalDocumentIds, finalPagesMap, finalOptions);
                } catch (Exception e) {
                    System.err.println("批量OCR处理线程错误: " + e.getMessage());

                    // 更新任务为失败状态
                    String failBatchSql = "UPDATE batch_ocr_tasks SET status = 'failed', error_message = ?, updated_at = ? WHERE batch_id = ?";
                    jdbcTemplate.update(failBatchSql, e.getMessage(), LocalDateTime.now(), finalBatchId);
                }
            }).start();

            // 10. 准备响应数据
            BatchTaskData responseData = new BatchTaskData(
                    batchId,
                    request.getDocumentIds().size(),
                    totalPages,
                    estimatedTime
            );

            BatchOcrResponse response = new BatchOcrResponse(true, "批量OCR处理任务已启动", responseData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("批量OCR处理过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BatchOcrResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private void processBatchDocuments(String batchId, List<String> documentIds,
                                       Map<String, List<Integer>> pagesMap, BatchOcrOptions options) {
        try {
            LocalDateTime startTime = LocalDateTime.now();
            int processedDocuments = 0;
            int processedPages = 0;
            int successfulPages = 0;
            int failedPages = 0;
            double totalConfidence = 0.0;

            List<Map<String, Object>> results = new ArrayList<>();

            // 处理每个文档
            for (String docId : documentIds) {
                // 获取该文档要处理的页码
                List<Integer> pageList = new ArrayList<>();
                if (pagesMap != null && pagesMap.containsKey(docId)) {
                    pageList = new ArrayList<>(pagesMap.get(docId));
                } else {
                    // 默认处理第一页
                    pageList.add(1);
                }

                // 处理每个页面
                for (Integer page : pageList) {
                    try {
                        System.out.println("处理文档: " + docId + ", 页码: " + page);

                        // 模拟OCR处理
                        Thread.sleep(2000);

                        // 生成模拟的OCR结果
                        Map<String, Object> ocrResult = new HashMap<>();
                        ocrResult.put("text", "文档" + docId + "第" + page + "页的OCR识别文本内容。");
                        ocrResult.put("confidence", 85.0 + Math.random() * 15);

                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("engine", "tesseract");
                        metadata.put("language", options.getLanguage());
                        metadata.put("processingTime", 2000);
                        ocrResult.put("metadata", metadata);

                        // 保存OCR结果
                        String ocrResultId = "ocr_" + UUID.randomUUID().toString().substring(0, 8);
                        String insertOcrResultSql = "INSERT INTO document_ocr_results (ocr_id, document_id, page_number, ocr_text, confidence, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

                        jdbcTemplate.update(insertOcrResultSql,
                                ocrResultId,
                                docId,
                                page,
                                (String) ocrResult.get("text"),
                                (Double) ocrResult.get("confidence"),
                                LocalDateTime.now(),
                                LocalDateTime.now()
                        );

                        // 记录结果
                        Map<String, Object> resultItem = new HashMap<>();
                        resultItem.put("documentId", docId);
                        resultItem.put("page", page);
                        resultItem.put("status", "success");
                        resultItem.put("confidence", ocrResult.get("confidence"));
                        resultItem.put("ocrId", ocrResultId);
                        results.add(resultItem);

                        successfulPages++;
                        totalConfidence += (Double) ocrResult.get("confidence");

                    } catch (Exception e) {
                        System.err.println("处理文档" + docId + "第" + page + "页失败: " + e.getMessage());

                        // 记录失败结果
                        Map<String, Object> resultItem = new HashMap<>();
                        resultItem.put("documentId", docId);
                        resultItem.put("page", page);
                        resultItem.put("status", "failed");
                        resultItem.put("error", e.getMessage());
                        results.add(resultItem);

                        failedPages++;
                    }

                    processedPages++;

                    // 更新批量任务进度
                    int progress = (int) ((processedPages * 100.0) / (documentIds.size() * (pagesMap != null ? pagesMap.get(docId).size() : 1)));
                    String updateProgressSql = "UPDATE batch_ocr_tasks SET processed_pages = ?, progress = ?, updated_at = ? WHERE batch_id = ?";
                    jdbcTemplate.update(updateProgressSql, processedPages, progress, LocalDateTime.now(), batchId);
                }

                processedDocuments++;

                // 更新已处理文档数
                String updateDocsSql = "UPDATE batch_ocr_tasks SET processed_documents = ?, updated_at = ? WHERE batch_id = ?";
                jdbcTemplate.update(updateDocsSql, processedDocuments, LocalDateTime.now(), batchId);
            }

            // 计算平均置信度
            double averageConfidence = successfulPages > 0 ? totalConfidence / successfulPages : 0.0;

            // 更新批量任务为完成状态
            String completeBatchSql = "UPDATE batch_ocr_tasks SET status = 'completed', successful_pages = ?, failed_pages = ?, average_confidence = ?, results_json = ?, completed_at = ?, updated_at = ? WHERE batch_id = ?";

            String resultsJson;
            try {
                resultsJson = objectMapper.writeValueAsString(results);
            } catch (JsonProcessingException e) {
                System.err.println("序列化结果失败: " + e.getMessage());
                resultsJson = "[]";
            }

            LocalDateTime endTime = LocalDateTime.now();

            jdbcTemplate.update(completeBatchSql,
                    successfulPages,
                    failedPages,
                    averageConfidence,
                    resultsJson,
                    endTime,
                    endTime,
                    batchId
            );

            System.out.println("批量OCR处理完成: " + batchId +
                    ", 成功页数: " + successfulPages +
                    ", 失败页数: " + failedPages +
                    ", 平均置信度: " + averageConfidence);

        } catch (Exception e) {
            System.err.println("批量文档处理过程中发生错误: " + e.getMessage());
            throw e;
        }
    }
}