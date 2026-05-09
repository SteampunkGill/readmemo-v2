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
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/documents")
public class OcrGetHistory {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取OCR历史请求 ===");
        System.out.println("请求参数: " + request);
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

    // 响应DTO
    public static class GetOcrHistoryResponse {
        private boolean success;
        private String message;
        private List<OcrHistoryItem> data;

        public GetOcrHistoryResponse(boolean success, String message, List<OcrHistoryItem> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<OcrHistoryItem> getData() { return data; }
        public void setData(List<OcrHistoryItem> data) { this.data = data; }
    }

    public static class OcrHistoryItem {
        private String id;
        private String documentId;
        private Integer page;
        private String text;
        private Double confidence;
        private String metadata;
        private String createdAt;
        private String updatedAt;

        public OcrHistoryItem(String id, String documentId, Integer page, String text,
                              Double confidence, String metadata, String createdAt, String updatedAt) {
            this.id = id;
            this.documentId = documentId;
            this.page = page;
            this.text = text;
            this.confidence = confidence;
            this.metadata = metadata;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }

        public String getMetadata() { return metadata; }
        public void setMetadata(String metadata) { this.metadata = metadata; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    @GetMapping("/{documentId}/ocr/histories")
    public ResponseEntity<GetOcrHistoryResponse> getOcrHistory(
            @PathVariable String documentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        // 构建请求信息
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("page", page);
        requestInfo.put("startDate", startDate);
        requestInfo.put("endDate", endDate);

        // 打印接收到的请求
        printRequest(requestInfo);

        try {
            // 1. 验证请求数据
            if (documentId == null || documentId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new GetOcrHistoryResponse(false, "文档ID不能为空", null)
                );
            }

            // 2. 检查文档是否存在
            String checkDocumentSql = "SELECT COUNT(*) FROM documents WHERE document_id = ?";
            Integer documentCount = jdbcTemplate.queryForObject(checkDocumentSql, Integer.class, documentId);

            if (documentCount == null || documentCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new GetOcrHistoryResponse(false, "文档不存在", null)
                );
            }

            // 3. 检查OCR结果表是否存在
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'document_ocr_results'";
            Integer tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class);

            if (tableCount == null || tableCount == 0) {
                return ResponseEntity.ok(
                        new GetOcrHistoryResponse(true, "没有OCR历史记录", new ArrayList<>())
                );
            }

            // 4. 构建查询SQL
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT ocr_id, document_id, page_number, ocr_text, confidence, metadata_json, created_at, updated_at ");
            queryBuilder.append("FROM document_ocr_results WHERE document_id = ?");

            List<Object> params = new ArrayList<>();
            params.add(documentId);

            // 添加页码条件
            if (page != null && page > 0) {
                queryBuilder.append(" AND page_number = ?");
                params.add(page);
            }

            // 添加日期条件
            if (startDate != null && !startDate.trim().isEmpty()) {
                queryBuilder.append(" AND created_at >= ?");
                params.add(LocalDateTime.parse(startDate + "T00:00:00"));
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                queryBuilder.append(" AND created_at <= ?");
                params.add(LocalDateTime.parse(endDate + "T23:59:59"));
            }

            queryBuilder.append(" ORDER BY created_at DESC");

            // 5. 执行查询
            List<Map<String, Object>> historyList = jdbcTemplate.queryForList(queryBuilder.toString(), params.toArray());
            printQueryResult(historyList);

            // 6. 转换为响应数据
            List<OcrHistoryItem> responseData = new ArrayList<>();
            for (Map<String, Object> historyItem : historyList) {
                OcrHistoryItem item = new OcrHistoryItem(
                        (String) historyItem.get("ocr_id"),
                        (String) historyItem.get("document_id"),
                        (Integer) historyItem.get("page_number"),
                        (String) historyItem.get("ocr_text"),
                        historyItem.get("confidence") != null ? ((Number) historyItem.get("confidence")).doubleValue() : 0.0,
                        (String) historyItem.get("metadata_json"),
                        historyItem.get("created_at") != null ? ((LocalDateTime) historyItem.get("created_at")).format(formatter) : "",
                        historyItem.get("updated_at") != null ? ((LocalDateTime) historyItem.get("updated_at")).format(formatter) : ""
                );
                responseData.add(item);
            }

