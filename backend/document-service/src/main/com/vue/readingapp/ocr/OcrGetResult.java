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
public class OcrGetResult {@Autowired
private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取OCR结果请求 ===");
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
    public static class GetOcrResultResponse {
        private boolean success;
        private String message;
        private OcrResultData data;

        public GetOcrResultResponse(boolean success, String message, OcrResultData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public OcrResultData getData() { return data; }
        public void setData(OcrResultData data) { this.data = data; }
    }

    public static class OcrResultData {
        private String id;
        private Integer documentId;
        private Integer page;
        private String text;
        private Double confidence;
        private String words;
        private String lines;
        private String blocks;
        private String metadata;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public OcrResultData(String id, Integer documentId, Integer page, String text,
                             Double confidence, String words, String lines, String blocks,
                             String metadata, LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.documentId = documentId;
            this.page = page;
            this.text = text;
            this.confidence = confidence;
            this.words = words;
            this.lines = lines;
            this.blocks = blocks;
            this.metadata = metadata;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public Integer getDocumentId() { return documentId; }
        public void setDocumentId(Integer documentId) { this.documentId = documentId; }

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }

        public String getWords() { return words; }
        public void setWords(String words) { this.words = words; }

        public String getLines() { return lines; }
        public void setLines(String lines) { this.lines = lines; }

        public String getBlocks() { return blocks; }
        public void setBlocks(String blocks) { this.blocks = blocks; }

        public String getMetadata() { return metadata; }
        public void setMetadata(String metadata) { this.metadata = metadata; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    @GetMapping("/{documentId}/ocr/{page}")
    public ResponseEntity<GetOcrResultResponse> getOcrResult(
            @PathVariable String documentId,
            @PathVariable Integer page) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("page", page);
        printRequest(requestInfo);

        try {
            // 1. 验证请求数据
            if (documentId == null || documentId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new GetOcrResultResponse(false, "文档ID不能为空", null)
                );
            }

            if (page == null || page <= 0) {
                return ResponseEntity.badRequest().body(
                        new GetOcrResultResponse(false, "页码必须大于0", null)
                );
            }

            // 2. 检查文档是否存在
            String checkDocumentSql = "SELECT COUNT(*) FROM documents WHERE document_id = ?";
            Integer documentCount = jdbcTemplate.queryForObject(checkDocumentSql, Integer.class, documentId);

            if (documentCount == null || documentCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new GetOcrResultResponse(false, "文档不存在", null)
                );
            }

            // 3. 查询OCR结果
            // 先检查OCR结果表是否存在
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'document_ocr_results'";
            Integer tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class);

            if (tableCount == null || tableCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new GetOcrResultResponse(false, "该页面没有OCR结果", null)
                );
            }

            // 查询OCR结果
            String queryOcrSql = "SELECT ocr_id, document_id, page_number, ocr_text, confidence, words_json, lines_json, blocks_json, metadata_json, created_at, updated_at FROM document_ocr_results WHERE document_id = ? AND page_number = ?";

            List<Map<String, Object>> ocrResults = jdbcTemplate.queryForList(queryOcrSql, documentId, page);
            printQueryResult(ocrResults);

            if (ocrResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new GetOcrResultResponse(false, "该页面没有OCR结果", null)
                );
            }

            Map<String, Object> ocrResult = ocrResults.get(0);

            // 4. 准备响应数据（保持LocalDateTime类型）
            OcrResultData responseData = new OcrResultData(
                    (String) ocrResult.get("ocr_id"),
                    (Integer) ocrResult.get("document_id"),
                    (Integer) ocrResult.get("page_number"),
                    (String) ocrResult.get("ocr_text"),
                    ocrResult.get("confidence") != null ? ((Number) ocrResult.get("confidence")).doubleValue() : 0.0,
                    (String) ocrResult.get("words_json"),
                    (String) ocrResult.get("lines_json"),
                    (String) ocrResult.get("blocks_json"),
                    (String) ocrResult.get("metadata_json"),
                    (LocalDateTime) ocrResult.get("created_at"),
                    (LocalDateTime) ocrResult.get("updated_at")
            );

            GetOcrResultResponse response = new GetOcrResultResponse(true, "获取OCR结果成功", responseData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取OCR结果过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetOcrResultResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 可选：获取文档所有页的OCR结果
    @GetMapping("/{documentId}/ocr")
    public ResponseEntity<GetAllOcrResultResponse> getAllOcrResults(
            @PathVariable String documentId) {

        try {
            // 1. 验证文档ID
            if (documentId == null || documentId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new GetAllOcrResultResponse(false, "文档ID不能为空", null)
                );
            }

            // 2. 检查文档是否存在
            String checkDocumentSql = "SELECT COUNT(*) FROM documents WHERE document_id = ?";
            Integer documentCount = jdbcTemplate.queryForObject(checkDocumentSql, Integer.class, documentId);

            if (documentCount == null || documentCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new GetAllOcrResultResponse(false, "文档不存在", null)
                );
            }

            // 3. 查询所有OCR结果
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'document_ocr_results'";
            Integer tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class);

            if (tableCount == null || tableCount == 0) {
                return ResponseEntity.ok(
                        new GetAllOcrResultResponse(true, "没有OCR结果", new ArrayList<>())
                );
            }

            String queryAllOcrSql = "SELECT ocr_id, document_id, page_number, ocr_text, confidence, words_json, lines_json, blocks_json, metadata_json, created_at, updated_at FROM document_ocr_results WHERE document_id = ? ORDER BY page_number";

            List<Map<String, Object>> allOcrResults = jdbcTemplate.queryForList(queryAllOcrSql, documentId);
            printQueryResult(allOcrResults);

            // 4. 转换为响应数据
            List<OcrResultData> responseDataList = new ArrayList<>();
            for (Map<String, Object> ocrResult : allOcrResults) {
                OcrResultData data = new OcrResultData(
                        (String) ocrResult.get("ocr_id"),
                        (Integer) ocrResult.get("document_id"),
                        (Integer) ocrResult.get("page_number"),
                        (String) ocrResult.get("ocr_text"),
                        ocrResult.get("confidence") != null ? ((Number) ocrResult.get("confidence")).doubleValue() : 0.0,
                        (String) ocrResult.get("words_json"),
                        (String) ocrResult.get("lines_json"),
                        (String) ocrResult.get("blocks_json"),
                        (String) ocrResult.get("metadata_json"),
                        (LocalDateTime) ocrResult.get("created_at"),
                        (LocalDateTime) ocrResult.get("updated_at")
                );
                responseDataList.add(data);
            }

            GetAllOcrResultResponse response = new GetAllOcrResultResponse(true, "获取所有OCR结果成功", responseDataList);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取所有OCR结果过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetAllOcrResultResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 获取所有OCR结果的响应DTO
    public static class GetAllOcrResultResponse {
        private boolean success;
        private String message;
        private List<OcrResultData> data;

        public GetAllOcrResultResponse(boolean success, String message, List<OcrResultData> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<OcrResultData> getData() { return data; }
        public void setData(List<OcrResultData> data) { this.data = data; }
    }}