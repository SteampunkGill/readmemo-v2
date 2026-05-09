package com.vue.readingapp.documents;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.io.IOException;
import java.nio.file.*;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentsDuplicate {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 文件存储路径
    private final String UPLOAD_DIR = "uploads/documents/";

    // 打印接收到的请求
    private void printRequest(Integer documentId, String authHeader) {
        System.out.println("=== 收到复制文档请求 ===");
        System.out.println("文档ID: " + documentId);
        System.out.println("认证头: " + authHeader);
        System.out.println("=====================");
    }

    // 打印查询结果
    private void printQueryResult(Map<String, Object> originalDocument, Integer newDocumentId) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("原始文档: " + originalDocument);
        System.out.println("新文档ID: " + newDocumentId);
        System.out.println("===================");
    }

    // 打印返回数据
    private void printResponse(DuplicateResponse response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 响应DTO
    public static class DuplicateResponse {
        private boolean success;
        private String message;
        private DuplicateData data;

        public DuplicateResponse(boolean success, String message, DuplicateData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public DuplicateData getData() { return data; }
        public void setData(DuplicateData data) { this.data = data; }
    }

    public static class DuplicateData {
        private DuplicateDocumentDTO document;

        public DuplicateData(DuplicateDocumentDTO document) {
            this.document = document;
        }

        public DuplicateDocumentDTO getDocument() { return document; }
        public void setDocument(DuplicateDocumentDTO document) { this.document = document; }
    }

    public static class DuplicateDocumentDTO {
        private Integer id;
        private String title;
        private String fileName;
        private String fileSize;
        private String fileType;
        private String status;
        private String createdAt;

        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getFileSize() { return fileSize; }
        public void setFileSize(String fileSize) { this.fileSize = fileSize; }

        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    @PostMapping("/{documentId}/duplicate")
    public ResponseEntity<DuplicateResponse> duplicateDocument(
            @PathVariable("documentId") Integer documentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest(documentId, authHeader);

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DuplicateResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DuplicateResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 2. 查询原始文档信息
            String originalSql = "SELECT * FROM documents WHERE document_id = ? AND user_id = ? AND deleted_at IS NULL";
            List<Map<String, Object>> originalDocuments = jdbcTemplate.queryForList(originalSql, documentId, userId);

            if (originalDocuments.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DuplicateResponse(false, "原始文档不存在或没有权限", null)
                );
            }

            Map<String, Object> originalDocument = originalDocuments.get(0);

            // 3. 复制文件
            String originalFilePath = (String) originalDocument.get("file_path");
            String originalFileName = (String) originalDocument.get("file_name");

            if (originalFilePath == null || originalFilePath.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new DuplicateResponse(false, "原始文件路径无效", null)
                );
            }

            Path originalPath = Paths.get(originalFilePath);
            if (!Files.exists(originalPath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DuplicateResponse(false, "原始文件不存在", null)
                );
            }

            // 生成新的文件名
            String fileExtension = getFileExtension(originalFileName);
            String newFileName = "copy_of_" + originalFileName;
            String uniqueFilename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + fileExtension;

            // 创建上传目录
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 复制文件
            Path newFilePath = uploadPath.resolve(uniqueFilename);
            Files.copy(originalPath, newFilePath, StandardCopyOption.REPLACE_EXISTING);

            // 4. 创建新的文档记录
            LocalDateTime now = LocalDateTime.now();
            Timestamp timestamp = Timestamp.valueOf(now);

            String originalTitle = (String) originalDocument.get("title");
            String newTitle = "副本 - " + originalTitle;

            String insertSql = "INSERT INTO documents (user_id, title, description, file_path, file_name, " +
                    "file_size, file_type, language, page_count, reading_progress, " +
                    "current_page, is_public, is_favorite, is_processed, processing_status, " +
                    "processing_progress, status, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(insertSql,
                    userId,
                    newTitle,
                    originalDocument.get("description"),
                    newFilePath.toString(),
                    newFileName,
                    originalDocument.get("file_size"),
                    originalDocument.get("file_type"),
                    originalDocument.get("language"),
                    originalDocument.get("page_count"),
                    0, // 重置阅读进度
                    1, // 重置当前页码
                    false, // 默认不公开
                    false, // 默认不收藏
                    originalDocument.get("is_processed"),
                    originalDocument.get("processing_status"),
                    0, // 重置处理进度
                    "uploading",
                    timestamp,
                    timestamp
            );

            // 获取插入的文档ID
            String lastIdSql = "SELECT LAST_INSERT_ID() as id";
            Integer newDocumentId = jdbcTemplate.queryForObject(lastIdSql, Integer.class);

            // 5. 复制标签关系
            String tagRelationsSql = "SELECT tag_id FROM document_tag_relations WHERE document_id = ?";
            List<Map<String, Object>> tagRelations = jdbcTemplate.queryForList(tagRelationsSql, documentId);

            for (Map<String, Object> relation : tagRelations) {
                Integer tagId = (Integer) relation.get("tag_id");

                String insertRelationSql = "INSERT INTO document_tag_relations (document_id, tag_id, created_at) VALUES (?, ?, ?)";
                jdbcTemplate.update(insertRelationSql, newDocumentId, tagId, timestamp);
            }

            // 6. 添加到文档处理队列
            String insertQueueSql = "INSERT INTO document_processing_queue (document_id, status, priority, created_at) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(insertQueueSql, newDocumentId, "pending", 1, timestamp);

            printQueryResult(originalDocument, newDocumentId);

            // 7. 构建响应数据
            DuplicateDocumentDTO dto = new DuplicateDocumentDTO();
            dto.setId(newDocumentId);
            dto.setTitle(newTitle);
            dto.setFileName(newFileName);
            dto.setFileSize(formatFileSize((Long) originalDocument.get("file_size")));
            dto.setFileType((String) originalDocument.get("file_type"));
            dto.setStatus("uploading");
            dto.setCreatedAt(now.toString());

            DuplicateData data = new DuplicateData(dto);
            DuplicateResponse response = new DuplicateResponse(true, "文档复制成功", data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            System.err.println("文件复制过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DuplicateResponse(false, "文件复制失败: " + e.getMessage(), null)
            );
        } catch (Exception e) {
            System.err.println("复制文档过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DuplicateResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 获取文件扩展名
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
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