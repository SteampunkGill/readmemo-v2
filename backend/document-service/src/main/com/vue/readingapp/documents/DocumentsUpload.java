package com.vue.readingapp.documents;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import com.vue.readingapp.ocr.OcrService;
import java.util.*;
import java.time.LocalDateTime;
import java.io.IOException;
import java.nio.file.*;
import java.sql.Timestamp;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentsUpload {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OcrService ocrService;

    // 文件存储路径
    private final String UPLOAD_DIR = "uploads/documents/";

    // 打印接收到的请求
    private void printRequest(String title, String description, String tags, String language, String fileName, long fileSize) {
        System.out.println("=== 收到上传文档请求 ===");
        System.out.println("标题: " + title);
        System.out.println("描述: " + description);
        System.out.println("标签: " + tags);
        System.out.println("语言: " + language);
        System.out.println("文件名: " + fileName);
        System.out.println("文件大小: " + fileSize + " bytes");
        System.out.println("=====================");
    }

    // 打印返回数据
    private void printResponse(UploadResponse response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 请求DTO
    public static class UploadRequest {
        private String title;
        private String description;
        private String tags;
        private String language;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }

    // 响应DTO
    public static class UploadResponse {
        private boolean success;
        private String message;
        private UploadData data;

        public UploadResponse(boolean success, String message, UploadData data) {
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
        private UploadDocumentDTO document;

        public UploadData(UploadDocumentDTO document) {
            this.document = document;
        }

        public UploadDocumentDTO getDocument() { return document; }
        public void setDocument(UploadDocumentDTO document) { this.document = document; }
    }

    public static class UploadDocumentDTO {
        private Integer id;
        private String title;
        private String fileName;
        private String fileSize;
        private String fileType;
        private String status;
        private Integer processingProgress;
        private LocalDateTime createdAt;

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

        public Integer getProcessingProgress() { return processingProgress; }
        public void setProcessingProgress(Integer processingProgress) { this.processingProgress = processingProgress; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "language", required = false) String language,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest(title, description, tags, language, file.getOriginalFilename(), file.getSize());

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UploadResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UploadResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 2. 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UploadResponse(false, "文件不能为空", null)
                );
            }

            // 验证文件类型
            String contentType = file.getContentType();
            List<String> allowedTypes = Arrays.asList(
                    "application/pdf",
                    "application/epub+zip",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "text/plain",
                    "text/html"
            );

            if (!allowedTypes.contains(contentType)) {
                return ResponseEntity.badRequest().body(
                        new UploadResponse(false, "不支持的文件类型: " + contentType, null)
                );
            }

            // 验证文件大小（最大100MB）
            long maxSize = 100 * 1024 * 1024; // 100MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest().body(
                        new UploadResponse(false, "文件大小不能超过100MB", null)
                );
            }

            // 3. 保存文件
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + fileExtension;

            // 创建上传目录
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 4. 保存到数据库
            LocalDateTime now = LocalDateTime.now();
            Timestamp timestamp = Timestamp.valueOf(now);

            String insertSql = "INSERT INTO documents (user_id, title, description, file_path, file_name, " +
                    "file_size, file_type, language, status, processing_progress, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(insertSql,
                    userId,
                    title,
                    description,
                    filePath.toString(),
                    originalFilename,
                    file.getSize(),
                    contentType,
                    language != null ? language : "en",
                    "uploaded", // 修改状态为 uploaded，表示已上传完成
                    0,
                    timestamp,
                    timestamp
            );

            // 获取插入的文档ID
            String lastIdSql = "SELECT LAST_INSERT_ID() as id";
            Integer documentId = jdbcTemplate.queryForObject(lastIdSql, Integer.class);

            System.out.println("INFO: 文档上传成功，文档ID: " + documentId);

            // 5. 处理标签
            if (tags != null && !tags.trim().isEmpty()) {
                // 解析标签（JSON格式或逗号分隔）
                List<String> tagList = new ArrayList<>();
                if (tags.startsWith("[") && tags.endsWith("]")) {
                    // JSON数组格式
                    tags = tags.substring(1, tags.length() - 1);
                    String[] tagArray = tags.split(",");
                    for (String tag : tagArray) {
                        tagList.add(tag.trim().replace("\"", ""));
                    }
                } else {
                    // 逗号分隔格式
                    String[] tagArray = tags.split(",");
                    for (String tag : tagArray) {
                        tagList.add(tag.trim());
                    }
                }

                // 为每个标签创建或获取标签ID，并建立关系
                for (String tagName : tagList) {
                    if (tagName.isEmpty()) continue;

                    // 检查标签是否已存在
                    String checkTagSql = "SELECT tag_id FROM document_tags WHERE tag_name = ?";
                    List<Map<String, Object>> existingTags = jdbcTemplate.queryForList(checkTagSql, tagName);

                    Integer tagId;
                    if (existingTags.isEmpty()) {
                        // 创建新标签
                        String insertTagSql = "INSERT INTO document_tags (tag_name, created_at) VALUES (?, ?)";
                        jdbcTemplate.update(insertTagSql, tagName, timestamp);

                        String lastTagIdSql = "SELECT LAST_INSERT_ID() as id";
                        tagId = jdbcTemplate.queryForObject(lastTagIdSql, Integer.class);
                    } else {
                        tagId = (Integer) existingTags.get(0).get("tag_id");
                    }

                    // 建立文档-标签关系
                    String insertRelationSql = "INSERT INTO document_tag_relations (document_id, tag_id, created_at) VALUES (?, ?, ?)";
                    jdbcTemplate.update(insertRelationSql, documentId, tagId, timestamp);
                }
            }

            // 6. 自动添加到文档处理队列
            boolean addedToQueue = addDocumentToProcessingQueue(documentId, userId);

            if (addedToQueue) {
                System.out.println("INFO: 文档已成功添加到处理队列，文档ID: " + documentId);

                // 更新文档状态为 pending
                String updateDocSql = "UPDATE documents SET status = ?, updated_at = ? WHERE document_id = ?";
                jdbcTemplate.update(updateDocSql, "pending", timestamp, documentId);
            } else {
                System.err.println("WARNING: 添加文档到处理队列失败，文档ID: " + documentId);
                // 如果添加到队列失败，设置状态为需要手动处理
                String updateDocSql = "UPDATE documents SET status = ?, updated_at = ? WHERE document_id = ?";
                jdbcTemplate.update(updateDocSql, "needs_manual_processing", timestamp, documentId);
            }

            // 7. 构建响应数据
            UploadDocumentDTO dto = new UploadDocumentDTO();
            dto.setId(documentId);
            dto.setTitle(title);
            dto.setFileName(originalFilename);
            dto.setFileSize(formatFileSize(file.getSize()));
            dto.setFileType(contentType);
            dto.setStatus(addedToQueue ? "pending" : "needs_manual_processing");
            dto.setProcessingProgress(0);
            dto.setCreatedAt(now);

            UploadData data = new UploadData(dto);
            String message = addedToQueue ? "文档上传成功并已加入处理队列" : "文档上传成功，但需要手动加入处理队列";
            UploadResponse response = new UploadResponse(true, message, data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            System.err.println("文件保存过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UploadResponse(false, "文件保存失败: " + e.getMessage(), null)
            );
        } catch (Exception e) {
            System.err.println("上传文档过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UploadResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    /**
     * 手动触发文档处理 - 批量处理
     * 可以一次处理多个文档
     */
    @PostMapping("/process-manually/batch")
    public ResponseEntity<Map<String, Object>> processDocumentsManuallyBatch(
            @RequestBody BatchProcessRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            System.out.println("=== 收到批量手动处理文档请求 ===");
            System.out.println("文档ID列表: " + request.getDocumentIds());
            System.out.println("处理选项: " + request.getOptions());

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of("success", false, "message", "请先登录")
                );
            }

            String token = authHeader.substring(7);
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of("success", false, "message", "登录已过期，请重新登录")
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            List<Integer> documentIds = request.getDocumentIds();
            if (documentIds == null || documentIds.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("success", false, "message", "请提供要处理的文档ID列表")
                );
            }

            // 2. 验证文档是否存在且属于当前用户
            String placeholders = String.join(",", Collections.nCopies(documentIds.size(), "?"));
            String docSql = "SELECT document_id, title, status, is_processed FROM documents " +
                    "WHERE document_id IN (" + placeholders + ") AND user_id = ?";

            List<Object> params = new ArrayList<>(documentIds);
            params.add(userId);

            List<Map<String, Object>> docResults = jdbcTemplate.queryForList(docSql, params.toArray());

            if (docResults.size() != documentIds.size()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        Map.of("success", false, "message", "部分文档不存在或无权访问")
                );
            }

            // 3. 批量添加到处理队列
            LocalDateTime now = LocalDateTime.now();
            Timestamp timestamp = Timestamp.valueOf(now);

            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int skippedCount = 0;
            int failedCount = 0;

            for (Map<String, Object> document : docResults) {
                Integer documentId = (Integer) document.get("document_id");
                String status = (String) document.get("status");
                Integer isProcessed = (Integer) document.get("is_processed");

                Map<String, Object> result = new HashMap<>();
                result.put("documentId", documentId);
                result.put("title", document.get("title"));

                // 检查文档是否已经在处理中或已完成
                if ("processing".equals(status) || (isProcessed != null && isProcessed == 1)) {
                    result.put("status", "skipped");
                    result.put("message", "文档已在处理中或已完成处理");
                    skippedCount++;
                } else {
                    // 检查是否已经在处理队列中
                    String checkQueueSql = "SELECT queue_id, status FROM document_processing_queue WHERE document_id = ?";
                    List<Map<String, Object>> queueResults = jdbcTemplate.queryForList(checkQueueSql, documentId);

                    boolean alreadyInQueue = false;
                    if (!queueResults.isEmpty()) {
                        Map<String, Object> queueTask = queueResults.get(0);
                        String queueStatus = (String) queueTask.get("status");
                        if ("pending".equals(queueStatus) || "processing".equals(queueStatus)) {
                            alreadyInQueue = true;
                            result.put("status", "skipped");
                            result.put("message", "文档已在处理队列中，状态: " + queueStatus);
                            skippedCount++;
                        }
                    }

                    if (!alreadyInQueue) {
                        // 添加到处理队列
                        String insertQueueSql = "INSERT INTO document_processing_queue (document_id, status, priority, created_at) " +
                                "VALUES (?, ?, ?, ?)";
                        int queueRows = jdbcTemplate.update(insertQueueSql, documentId, "pending", 1, timestamp);

                        if (queueRows > 0) {
                            // 更新文档状态
                            String updateDocSql = "UPDATE documents SET status = ?, updated_at = ? WHERE document_id = ?";
                            jdbcTemplate.update(updateDocSql, "pending", timestamp, documentId);

                            result.put("status", "success");
                            result.put("message", "已成功添加到处理队列");
                            successCount++;
                        } else {
                            result.put("status", "failed");
                            result.put("message", "添加文档到处理队列失败");
                            failedCount++;
                        }
                    }
                }

                results.add(result);
            }

            System.out.println("INFO: 批量处理完成 - 成功: " + successCount + ", 跳过: " + skippedCount + ", 失败: " + failedCount);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "批量处理完成",
                    "total", documentIds.size(),
                    "success", successCount,
                    "skipped", skippedCount,
                    "failed", failedCount,
                    "results", results
            ));

        } catch (Exception e) {
            System.err.println("批量手动处理文档过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "message", "服务器内部错误: " + e.getMessage())
            );
        }
    }

    /**
     * 手动触发文档处理 - 单个文档
     */
    @PostMapping("/{documentId}/process-manually")
    public ResponseEntity<Map<String, Object>> processDocumentManually(
            @PathVariable Integer documentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            System.out.println("=== 收到手动处理文档请求 ===");
            System.out.println("文档ID: " + documentId);

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of("success", false, "message", "请先登录")
                );
            }

            String token = authHeader.substring(7);
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of("success", false, "message", "登录已过期，请重新登录")
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 2. 验证文档是否存在且属于当前用户
            String docSql = "SELECT document_id, title, status, is_processed FROM documents WHERE document_id = ? AND user_id = ?";
            List<Map<String, Object>> docResults = jdbcTemplate.queryForList(docSql, documentId, userId);

            if (docResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        Map.of("success", false, "message", "文档不存在或无权访问")
                );
            }

            Map<String, Object> document = docResults.get(0);
            String status = (String) document.get("status");
            Integer isProcessed = (Integer) document.get("is_processed");

            System.out.println("INFO: 文档状态 - 状态: " + status + ", 是否已处理: " + isProcessed);

            // 3. 检查文档是否已经在处理中或已完成
            if ("processing".equals(status) || (isProcessed != null && isProcessed == 1)) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "文档已在处理中或已完成处理",
                        "currentStatus", status,
                        "isProcessed", isProcessed
                ));
            }

            // 4. 添加到处理队列
            LocalDateTime now = LocalDateTime.now();
            Timestamp timestamp = Timestamp.valueOf(now);

            boolean addedToQueue = addDocumentToProcessingQueue(documentId, userId);

            if (addedToQueue) {
                System.out.println("INFO: 已成功添加文档到处理队列，文档ID: " + documentId);

                // 更新文档状态
                String updateDocSql = "UPDATE documents SET status = ?, updated_at = ? WHERE document_id = ?";
                jdbcTemplate.update(updateDocSql, "pending", timestamp, documentId);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "文档已成功添加到处理队列",
                        "documentId", documentId,
                        "status", "pending"
                ));
            } else {
                System.err.println("ERROR: 添加文档到处理队列失败，文档ID: " + documentId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        Map.of("success", false, "message", "添加文档到处理队列失败")
                );
            }

        } catch (Exception e) {
            System.err.println("手动处理文档过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "message", "服务器内部错误: " + e.getMessage())
            );
        }
    }

    /**
     * 获取需要手动处理的文档列表
     */
    @GetMapping("/needs-manual-processing")
    public ResponseEntity<Map<String, Object>> getDocumentsNeedingManualProcessing(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            System.out.println("=== 收到获取需要手动处理文档列表请求 ===");
            System.out.println("页码: " + page + ", 每页大小: " + pageSize);

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of("success", false, "message", "请先登录")
                );
            }

            String token = authHeader.substring(7);
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of("success", false, "message", "登录已过期，请重新登录")
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 计算偏移量
            int offset = (page - 1) * pageSize;

            // 2. 查询需要手动处理的文档
            String countSql = "SELECT COUNT(*) as total FROM documents " +
                    "WHERE user_id = ? AND status IN ('needs_manual_processing', 'uploaded') " +
                    "AND (is_processed IS NULL OR is_processed = 0)";

            Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, userId);

            String querySql = "SELECT document_id, title, file_name, file_size, file_type, status, " +
                    "processing_progress, created_at, updated_at " +
                    "FROM documents " +
                    "WHERE user_id = ? AND status IN ('needs_manual_processing', 'uploaded') " +
                    "AND (is_processed IS NULL OR is_processed = 0) " +
                    "ORDER BY created_at DESC " +
                    "LIMIT ? OFFSET ?";

            List<Map<String, Object>> documents = jdbcTemplate.queryForList(querySql, userId, pageSize, offset);

            // 3. 检查每个文档是否已经在处理队列中
            for (Map<String, Object> document : documents) {
                Integer documentId = (Integer) document.get("document_id");

                String queueSql = "SELECT queue_id, status FROM document_processing_queue WHERE document_id = ?";
                List<Map<String, Object>> queueResults = jdbcTemplate.queryForList(queueSql, documentId);

                if (!queueResults.isEmpty()) {
                    Map<String, Object> queueTask = queueResults.get(0);
                    String queueStatus = (String) queueTask.get("status");
                    document.put("queueStatus", queueStatus);
                    document.put("queueId", queueTask.get("queue_id"));
                } else {
                    document.put("queueStatus", "not_in_queue");
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取需要手动处理的文档列表成功");
            response.put("total", total);
            response.put("page", page);
            response.put("pageSize", pageSize);
            response.put("totalPages", (int) Math.ceil((double) total / pageSize));
            response.put("documents", documents);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取需要手动处理的文档列表过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "message", "服务器内部错误: " + e.getMessage())
            );
        }
    }

    /**
     * 检查文档处理状态
     */
    @GetMapping("/{documentId}/upload-status")
    public ResponseEntity<Map<String, Object>> getProcessingStatus(
            @PathVariable Integer documentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            System.out.println("=== 收到检查文档处理状态请求 ===");
            System.out.println("文档ID: " + documentId);

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of("success", false, "message", "请先登录")
                );
            }

            String token = authHeader.substring(7);
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of("success", false, "message", "登录已过期，请重新登录")
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 2. 验证文档是否存在且属于当前用户
            String docSql = "SELECT document_id, title, status, is_processed, processing_status, processing_progress, " +
                    "processing_started_at, processing_completed_at " +
                    "FROM documents WHERE document_id = ? AND user_id = ?";
            List<Map<String, Object>> docResults = jdbcTemplate.queryForList(docSql, documentId, userId);

            if (docResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        Map.of("success", false, "message", "文档不存在或无权访问")
                );
            }

            Map<String, Object> document = docResults.get(0);

            // 3. 检查处理队列状态
            String queueSql = "SELECT queue_id, status, created_at, started_at, completed_at " +
                    "FROM document_processing_queue WHERE document_id = ? ORDER BY created_at DESC LIMIT 1";
            List<Map<String, Object>> queueResults = jdbcTemplate.queryForList(queueSql, documentId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文档处理状态查询成功");
            response.put("document", document);
            response.put("queue", queueResults.isEmpty() ? null : queueResults.get(0));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("检查文档处理状态过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "message", "服务器内部错误: " + e.getMessage())
            );
        }
    }

    /**
     * 添加文档到处理队列的通用方法
     */
    private boolean addDocumentToProcessingQueue(Integer documentId, Integer userId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            Timestamp timestamp = Timestamp.valueOf(now);

            // 检查是否已经在处理队列中
            String checkQueueSql = "SELECT queue_id, status FROM document_processing_queue WHERE document_id = ?";
            List<Map<String, Object>> queueResults = jdbcTemplate.queryForList(checkQueueSql, documentId);

            if (!queueResults.isEmpty()) {
                Map<String, Object> queueTask = queueResults.get(0);
                String queueStatus = (String) queueTask.get("status");

                if ("pending".equals(queueStatus) || "processing".equals(queueStatus)) {
                    System.out.println("INFO: 文档已在处理队列中，文档ID: " + documentId + ", 状态: " + queueStatus);
                    return true; // 已经在队列中，返回成功
                }
            }

            // 添加到处理队列
            String insertQueueSql = "INSERT INTO document_processing_queue (document_id, status, priority, created_at) " +
                    "VALUES (?, ?, ?, ?)";
            int queueRows = jdbcTemplate.update(insertQueueSql, documentId, "pending", 1, timestamp);

            return queueRows > 0;

        } catch (Exception e) {
            System.err.println("添加文档到处理队列失败，文档ID: " + documentId + ", 错误: " + e.getMessage());
            return false;
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

    /**
     * 批量处理请求DTO
     */
    public static class BatchProcessRequest {
        private List<Integer> documentIds;
        private Map<String, Object> options;

        public List<Integer> getDocumentIds() { return documentIds; }
        public void setDocumentIds(List<Integer> documentIds) { this.documentIds = documentIds; }

        public Map<String, Object> getOptions() { return options; }
        public void setOptions(Map<String, Object> options) { this.options = options; }
    }
}