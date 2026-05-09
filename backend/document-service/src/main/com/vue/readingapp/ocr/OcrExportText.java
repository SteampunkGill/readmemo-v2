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
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/documents")
public class OcrExportText {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到导出OCR文本请求 ===");
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
    public static class ExportOcrTextRequest {
        private String documentId;
        private List<Integer> pages;
        private String format;
        private Boolean includeMetadata;
        private Boolean includeConfidence;

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public List<Integer> getPages() { return pages; }
        public void setPages(List<Integer> pages) { this.pages = pages; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public Boolean getIncludeMetadata() { return includeMetadata; }
        public void setIncludeMetadata(Boolean includeMetadata) { this.includeMetadata = includeMetadata; }

        public Boolean getIncludeConfidence() { return includeConfidence; }
        public void setIncludeConfidence(Boolean includeConfidence) { this.includeConfidence = includeConfidence; }
    }

    // 响应DTO
    public static class ExportOcrTextResponse {
        private boolean success;
        private String message;
        private ExportData data;

        public ExportOcrTextResponse(boolean success, String message, ExportData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ExportData getData() { return data; }
        public void setData(ExportData data) { this.data = data; }
    }

    public static class ExportData {
        private String exportId;
        private String documentId;
        private String format;
        private String downloadUrl;
        private Integer totalPages;
        private String createdAt;

        public ExportData(String exportId, String documentId, String format,
                          String downloadUrl, Integer totalPages, String createdAt) {
            this.exportId = exportId;
            this.documentId = documentId;
            this.format = format;
            this.downloadUrl = downloadUrl;
            this.totalPages = totalPages;
            this.createdAt = createdAt;
        }

        public String getExportId() { return exportId; }
        public void setExportId(String exportId) { this.exportId = exportId; }

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    @PostMapping("/{documentId}/ocr/export")
    public ResponseEntity<ExportOcrTextResponse> exportOcrText(
            @PathVariable String documentId,
            @RequestBody ExportOcrTextRequest request) {

        // 设置文档ID
        request.setDocumentId(documentId);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证请求数据
            if (documentId == null || documentId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ExportOcrTextResponse(false, "文档ID不能为空", null)
                );
            }

            // 设置默认值
            if (request.getFormat() == null) request.setFormat("txt");
            if (request.getIncludeMetadata() == null) request.setIncludeMetadata(true);
            if (request.getIncludeConfidence() == null) request.setIncludeConfidence(false);

            // 2. 检查文档是否存在
            String checkDocumentSql = "SELECT COUNT(*) FROM documents WHERE document_id = ?";
            Integer documentCount = jdbcTemplate.queryForObject(checkDocumentSql, Integer.class, documentId);

            if (documentCount == null || documentCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ExportOcrTextResponse(false, "文档不存在", null)
                );
            }

            // 3. 检查OCR结果表是否存在
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'document_ocr_results'";
            Integer tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class);

            if (tableCount == null || tableCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ExportOcrTextResponse(false, "没有OCR结果可供导出", null)
                );
            }

            // 4. 构建查询条件
            List<Object> params = new ArrayList<>();
            params.add(documentId);

            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT ocr_id, page_number, ocr_text, confidence, metadata_json ");
            queryBuilder.append("FROM document_ocr_results WHERE document_id = ?");

            // 添加页码条件
            if (request.getPages() != null && !request.getPages().isEmpty()) {
                queryBuilder.append(" AND page_number IN (");
                for (int i = 0; i < request.getPages().size(); i++) {
                    if (i > 0) queryBuilder.append(",");
                    queryBuilder.append("?");
                    params.add(request.getPages().get(i));
                }
                queryBuilder.append(")");
            }

            queryBuilder.append(" ORDER BY page_number");

            // 5. 执行查询
            List<Map<String, Object>> ocrResults = jdbcTemplate.queryForList(queryBuilder.toString(), params.toArray());
            printQueryResult(ocrResults);