            GetOcrHistoryResponse response = new GetOcrHistoryResponse(true, "获取OCR历史成功", responseData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取OCR历史过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetOcrHistoryResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 可选：获取所有文档的OCR历史
    @GetMapping("/ocr/histories")
    public ResponseEntity<GetAllOcrHistoryResponse> getAllOcrHistory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        try {
            // 1. 检查OCR结果表是否存在
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'document_ocr_results'";
            Integer tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class);

            if (tableCount == null || tableCount == 0) {
                return ResponseEntity.ok(
                        new GetAllOcrHistoryResponse(true, "没有OCR历史记录", new ArrayList<>(), 0, 0, 0)
                );
            }

            // 2. 构建查询SQL
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT ocr_id, document_id, page_number, ocr_text, confidence, metadata_json, created_at, updated_at ");
            queryBuilder.append("FROM document_ocr_results WHERE 1=1");

            List<Object> params = new ArrayList<>();

            // 添加日期条件
            if (startDate != null && !startDate.trim().isEmpty()) {
                queryBuilder.append(" AND created_at >= ?");
                params.add(LocalDateTime.parse(startDate + "T00:00:00"));
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                queryBuilder.append(" AND created_at <= ?");
                params.add(LocalDateTime.parse(endDate + "T23:59:59"));
            }

            queryBuilder.append(" ORDER BY created_at DESC");

            // 3. 执行查询
            List<Map<String, Object>> historyList = jdbcTemplate.queryForList(queryBuilder.toString(), params.toArray());

            // 4. 分页处理
            int totalCount = historyList.size();
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);
            int startIndex = (pageNum - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, totalCount);

            List<Map<String, Object>> pagedList = historyList.subList(startIndex, endIndex);

            // 5. 转换为响应数据
            List<OcrHistoryItem> responseData = new ArrayList<>();
            for (Map<String, Object> historyItem : pagedList) {
                OcrHistoryItem item = new OcrHistoryItem(
                        (String) historyItem.get("ocr_id"),
                        (String) historyItem.get("document_id"),
                        (Integer) historyItem.get("page_number"),
                        (String) historyItem.get("ocr_text"),
                        historyItem.get("confidence") != null ? ((Number) historyItem.get("confidence")).doubleValue() : 0.0,
                        (String) historyItem.get("metadata_json"),
                        historyItem.get("created_at") != null ? ((LocalDateTime) historyItem.get("created_at")).format(formatter) : "",
                        historyItem.get("updated_at") != null ? ((LocalDateTime) historyItem.get("updated_at")).format(formatter) : ""
                );
                responseData.add(item);
            }

            GetAllOcrHistoryResponse response = new GetAllOcrHistoryResponse(
                    true,
                    "获取所有OCR历史成功",
                    responseData,
                    totalCount,
                    totalPages,
                    pageNum
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取所有OCR历史过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetAllOcrHistoryResponse(false, "服务器内部错误: " + e.getMessage(), null, 0, 0, 0)
            );
        }
    }

    // 获取所有OCR历史的响应DTO
    public static class GetAllOcrHistoryResponse {
        private boolean success;
        private String message;
        private List<OcrHistoryItem> data;
        private Integer totalCount;
        private Integer totalPages;
        private Integer currentPage;

        public GetAllOcrHistoryResponse(boolean success, String message, List<OcrHistoryItem> data,
                                        Integer totalCount, Integer totalPages, Integer currentPage) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.totalCount = totalCount;
            this.totalPages = totalPages;
            this.currentPage = currentPage;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<OcrHistoryItem> getData() { return data; }
        public void setData(List<OcrHistoryItem> data) { this.data = data; }

        public Integer getTotalCount() { return totalCount; }
        public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }

        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }

        public Integer getCurrentPage() { return currentPage; }
        public void setCurrentPage(Integer currentPage) { this.currentPage = currentPage; }
    }
}