package com.vue.readingapp.documents;

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
import java.sql.Timestamp;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentsGetById {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // 打印接收到的请求
    private void printRequest(Integer documentId, String authHeader) {
        System.out.println("=== 收到获取文档详情请求 ===");
        System.out.println("文档ID: " + documentId);
        System.out.println("认证头: " + authHeader);
        System.out.println("========================");
    }

    // 打印返回数据
    private void printResponse(DocumentDetailResponse response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 响应DTO
    public static class DocumentDetailResponse {
        private boolean success;
        private String message;
        private DocumentDetailDTO data;

        public DocumentDetailResponse(boolean success, String message, DocumentDetailDTO data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public DocumentDetailDTO getData() { return data; }
        public void setData(DocumentDetailDTO data) { this.data = data; }
    }

    public static class DocumentDetailDTO {
        private Integer id;
        private String title;
        private String description;
        private String filePath;
        private String fileName;
        private String fileSize;
        private String fileType;
        private String language;
        private Integer pageCount;
        private Double readProgress;
        private Boolean isPublic;
        private Boolean isFavorite;
        private Boolean isProcessed;
        private String processingStatus;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime lastReadAt;
        private String uploader;
        private List<String> tags;
        private String thumbnail;
        private String processingError;
        private Long wordCount;
        private Integer noteCount;
        private Integer highlightCount;

        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getFileSize() { return fileSize; }
        public void setFileSize(String fileSize) { this.fileSize = fileSize; }

        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public Integer getPageCount() { return pageCount; }
        public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

        public Double getReadProgress() { return readProgress; }
        public void setReadProgress(Double readProgress) { this.readProgress = readProgress; }

        public Boolean getIsPublic() { return isPublic; }
        public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

        public Boolean getIsFavorite() { return isFavorite; }
        public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }

        public Boolean getIsProcessed() { return isProcessed; }
        public void setIsProcessed(Boolean isProcessed) { this.isProcessed = isProcessed; }

        public String getProcessingStatus() { return processingStatus; }
        public void setProcessingStatus(String processingStatus) { this.processingStatus = processingStatus; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

        public LocalDateTime getLastReadAt() { return lastReadAt; }
        public void setLastReadAt(LocalDateTime lastReadAt) { this.lastReadAt = lastReadAt; }

        public String getUploader() { return uploader; }
        public void setUploader(String uploader) { this.uploader = uploader; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public String getThumbnail() { return thumbnail; }
        public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

        public String getProcessingError() { return processingError; }
        public void setProcessingError(String processingError) { this.processingError = processingError; }

        public Long getWordCount() { return wordCount; }
        public void setWordCount(Long wordCount) { this.wordCount = wordCount; }

        public Integer getNoteCount() { return noteCount; }
        public void setNoteCount(Integer noteCount) { this.noteCount = noteCount; }

        public Integer getHighlightCount() { return highlightCount; }
        public void setHighlightCount(Integer highlightCount) { this.highlightCount = highlightCount; }
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentDetailResponse> getDocumentById(
            @PathVariable Integer documentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest(documentId, authHeader);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DocumentDetailResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);
            
            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DocumentDetailResponse(false, "登录已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 2. 查询文档详情
            String sql = "SELECT d.document_id, d.title, d.description, d.file_path, d.file_name, " +
                    "d.file_size, d.file_type, d.language, d.page_count, d.reading_progress, " +
                    "d.is_public, d.is_favorite, d.is_processed, d.processing_status, " +
                    "d.created_at, d.updated_at, d.last_read_at, u.username as uploader, " +
                    "d.processing_error, " +
                    "(SELECT COALESCE(SUM(word_count), 0) FROM document_pages WHERE document_id = d.document_id) as word_count, " +
                    "(SELECT COUNT(*) FROM document_notes WHERE document_id = d.document_id) as note_count, " +
                    "(SELECT COUNT(*) FROM document_highlights WHERE document_id = d.document_id) as highlight_count " +
                    "FROM documents d " +
                    "LEFT JOIN users u ON d.user_id = u.user_id " +
                    "WHERE d.document_id = ? AND d.user_id = ? AND (d.status IS NULL OR d.status != 'deleted')";

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, documentId, userId);

            if (results.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DocumentDetailResponse(false, "文档不存在或无权访问", null)
                );
            }

            Map<String, Object> row = results.get(0);

            // 3. 查询文档标签
            String tagsSql = "SELECT dt.tag_name FROM document_tag_relations dtr " +
                    "JOIN document_tags dt ON dtr.tag_id = dt.tag_id " +
                    "WHERE dtr.document_id = ?";
            List<Map<String, Object>> tagResults = jdbcTemplate.queryForList(tagsSql, documentId);
            List<String> tags = new java.util.ArrayList<>();
            for (Map<String, Object> tagRow : tagResults) {
                tags.add((String) tagRow.get("tag_name"));
            }

            // 4. 构建响应数据
            DocumentDetailDTO dto = new DocumentDetailDTO();
            dto.setId((Integer) row.get("document_id"));
            dto.setTitle((String) row.get("title"));
            dto.setDescription((String) row.get("description"));
            dto.setFilePath((String) row.get("file_path"));
            dto.setFileName((String) row.get("file_name"));
            
            // 安全转换 file_size
            Object fileSizeObj = row.get("file_size");
            if (fileSizeObj instanceof Number) {
                dto.setFileSize(formatFileSize(((Number) fileSizeObj).longValue()));
            } else {
                dto.setFileSize("0 B");
            }
            
            dto.setFileType((String) row.get("file_type"));
            dto.setLanguage((String) row.get("language"));
            dto.setPageCount((Integer) row.get("page_count"));
            
            // 安全转换 reading_progress
            Object readingProgress = row.get("reading_progress");
            if (readingProgress instanceof Number) {
                dto.setReadProgress(((Number) readingProgress).doubleValue());
            } else {
                dto.setReadProgress(0.0);
            }
            
            // 安全转换布尔值
            dto.setIsPublic(convertToBoolean(row.get("is_public")));
            dto.setIsFavorite(convertToBoolean(row.get("is_favorite")));
            dto.setIsProcessed(convertToBoolean(row.get("is_processed")));
            dto.setProcessingStatus((String) row.get("processing_status"));
            
            // 安全转换时间戳
            dto.setCreatedAt(convertToLocalDateTime(row.get("created_at")));
            dto.setUpdatedAt(convertToLocalDateTime(row.get("updated_at")));
            dto.setLastReadAt(convertToLocalDateTime(row.get("last_read_at")));
            
            dto.setUploader((String) row.get("uploader"));
            dto.setTags(tags);
            dto.setThumbnail(null);
            dto.setProcessingError((String) row.get("processing_error"));
            
            // 设置统计数据
            Object wordCountObj = row.get("word_count");
            dto.setWordCount(wordCountObj != null ? ((Number) wordCountObj).longValue() : 0L);
            
            Object noteCountObj = row.get("note_count");
            dto.setNoteCount(noteCountObj != null ? ((Number) noteCountObj).intValue() : 0);
            
            Object highlightCountObj = row.get("highlight_count");
            dto.setHighlightCount(highlightCountObj != null ? ((Number) highlightCountObj).intValue() : 0);

            DocumentDetailResponse response = new DocumentDetailResponse(true, "获取文档详情成功", dto);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取文档详情过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DocumentDetailResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 安全转换 LocalDateTime
    private LocalDateTime convertToLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        if (value instanceof Timestamp) return ((Timestamp) value).toLocalDateTime();
        return null;
    }

    // 安全转换布尔值
    private Boolean convertToBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        return false;
    }

    // 格式化文件大小
    private String formatFileSize(long bytes) {
        if (bytes == 0) return "0 B";

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }
}