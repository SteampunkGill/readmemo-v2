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
@RequestMapping("/api/v1/documents")
public class OcrUpdateResult {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到更新OCR结果请求 ===");
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
    public static class UpdateOcrResultRequest {
        private String documentId;
        private Integer page;
        private OcrData ocrData;

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }

        public OcrData getOcrData() { return ocrData; }
        public void setOcrData(OcrData ocrData) { this.ocrData = ocrData; }
    }

    public static class OcrData {
        private String text;
        private Double confidence;
        private String words;
        private String lines;
        private String metadata;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }

        public String getWords() { return words; }
        public void setWords(String words) { this.words = words; }

        public String getLines() { return lines; }
        public void setLines(String lines) { this.lines = lines; }

        public String getMetadata() { return metadata; }
        public void setMetadata(String metadata) { this.metadata = metadata; }
    }

    // 响应DTO
    public static class UpdateOcrResultResponse {
        private boolean success;
        private String message;
        private UpdatedOcrData data;

        public UpdateOcrResultResponse(boolean success, String message, UpdatedOcrData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UpdatedOcrData getData() { return data; }
        public void setData(UpdatedOcrData data) { this.data = data; }
    }

    public static class UpdatedOcrData {
        private String id;
        private Integer documentId;
        private Integer page;
        private String text;
        private Double confidence;
        private LocalDateTime updatedAt;

        public UpdatedOcrData(String id, Integer documentId, Integer page, String text, Double confidence, LocalDateTime updatedAt) {
            this.id = id;
            this.documentId = documentId;
            this.page = page;
            this.text = text;
            this.confidence = confidence;
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

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    @PutMapping("/{documentId}/ocr/{page}")
    public ResponseEntity<UpdateOcrResultResponse> updateOcrResult(
            @PathVariable String documentId,
            @PathVariable Integer page,
            @RequestBody UpdateOcrResultRequest request) {

        // 设置文档ID和页码
        request.setDocumentId(documentId);
        request.setPage(page);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证请求数据
            if (documentId == null || documentId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UpdateOcrResultResponse(false, "文档ID不能为空", null)
                );
            }

            if (page == null || page <= 0) {
                return ResponseEntity.badRequest().body(
                        new UpdateOcrResultResponse(false, "页码必须大于0", null)
                );
            }

            if (request.getOcrData() == null) {
                return ResponseEntity.badRequest().body(
                        new UpdateOcrResultResponse(false, "OCR数据不能为空", null)
                );
            }

            if (request.getOcrData().getText() == null || request.getOcrData().getText().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UpdateOcrResultResponse(false, "OCR文本不能为空", null)
                );
            }

            // 2. 检查文档是否存在
            String checkDocumentSql = "SELECT COUNT(*) FROM documents WHERE document_id = ?";
            Integer documentCount = jdbcTemplate.queryForObject(checkDocumentSql, Integer.class, documentId);

            if (documentCount == null || documentCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdateOcrResultResponse(false, "文档不存在", null)
                );
            }

            // 3. 检查OCR结果表是否存在
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'document_ocr_results'";
            Integer tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class);

            if (tableCount == null || tableCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdateOcrResultResponse(false, "OCR结果表不存在", null)
                );
            }

            // 4. 检查OCR结果是否存在
            String checkOcrSql = "SELECT ocr_id FROM document_ocr_results WHERE document_id = ? AND page_number = ?";
            List<Map<String, Object>> existingOcr = jdbcTemplate.queryForList(checkOcrSql, documentId, page);

            LocalDateTime now = LocalDateTime.now();
            String ocrId;

            if (existingOcr.isEmpty()) {
                // 创建新的OCR结果
                ocrId = "ocr_" + System.currentTimeMillis();
                String insertOcrSql = "INSERT INTO document_ocr_results (ocr_id, document_id, page_number, ocr_text, confidence, words_json, lines_json, metadata_json, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                jdbcTemplate.update(insertOcrSql,
                        ocrId,
                        documentId,
                        page,
                        request.getOcrData().getText(),
                        request.getOcrData().getConfidence() != null ? request.getOcrData().getConfidence() : 0.0,
                        request.getOcrData().getWords() != null ? request.getOcrData().getWords() : "[]",
                        request.getOcrData().getLines() != null ? request.getOcrData().getLines() : "[]",
                        request.getOcrData().getMetadata() != null ? request.getOcrData().getMetadata() : "{}",
                        now,
                        now
                );

                System.out.println("创建了新的OCR结果: " + ocrId);
            } else {
                // 更新现有的OCR结果
                ocrId = (String) existingOcr.get(0).get("ocr_id");
                String updateOcrSql = "UPDATE document_ocr_results SET ocr_text = ?, confidence = ?, words_json = ?, lines_json = ?, metadata_json = ?, updated_at = ? WHERE ocr_id = ?";

                jdbcTemplate.update(updateOcrSql,
                        request.getOcrData().getText(),
                        request.getOcrData().getConfidence() != null ? request.getOcrData().getConfidence() : 0.0,
                        request.getOcrData().getWords() != null ? request.getOcrData().getWords() : "[]",
                        request.getOcrData().getLines() != null ? request.getOcrData().getLines() : "[]",
                        request.getOcrData().getMetadata() != null ? request.getOcrData().getMetadata() : "{}",
                        now,
                        ocrId
                );

                System.out.println("更新了OCR结果: " + ocrId);
            }

            // 5. 查询更新后的OCR结果
            String queryOcrSql = "SELECT ocr_id, document_id, page_number, ocr_text, confidence, updated_at FROM document_ocr_results WHERE ocr_id = ?";
            List<Map<String, Object>> updatedOcr = jdbcTemplate.queryForList(queryOcrSql, ocrId);
            printQueryResult(updatedOcr);

            if (updatedOcr.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new UpdateOcrResultResponse(false, "更新OCR结果失败", null)
                );
            }

            Map<String, Object> ocrResult = updatedOcr.get(0);

            // 6. 准备响应数据（保持LocalDateTime类型）
            UpdatedOcrData responseData = new UpdatedOcrData(
                    (String) ocrResult.get("ocr_id"),
                    (Integer) ocrResult.get("document_id"),
                    (Integer) ocrResult.get("page_number"),
                    (String) ocrResult.get("ocr_text"),
                    ocrResult.get("confidence") != null ? ((Number) ocrResult.get("confidence")).doubleValue() : 0.0,
                    (LocalDateTime) ocrResult.get("updated_at")
            );

            UpdateOcrResultResponse response = new UpdateOcrResultResponse(true, "OCR结果更新成功", responseData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新OCR结果过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateOcrResultResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}