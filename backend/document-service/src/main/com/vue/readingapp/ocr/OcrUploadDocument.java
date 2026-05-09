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
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@RestController
@RequestMapping("/api/v1/documents")
public class OcrUploadDocument {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到文档上传OCR请求 ===");
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
    public static class UploadWithOcrRequest {
        private String title;
        private String author;
        private String tags; // JSON字符串，前端传数组
        private String language;
        private String description;
        private boolean autoOCR;
        private String ocrResult; // JSON字符串
        private boolean extractImages;
        private boolean createThumbnails;
        private boolean preserveFormat;
        private String fileContent; // Base64编码的文件内容

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }

        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public boolean isAutoOCR() { return autoOCR; }
        public void setAutoOCR(boolean autoOCR) { this.autoOCR = autoOCR; }

        public String getOcrResult() { return ocrResult; }
        public void setOcrResult(String ocrResult) { this.ocrResult = ocrResult; }

        public boolean isExtractImages() { return extractImages; }
        public void setExtractImages(boolean extractImages) { this.extractImages = extractImages; }

        public boolean isCreateThumbnails() { return createThumbnails; }
        public void setCreateThumbnails(boolean createThumbnails) { this.createThumbnails = createThumbnails; }

        public boolean isPreserveFormat() { return preserveFormat; }
        public void setPreserveFormat(boolean preserveFormat) { this.preserveFormat = preserveFormat; }

