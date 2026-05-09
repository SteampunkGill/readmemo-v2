package com.vue.readingapp.documents;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentsGet {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Map<String, Object> params) {
        System.out.println("=== 收到获取文档列表请求 ===");
        System.out.println("请求参数: " + params);
        System.out.println("========================");
    }

    // 打印查询结果
    private void printQueryResult(List<Map<String, Object>> documents, int total) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("查询到文档数量: " + documents.size());
        System.out.println("总文档数: " + total);
        System.out.println("文档列表摘要 (前5条): " + documents.stream().limit(5).collect(Collectors.toList()));
        System.out.println("===================");
    }

    // 打印返回数据
    private void printResponse(DocumentsResponse response) {
        System.out.println("=== 准备返回的响应 ===");
        if (response != null && response.getData() != null && response.getData().getDocuments() != null) {
            response.getData().setDocuments(response.getData().getDocuments().stream().limit(5).collect(Collectors.toList()));
        }
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 请求 DTO
    public static class DocumentsRequest {
        private Integer page;
        private Integer pageSize;
        private String sortBy;
        private String sortOrder;
        private String search;
        private List<String> tags;
        private String language;
        private String status;

        public Integer getPage() { return page != null ? page : 1; }
        public void setPage(Integer page) { this.page = page; }

        public Integer getPageSize() { return pageSize != null ? pageSize : 20; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

        public String getSortBy() { return sortBy != null ? sortBy : "created_at"; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }

        public String getSortOrder() { return sortOrder != null ? sortOrder : "desc"; }
        public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }

        public String getSearch() { return search; }
        public void setSearch(String search) { this.search = search; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // 响应 DTO
    public static class DocumentsResponse {
        private boolean success;
        private String message;
        private DocumentsData data;

        public DocumentsResponse(boolean success, String message, DocumentsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public DocumentsData getData() { return data; }
        public void setData(DocumentsData data) { this.data = data; }

        @Override
        public String toString() {
            return "DocumentsResponse{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", data=" + data +
                    '}';
        }
    }

    public static class DocumentsData {
        private List<DocumentDTO> documents;
        private PaginationDTO pagination;

        public DocumentsData(List<DocumentDTO> documents, PaginationDTO pagination) {
            this.documents = documents;
            this.pagination = pagination;
        }

        public List<DocumentDTO> getDocuments() { return documents; }
        public void setDocuments(List<DocumentDTO> documents) { this.documents = documents; }

        public PaginationDTO getPagination() { return pagination; }
        public void setPagination(PaginationDTO pagination) { this.pagination = pagination; }

        @Override
        public String toString() {
            return "DocumentsData{" +
                    "documents=" + documents +
                    ", pagination=" + pagination +
                    '}';
        }
    }

    public static class DocumentDTO {
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
        private List<String> tags;
        private Boolean isPublic;
        private Boolean isFavorite;
        private Boolean isProcessed;
        private String uploader;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String thumbnail;
        private String processingStatus = "pending";
        private String processingError;
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

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public Boolean getIsPublic() { return isPublic; }
        public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

        public Boolean getIsFavorite() { return isFavorite; }
        public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }

        public Boolean getIsProcessed() { return isProcessed; }
        public void setIsProcessed(Boolean isProcessed) { this.isProcessed = isProcessed; }

        public String getUploader() { return uploader; }
        public void setUploader(String uploader) { this.uploader = uploader; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

        public String getThumbnail() { return thumbnail; }
        public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

        public String getProcessingStatus() { return processingStatus; }
        public void setProcessingStatus(String processingStatus) { this.processingStatus = processingStatus; }

        public String getProcessingError() { return processingError; }
        public void setProcessingError(String processingError) { this.processingError = processingError; }

        @Override
        public String toString() {
            return "DocumentDTO{" +
                    "id=" + id +
                    ", title='" + title + '\'' +
                    ", description='" + description + '\'' +
                    ", fileName='" + fileName + '\'' +
                    ", fileSize='" + fileSize + '\'' +
                    ", fileType='" + fileType + '\'' +
                    ", language='" + language + '\'' +
                    ", pageCount=" + pageCount +
                    ", readProgress=" + readProgress +
                    ", tags=" + tags +
                    ", isPublic=" + isPublic +
                    ", isFavorite=" + isFavorite +
                    ", uploader='" + uploader + '\'' +
                    ", createdAt=" + createdAt +
                    ", updatedAt=" + updatedAt +
                    ", thumbnail='" + thumbnail + '\'' +
                    ", processingStatus='" + processingStatus + '\'' +
                    '}';
        }
    }

    public static class PaginationDTO {
        private Integer page;
        private Integer pageSize;
        private Integer total;
        private Integer totalPages;

        public PaginationDTO(Integer page, Integer pageSize, Integer total) {
            this.page = page;
            this.pageSize = pageSize;
            this.total = total;
            this.totalPages = (pageSize != null && pageSize > 0) ? (int) Math.ceil((double) total / pageSize) : 0;
        }

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }

        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }

        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }

        @Override
        public String toString() {
            return "PaginationDTO{" +
                    "page=" + page +
                    ", pageSize=" + pageSize +
                    ", total=" + total +
                    ", totalPages=" + totalPages +
                    '}';
        }
    }

    @GetMapping("")
    public ResponseEntity<DocumentsResponse> getDocuments(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String status,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 1. 构建请求参数
            DocumentsRequest request = new DocumentsRequest();
            request.setPage(page);
            request.setPageSize(pageSize);
            request.setSortBy(sortBy);
            request.setSortOrder(sortOrder);
            request.setSearch(search);
            request.setLanguage(language);
            request.setStatus(status);

            if (tags != null && !tags.trim().isEmpty()) {
                request.setTags(Arrays.asList(tags.trim().split(",")));
            }

            // 打印接收到的请求
            Map<String, Object> params = new HashMap<>();
            params.put("page", request.getPage());
            params.put("pageSize", request.getPageSize());
            params.put("sortBy", request.getSortBy());
            params.put("sortOrder", request.getSortOrder());
            params.put("search", request.getSearch());
            params.put("tags", request.getTags());
            params.put("language", request.getLanguage());
            params.put("status", request.getStatus());
            printRequest(params);

            // 2. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DocumentsResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DocumentsResponse(false, "登录已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 3. 构建查询条件
            StringBuilder whereClause = new StringBuilder("WHERE d.user_id = ?");
            List<Object> queryParams = new ArrayList<>();
            queryParams.add(userId);

            // 搜索条件
            if (request.getSearch() != null && !request.getSearch().trim().isEmpty()) {
                whereClause.append(" AND (d.title LIKE ? OR d.description LIKE ?)");
                String searchTerm = "%" + request.getSearch().trim() + "%";
                queryParams.add(searchTerm);
                queryParams.add(searchTerm);
            }

            // 状态过滤
            if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
                whereClause.append(" AND d.status = ?");
                queryParams.add(request.getStatus().trim());
            } else {
                whereClause.append(" AND (d.status IS NULL OR d.status != 'deleted')");
            }

            // 语言过滤
            if (request.getLanguage() != null && !request.getLanguage().trim().isEmpty()) {
                whereClause.append(" AND d.language = ?");
                queryParams.add(request.getLanguage().trim());
            }

            // 标签过滤 - 修复：去掉 dt.user_id 条件
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                whereClause.append(" AND EXISTS (");
                whereClause.append("SELECT 1 FROM document_tag_relations dtr ");
                whereClause.append("JOIN document_tags dt ON dtr.tag_id = dt.tag_id ");
                whereClause.append("WHERE dtr.document_id = d.document_id ");
                whereClause.append("AND dt.tag_name IN (");

                for (int i = 0; i < request.getTags().size(); i++) {
                    if (i > 0) whereClause.append(", ");
                    whereClause.append("?");
                    queryParams.add(request.getTags().get(i));
                }
                whereClause.append(")");
                whereClause.append(")");
            }

            // 4. 获取总数
            String countSql = "SELECT COUNT(*) FROM documents d " + whereClause;
            Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, queryParams.toArray());
            if (total == null) {
                total = 0;
            }

            // 5. 构建排序
            String sortField = request.getSortBy();
            Set<String> allowedSortFields = new HashSet<>(Arrays.asList("created_at", "updated_at", "title", "file_size", "page_count"));
            if (!allowedSortFields.contains(sortField)) {
                sortField = "created_at";
            }

            String orderDirection = "desc".equalsIgnoreCase(request.getSortOrder()) ? "DESC" : "ASC";
            String orderClause = "ORDER BY d." + sortField + " " + orderDirection;

            // 6. 构建分页
            int effectivePageSize = Math.max(request.getPageSize(), 1);
            int offset = (request.getPage() - 1) * effectivePageSize;
            if (offset < 0) {
                offset = 0;
            }
            String limitClause = "LIMIT ? OFFSET ?";

            // 7. 查询文档列表
            String querySql = "SELECT d.document_id, d.title, d.description, d.file_path, d.file_name, d.file_size, " +
                    "d.file_type, d.language, d.page_count, d.reading_progress, d.is_public, " +
                    "d.is_favorite, d.is_processed, d.processing_status, d.processing_error, " +
                    "u.username as uploader " +
                    "FROM documents d " +
                    "LEFT JOIN users u ON d.user_id = u.user_id " +
                    whereClause.toString() + " " + orderClause + " " + limitClause;

            List<Object> finalQueryParams = new ArrayList<>(queryParams);
            finalQueryParams.add(effectivePageSize);
            finalQueryParams.add(offset);

            List<Map<String, Object>> documents = jdbcTemplate.queryForList(querySql, finalQueryParams.toArray());

            // 8. 获取每个文档的标签 - 修复：去掉 dt.user_id 条件
            List<DocumentDTO> documentDTOs = new ArrayList<>();

            for (Map<String, Object> doc : documents) {
                Integer docId = (Integer) doc.get("document_id");
                if (docId == null) continue;

                // 查询文档标签 - 修复：去掉 AND dt.user_id = ? 条件
                String tagSql = "SELECT dt.tag_name FROM document_tag_relations dtr " +
                        "JOIN document_tags dt ON dtr.tag_id = dt.tag_id " +
                        "WHERE dtr.document_id = ?";
                List<Map<String, Object>> tagResults = jdbcTemplate.queryForList(tagSql, docId);

                List<String> tagNames = tagResults.stream()
                        .map(tag -> (String) tag.get("tag_name"))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                // 构建 DTO
                DocumentDTO dto = new DocumentDTO();
                dto.setId(docId);
                dto.setTitle((String) doc.get("title"));
                dto.setDescription((String) doc.get("description"));
                dto.setFilePath((String) doc.get("file_path"));
                dto.setFileName((String) doc.get("file_name"));

                // 格式化文件大小
                Long fileSize = (Long) doc.get("file_size");
                dto.setFileSize(formatFileSize(fileSize));

                dto.setFileType((String) doc.get("file_type"));
                dto.setLanguage((String) doc.get("language"));
                dto.setPageCount((Integer) doc.get("page_count"));

                // 处理 reading_progress
                Object progressObj = doc.get("reading_progress");
                System.out.println("文档ID " + dto.getId() + " 的 reading_progress 原始类型: " + (progressObj != null ? progressObj.getClass().getName() : "null"));
                if (progressObj instanceof Number) {
                    dto.setReadProgress(((Number) progressObj).doubleValue());
                } else {
                    dto.setReadProgress(0.0);
                }

                dto.setTags(tagNames);
                dto.setIsPublic(convertToBoolean(doc.get("is_public")));
                dto.setIsFavorite(convertToBoolean(doc.get("is_favorite")));
                dto.setIsProcessed(convertToBoolean(doc.get("is_processed")));
                dto.setUploader((String) doc.get("uploader"));
                dto.setProcessingStatus((String) doc.get("processing_status"));
                dto.setProcessingError((String) doc.get("processing_error"));

                // 转换 Timestamp 为 LocalDateTime
                dto.setCreatedAt(convertToLocalDateTime(doc.get("created_at")));
                dto.setUpdatedAt(convertToLocalDateTime(doc.get("updated_at")));
                dto.setThumbnail(null);

                documentDTOs.add(dto);
            }

            // 打印查询结果
            printQueryResult(documents, total);

            // 9. 构建响应
            PaginationDTO pagination = new PaginationDTO(
                    request.getPage(),
                    effectivePageSize,
                    total
            );

            DocumentsData data = new DocumentsData(documentDTOs, pagination);
            DocumentsResponse response = new DocumentsResponse(true, "获取文档列表成功", data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取文档列表过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DocumentsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 安全转换布尔值
    private Boolean convertToBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        return false;
    }

    /**
     * 格式化文件大小为人类可读的格式
     */
    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";

        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }

    /**
     * 安全转换 LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        if (value instanceof Timestamp) return ((Timestamp) value).toLocalDateTime();
        return null;
    }
}