            if (ocrResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ExportOcrTextResponse(false, "没有找到符合条件的OCR结果", null)
                );
            }

            // 6. 生成导出内容
            String exportContent = generateExportContent(ocrResults, request);

            // 7. 生成导出ID和下载URL
            String exportId = "export_" + UUID.randomUUID().toString().substring(0, 8);
            String downloadUrl = "/api/exports/" + exportId + "." + request.getFormat();
            LocalDateTime now = LocalDateTime.now();

            // 8. 保存导出记录（需要创建导出记录表）
            String checkExportTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'ocr_exports'";
            Integer exportTableCount = jdbcTemplate.queryForObject(checkExportTableSql, Integer.class);

            if (exportTableCount == null || exportTableCount == 0) {
                // 创建导出记录表
                String createExportTableSql = "CREATE TABLE ocr_exports (" +
                        "export_id VARCHAR(50) PRIMARY KEY," +
                        "document_id VARCHAR(50) NOT NULL," +
                        "format VARCHAR(10) NOT NULL," +
                        "total_pages INT NOT NULL," +
                        "file_path VARCHAR(500)," +
                        "file_size BIGINT," +
                        "download_url VARCHAR(500)," +
                        "created_at TIMESTAMP," +
                        "expires_at TIMESTAMP," +
                        "FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE" +
                        ")";
                jdbcTemplate.execute(createExportTableSql);
                System.out.println("已创建 ocr_exports 表");
            }

            // 插入导出记录
            String insertExportSql = "INSERT INTO ocr_exports (export_id, document_id, format, total_pages, file_path, file_size, download_url, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            String filePath = "/exports/" + exportId + "." + request.getFormat();
            long fileSize = exportContent.getBytes("UTF-8").length;
            LocalDateTime expiresAt = now.plusHours(24); // 24小时后过期

            jdbcTemplate.update(insertExportSql,
                    exportId,
                    documentId,
                    request.getFormat(),
                    ocrResults.size(),
                    filePath,
                    fileSize,
                    downloadUrl,
                    now,
                    expiresAt
            );

            // 9. 在实际项目中，这里应该将exportContent保存到文件系统或云存储
            // 为了课设演示，我们只打印内容
            System.out.println("=== 导出的OCR文本内容 ===");
            System.out.println(exportContent);
            System.out.println("======================");

            // 10. 准备响应数据
            ExportData responseData = new ExportData(
                    exportId,
                    documentId,
                    request.getFormat(),
                    downloadUrl,
                    ocrResults.size(),
                    now.format(formatter)
            );

            ExportOcrTextResponse response = new ExportOcrTextResponse(true, "OCR文本导出成功", responseData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("导出OCR文本过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ExportOcrTextResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private String generateExportContent(List<Map<String, Object>> ocrResults, ExportOcrTextRequest request) {
        StringBuilder contentBuilder = new StringBuilder();

        // 添加标题
        contentBuilder.append("OCR Export - Document: ").append(request.getDocumentId()).append("\n");
        contentBuilder.append("Export Date: ").append(LocalDateTime.now().format(formatter)).append("\n");
        contentBuilder.append("Total Pages: ").append(ocrResults.size()).append("\n");
        contentBuilder.append("Format: ").append(request.getFormat().toUpperCase()).append("\n");
        contentBuilder.append("=".repeat(80)).append("\n\n");

        // 根据格式生成内容
        switch (request.getFormat().toLowerCase()) {
            case "txt":
                contentBuilder.append(generateTxtContent(ocrResults, request));
                break;
            case "json":
                contentBuilder.append(generateJsonContent(ocrResults, request));
                break;
            case "csv":
                contentBuilder.append(generateCsvContent(ocrResults, request));
                break;
            default:
                contentBuilder.append(generateTxtContent(ocrResults, request));
                break;
        }

        return contentBuilder.toString();
    }

    private String generateTxtContent(List<Map<String, Object>> ocrResults, ExportOcrTextRequest request) {
        StringBuilder txtBuilder = new StringBuilder();

        for (Map<String, Object> result : ocrResults) {
            Integer pageNumber = (Integer) result.get("page_number");
            String ocrText = (String) result.get("ocr_text");
            Double confidence = result.get("confidence") != null ? ((Number) result.get("confidence")).doubleValue() : 0.0;

            txtBuilder.append("Page ").append(pageNumber).append("\n");
            txtBuilder.append("-".repeat(40)).append("\n");

            if (request.getIncludeConfidence()) {
                txtBuilder.append("Confidence: ").append(String.format("%.2f", confidence)).append("%\n\n");
            }

            txtBuilder.append(ocrText).append("\n\n");

            if (request.getIncludeMetadata()) {
                String metadataJson = (String) result.get("metadata_json");
                if (metadataJson != null && !metadataJson.trim().isEmpty()) {
                    try {
                        Map<String, Object> metadata = objectMapper.readValue(metadataJson, Map.class);
                        txtBuilder.append("Metadata:\n");
                        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                            txtBuilder.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                        }
                    } catch (Exception e) {
                        txtBuilder.append("Metadata: [Error parsing metadata]\n");
                    }
                }
                txtBuilder.append("\n");
            }

            txtBuilder.append("=".repeat(80)).append("\n\n");
        }

        return txtBuilder.toString();
    }

    private String generateJsonContent(List<Map<String, Object>> ocrResults, ExportOcrTextRequest request) {
        try {
            List<Map<String, Object>> exportData = new ArrayList<>();

            for (Map<String, Object> result : ocrResults) {
                Map<String, Object> item = new HashMap<>();
                item.put("page", result.get("page_number"));
                item.put("text", result.get("ocr_text"));

                if (request.getIncludeConfidence()) {
                    item.put("confidence", result.get("confidence"));
                }

                if (request.getIncludeMetadata()) {
                    String metadataJson = (String) result.get("metadata_json");
                    if (metadataJson != null && !metadataJson.trim().isEmpty()) {
                        try {
                            Map<String, Object> metadata = objectMapper.readValue(metadataJson, Map.class);
                            item.put("metadata", metadata);
                        } catch (Exception e) {
                            item.put("metadata", "Error parsing metadata");
                        }
                    }
                }

                exportData.add(item);
            }

            Map<String, Object> fullExport = new HashMap<>();
            fullExport.put("documentId", request.getDocumentId());
            fullExport.put("exportDate", LocalDateTime.now().format(formatter));
            fullExport.put("totalPages", ocrResults.size());
            fullExport.put("format", request.getFormat());
            fullExport.put("pages", exportData);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(fullExport);
        } catch (Exception e) {
            return "{\"error\": \"Failed to generate JSON content: " + e.getMessage() + "\"}";
        }
    }

    private String generateCsvContent(List<Map<String, Object>> ocrResults, ExportOcrTextRequest request) {
        StringBuilder csvBuilder = new StringBuilder();

        // CSV头部
        csvBuilder.append("Page,Text");
        if (request.getIncludeConfidence()) {
            csvBuilder.append(",Confidence");
        }
        if (request.getIncludeMetadata()) {
            csvBuilder.append(",Metadata");
        }
        csvBuilder.append("\n");

        // CSV数据行
        for (Map<String, Object> result : ocrResults) {
            Integer pageNumber = (Integer) result.get("page_number");
            String ocrText = (String) result.get("ocr_text");
            Double confidence = result.get("confidence") != null ? ((Number) result.get("confidence")).doubleValue() : 0.0;

            // 转义文本中的逗号和引号
            String escapedText = ocrText.replace("\"", "\"\"");
            if (escapedText.contains(",") || escapedText.contains("\"") || escapedText.contains("\n")) {
                escapedText = "\"" + escapedText + "\"";
            }

            csvBuilder.append(pageNumber).append(",").append(escapedText);

            if (request.getIncludeConfidence()) {
                csvBuilder.append(",").append(String.format("%.2f", confidence));
            }

            if (request.getIncludeMetadata()) {
                String metadataJson = (String) result.get("metadata_json");
                if (metadataJson != null && !metadataJson.trim().isEmpty()) {
                    String escapedMetadata = metadataJson.replace("\"", "\"\"");
                    if (escapedMetadata.contains(",") || escapedMetadata.contains("\"") || escapedMetadata.contains("\n")) {
                        escapedMetadata = "\"" + escapedMetadata + "\"";
                    }
                    csvBuilder.append(",").append(escapedMetadata);
                } else {
                    csvBuilder.append(",");
                }
            }

            csvBuilder.append("\n");
        }

        return csvBuilder.toString();
    }

    // 可选：获取导出文件
    @GetMapping("/exports/{exportId}")
    public ResponseEntity<String> getExportFile(@PathVariable String exportId) {
        try {
            // 检查导出记录是否存在
            String checkExportSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'ocr_exports'";
            Integer exportTableCount = jdbcTemplate.queryForObject(checkExportSql, Integer.class);

            if (exportTableCount == null || exportTableCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("导出文件不存在");
            }

            String queryExportSql = "SELECT export_id, document_id, format, download_url, created_at FROM ocr_exports WHERE export_id = ?";
            List<Map<String, Object>> exports = jdbcTemplate.queryForList(queryExportSql, exportId);

            if (exports.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("导出文件不存在");
            }

            Map<String, Object> export = exports.get(0);
            String format = (String) export.get("format");

            // 在实际项目中，这里应该从文件系统或云存储读取文件内容
            // 为了课设演示，我们返回一个模拟的下载链接
            String downloadUrl = "/api/exports/" + exportId + "." + format;

            return ResponseEntity.ok("导出文件下载链接: " + downloadUrl + "\n请在24小时内下载，过期后将失效。");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取导出文件失败: " + e.getMessage());
        }
    }
}