        public String getFileContent() { return fileContent; }
        public void setFileContent(String fileContent) { this.fileContent = fileContent; }
    }

    // OCR结果DTO
    public static class OcrResultData {
        private String text;
        private double confidence;
        private String words; // JSON字符串
        private String lines; // JSON字符串
        private String blocks; // JSON字符串
        private String metadata; // JSON字符串

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public String getWords() { return words; }
        public void setWords(String words) { this.words = words; }

        public String getLines() { return lines; }
        public void setLines(String lines) { this.lines = lines; }

        public String getBlocks() { return blocks; }
        public void setBlocks(String blocks) { this.blocks = blocks; }

        public String getMetadata() { return metadata; }
        public void setMetadata(String metadata) { this.metadata = metadata; }
    }

    // 响应DTO
    public static class UploadWithOcrResponse {
        private boolean success;
        private String message;
        private UploadData data;

        public UploadWithOcrResponse(boolean success, String message, UploadData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UploadData getData() { return data; }
        public void setData(UploadData data) { this.data = data; }
    }

    public static class UploadData {
        private String id;
        private String documentId;
        private String title;
        private boolean ocrProcessed;
        private double ocrConfidence;
        private String ocrText;
        private String ocrPages; // JSON字符串
        private String createdAt;
        private String updatedAt;

        public UploadData(String id, String documentId, String title, boolean ocrProcessed,
                          double ocrConfidence, String ocrText, String ocrPages,
                          String createdAt, String updatedAt) {
            this.id = id;
            this.documentId = documentId;
            this.title = title;
            this.ocrProcessed = ocrProcessed;
            this.ocrConfidence = ocrConfidence;
            this.ocrText = ocrText;
            this.ocrPages = ocrPages;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public boolean isOcrProcessed() { return ocrProcessed; }
        public void setOcrProcessed(boolean ocrProcessed) { this.ocrProcessed = ocrProcessed; }

        public double getOcrConfidence() { return ocrConfidence; }
        public void setOcrConfidence(double ocrConfidence) { this.ocrConfidence = ocrConfidence; }

        public String getOcrText() { return ocrText; }
        public void setOcrText(String ocrText) { this.ocrText = ocrText; }

        public String getOcrPages() { return ocrPages; }
        public void setOcrPages(String ocrPages) { this.ocrPages = ocrPages; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    @PostMapping("/upload-with-ocr")
    public ResponseEntity<UploadWithOcrResponse> uploadWithOcr(@RequestBody UploadWithOcrRequest request) {
        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证请求数据
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UploadWithOcrResponse(false, "文档标题不能为空", null)
                );
            }

            if (request.getFileContent() == null || request.getFileContent().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UploadWithOcrResponse(false, "文件内容不能为空", null)
                );
            }

            // 2. 生成文档ID和OCR结果ID
            String documentId = "doc_" + UUID.randomUUID().toString().substring(0, 8);
            String ocrResultId = "ocr_" + UUID.randomUUID().toString().substring(0, 8);
            LocalDateTime now = LocalDateTime.now();

            // 3. 解析OCR结果（如果提供了）
            double ocrConfidence = 0.0;
            String ocrText = "";
            String ocrPagesJson = "[]";

            if (request.getOcrResult() != null && !request.getOcrResult().trim().isEmpty()) {
                try {
                    OcrResultData ocrData = objectMapper.readValue(request.getOcrResult(), OcrResultData.class);
                    ocrConfidence = ocrData.getConfidence();
                    ocrText = ocrData.getText();

                    // 创建OCR页面数据
                    List<Map<String, Object>> ocrPages = new ArrayList<>();
                    Map<String, Object> pageData = new HashMap<>();
                    pageData.put("page", 1);
                    pageData.put("text", ocrText);
                    pageData.put("confidence", ocrConfidence);
                    ocrPages.add(pageData);

                    ocrPagesJson = objectMapper.writeValueAsString(ocrPages);

                } catch (Exception e) {
                    System.err.println("解析OCR结果失败: " + e.getMessage());
                    return ResponseEntity.badRequest().body(
                            new UploadWithOcrResponse(false, "OCR结果格式错误", null)
                    );
                }
            }

            // 4. 保存文档到数据库
            String insertDocumentSql = "INSERT INTO documents (document_id, user_id, title, author, language, description, file_path, file_size, file_type, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            // 假设当前用户ID为1（课设简化处理）
            int userId = 1;
            String filePath = "/uploads/" + documentId + ".pdf"; // 假设文件路径
            long fileSize = request.getFileContent().length();
            String fileType = "application/pdf"; // 假设文件类型

            jdbcTemplate.update(insertDocumentSql,
                    documentId,
                    userId,
                    request.getTitle(),
                    request.getAuthor(),
                    request.getLanguage(),
                    request.getDescription(),
                    filePath,
                    fileSize,
                    fileType,
                    "processed", // 状态
                    now,
                    now
            );

            // 5. 保存OCR结果到数据库（需要创建OCR结果表）
            // 先检查表是否存在，如果不存在则创建
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'document_ocr_results'";
            Integer tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class);

            if (tableCount == null || tableCount == 0) {
                // 创建OCR结果表
                String createTableSql = "CREATE TABLE document_ocr_results (" +
                        "ocr_id VARCHAR(50) PRIMARY KEY," +
                        "document_id VARCHAR(50) NOT NULL," +
                        "page_number INT NOT NULL," +
                        "ocr_text TEXT," +
                        "confidence DECIMAL(5,2)," +
                        "words_json TEXT," +
                        "lines_json TEXT," +
                        "blocks_json TEXT," +
                        "metadata_json TEXT," +
                        "created_at TIMESTAMP," +
                        "updated_at TIMESTAMP," +
                        "FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE" +
                        ")";
                jdbcTemplate.execute(createTableSql);
                System.out.println("已创建 document_ocr_results 表");
            }

            // 插入OCR结果
            String insertOcrSql = "INSERT INTO document_ocr_results (ocr_id, document_id, page_number, ocr_text, confidence, words_json, lines_json, blocks_json, metadata_json, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            OcrResultData ocrData = null;
            if (request.getOcrResult() != null && !request.getOcrResult().trim().isEmpty()) {
                ocrData = objectMapper.readValue(request.getOcrResult(), OcrResultData.class);
            }

            jdbcTemplate.update(insertOcrSql,
                    ocrResultId,
                    documentId,
                    1, // 第一页
                    ocrText,
                    ocrConfidence,
                    ocrData != null ? ocrData.getWords() : "[]",
                    ocrData != null ? ocrData.getLines() : "[]",
                    ocrData != null ? ocrData.getBlocks() : "[]",
                    ocrData != null ? ocrData.getMetadata() : "{}",
                    now,
                    now
            );

            // 6. 保存标签（如果需要）
            if (request.getTags() != null && !request.getTags().trim().isEmpty()) {
                try {
                    List<String> tags = objectMapper.readValue(request.getTags(), new TypeReference<List<String>>() {});

                    for (String tagName : tags) {
                        // 检查标签是否存在
                        String checkTagSql = "SELECT tag_id FROM document_tags WHERE tag_name = ?";
                        List<Map<String, Object>> existingTags = jdbcTemplate.queryForList(checkTagSql, tagName);

                        String tagId;
                        if (existingTags.isEmpty()) {
                            // 创建新标签
                            tagId = "tag_" + UUID.randomUUID().toString().substring(0, 8);
                            String insertTagSql = "INSERT INTO document_tags (tag_id, tag_name, created_at) VALUES (?, ?, ?)";
                            jdbcTemplate.update(insertTagSql, tagId, tagName, now);
                        } else {
                            tagId = (String) existingTags.get(0).get("tag_id");
                        }

                        // 创建文档标签关系
                        String relationId = "rel_" + UUID.randomUUID().toString().substring(0, 8);
                        String insertRelationSql = "INSERT INTO document_tag_relations (relation_id, document_id, tag_id, created_at) VALUES (?, ?, ?, ?)";
                        jdbcTemplate.update(insertRelationSql, relationId, documentId, tagId, now);
                    }
                } catch (Exception e) {
                    System.err.println("保存标签失败: " + e.getMessage());
                    // 不中断流程，继续执行
                }
            }

            // 7. 准备响应数据
            UploadData responseData = new UploadData(
                    ocrResultId,
                    documentId,
                    request.getTitle(),
                    request.getOcrResult() != null && !request.getOcrResult().trim().isEmpty(),
                    ocrConfidence,
                    ocrText,
                    ocrPagesJson,
                    now.toString(),
                    now.toString()
            );

            UploadWithOcrResponse response = new UploadWithOcrResponse(true, "文档上传并OCR处理成功", responseData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("文档上传OCR处理过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UploadWithOcrResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}