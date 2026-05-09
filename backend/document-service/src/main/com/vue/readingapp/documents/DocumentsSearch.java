package com.vue.readingapp.documents;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentsSearch {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(String query, Map<String, Object> params) {
        System.out.println("=== 收到搜索文档请求 ===");
        System.out.println("搜索关键词: " + query);
        System.out.println("搜索参数: " + params);
        System.out.println("=====================");
    }

    // 打印查询结果
    private void printQueryResult(List<Map<String, Object>> documents, int total) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("搜索到文档数量: " + documents.size());
        System.out.println("总文档数: " + total);
        System.out.println("文档列表: " + documents);
        System.out.println("===================");
    }

    // 打印返回数据
    private void printResponse(SearchResponse response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 请求DTO
    public static class SearchRequest {
        private String query;
        private Integer page;
        private Integer pageSize;
        private String sortBy;

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public Integer getPage() { return page != null ? page : 1; }
        public void setPage(Integer page) { this.page = page; }

        public Integer getPageSize() { return pageSize != null ? pageSize : 20; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

        public String getSortBy() { return sortBy != null ? sortBy : "relevance"; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    }

    // 响应DTO
    public static class SearchResponse {
        private boolean success;
        private String message;
        private SearchData data;

        public SearchResponse(boolean success, String message, SearchData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SearchData getData() { return data; }
        public void setData(SearchData data) { this.data = data; }
    }

    public static class SearchData {
        private List<SearchDocumentDTO> documents;
        private SearchPaginationDTO pagination;

        public SearchData(List<SearchDocumentDTO> documents, SearchPaginationDTO pagination) {
            this.documents = documents;
            this.pagination = pagination;
        }

        public List<SearchDocumentDTO> getDocuments() { return documents; }
        public void setDocuments(List<SearchDocumentDTO> documents) { this.documents = documents; }

        public SearchPaginationDTO getPagination() { return pagination; }
        public void setPagination(SearchPaginationDTO pagination) { this.pagination = pagination; }
    }

    public static class SearchDocumentDTO {
        private Integer id;
        private String title;
        private String description;
        private String fileName;
        private String fileSize;
        private String fileType;
        private String language;
        private Integer pageCount;
        private Integer readProgress;
        private List<String> tags;
        private Double relevance;
        private Map<String, List<String>> highlights;
        private String createdAt;
        private String thumbnail;

        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

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

        public Integer getReadProgress() { return readProgress; }
        public void setReadProgress(Integer readProgress) { this.readProgress = readProgress; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public Double getRelevance() { return relevance; }
        public void setRelevance(Double relevance) { this.relevance = relevance; }

        public Map<String, List<String>> getHighlights() { return highlights; }
        public void setHighlights(Map<String, List<String>> highlights) { this.highlights = highlights; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getThumbnail() { return thumbnail; }
        public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
    }

    public static class SearchPaginationDTO {
        private Integer page;
        private Integer pageSize;
        private Integer total;
        private Integer totalPages;

        public SearchPaginationDTO(Integer page, Integer pageSize, Integer total) {
            this.page = page;
            this.pageSize = pageSize;
            this.total = total;
            this.totalPages = (int) Math.ceil((double) total / pageSize);
        }

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }

        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }

        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchDocuments(
            @RequestParam("query") String query,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 构建请求参数
            SearchRequest request = new SearchRequest();
            request.setQuery(query);
            request.setPage(page);
            request.setPageSize(pageSize);
            request.setSortBy(sortBy);

            Map<String, Object> params = new HashMap<>();
            params.put("query", request.getQuery());
            params.put("page", request.getPage());
            params.put("pageSize", request.getPageSize());
            params.put("sortBy", request.getSortBy());

            // 打印接收到的请求
            printRequest(query, params);

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SearchResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SearchResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 2. 验证搜索关键词
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SearchResponse(false, "搜索关键词不能为空", null)
                );
            }

            // 3. 构建搜索条件
            String searchTerm = "%" + request.getQuery().trim() + "%";

            // 基础查询
            StringBuilder whereClause = new StringBuilder("WHERE d.user_id = ? AND d.deleted_at IS NULL ");
            whereClause.append("AND (d.title LIKE ? OR d.description LIKE ? OR d.file_name LIKE ?)");

            List<Object> queryParams = new ArrayList<>();
            queryParams.add(userId);
            queryParams.add(searchTerm);
            queryParams.add(searchTerm);
            queryParams.add(searchTerm);

            // 4. 获取总数
            String countSql = "SELECT COUNT(*) as total FROM documents d " + whereClause;
            Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, queryParams.toArray());

            // 5. 构建排序
            String orderClause;
            if ("relevance".equals(request.getSortBy())) {
                // 简单相关性排序：标题匹配 > 描述匹配 > 文件名匹配
                orderClause = "ORDER BY " +
                        "CASE WHEN d.title LIKE ? THEN 1 " +
                        "WHEN d.description LIKE ? THEN 2 " +
                        "WHEN d.file_name LIKE ? THEN 3 " +
                        "ELSE 4 END, " +
                        "d.created_at DESC";
            } else {
                orderClause = "ORDER BY d.created_at DESC";
            }

            // 6. 构建分页
            int offset = (request.getPage() - 1) * request.getPageSize();
            String limitClause = "LIMIT ? OFFSET ?";

            // 7. 查询文档列表
            String querySql = "SELECT d.document_id, d.title, d.description, d.file_name, d.file_size, " +
                    "d.file_type, d.language, d.page_count, d.reading_progress, d.created_at, " +
                    "u.username as uploader " +
                    "FROM documents d " +
                    "LEFT JOIN users u ON d.user_id = u.user_id " +
                    whereClause + " " + orderClause + " " + limitClause;

            List<Object> finalQueryParams = new ArrayList<>(queryParams);

            if ("relevance".equals(request.getSortBy())) {
                finalQueryParams.add(searchTerm);
                finalQueryParams.add(searchTerm);
                finalQueryParams.add(searchTerm);
            }

            finalQueryParams.add(request.getPageSize());
            finalQueryParams.add(offset);

            List<Map<String, Object>> documents = jdbcTemplate.queryForList(querySql, finalQueryParams.toArray());
            printQueryResult(documents, total);

            // 8. 构建响应数据
            List<SearchDocumentDTO> documentDTOs = new ArrayList<>();

            for (Map<String, Object> doc : documents) {
                Integer docId = (Integer) doc.get("document_id");

                // 查询文档标签
                String tagSql = "SELECT dt.tag_name FROM document_tag_relations dtr " +
                        "JOIN document_tags dt ON dtr.tag_id = dt.tag_id " +
                        "WHERE dtr.document_id = ?";
                List<Map<String, Object>> tagResults = jdbcTemplate.queryForList(tagSql, docId);

                List<String> tags = tagResults.stream()
                        .map(tag -> (String) tag.get("tag_name"))
                        .collect(Collectors.toList());

                // 构建高亮信息
                Map<String, List<String>> highlights = new HashMap<>();
                String title = (String) doc.get("title");
                String description = (String) doc.get("description");

                // 简单高亮逻辑：如果包含搜索词，就标记
                if (title != null && title.toLowerCase().contains(request.getQuery().toLowerCase())) {
                    highlights.put("title", Collections.singletonList(title));
                }

                if (description != null && description.toLowerCase().contains(request.getQuery().toLowerCase())) {
                    // 截取包含搜索词的部分
                    int index = description.toLowerCase().indexOf(request.getQuery().toLowerCase());
                    int start = Math.max(0, index - 50);
                    int end = Math.min(description.length(), index + request.getQuery().length() + 50);
                    String snippet = description.substring(start, end);
                    if (start > 0) snippet = "..." + snippet;
                    if (end < description.length()) snippet = snippet + "...";
                    highlights.put("description", Collections.singletonList(snippet));
                }

                // 计算相关性分数（简单实现）
                double relevance = 0.0;
                if (title != null && title.toLowerCase().contains(request.getQuery().toLowerCase())) {
                    relevance += 1.0;
                }
                if (description != null && description.toLowerCase().contains(request.getQuery().toLowerCase())) {
                    relevance += 0.5;
                }

                // 构建DTO
                SearchDocumentDTO dto = new SearchDocumentDTO();
                dto.setId(docId);
                dto.setTitle(title);
                dto.setDescription(description);
                dto.setFileName((String) doc.get("file_name"));

                // 格式化文件大小
                Long fileSize = (Long) doc.get("file_size");
                dto.setFileSize(formatFileSize(fileSize));

                dto.setFileType((String) doc.get("file_type"));
                dto.setLanguage((String) doc.get("language"));
                dto.setPageCount((Integer) doc.get("page_count"));
                dto.setReadProgress((Integer) doc.get("reading_progress"));
                dto.setTags(tags);
                dto.setRelevance(relevance);
                dto.setHighlights(highlights);
                dto.setCreatedAt(formatDate((java.sql.Timestamp) doc.get("created_at")));
                dto.setThumbnail(null); // 暂时没有缩略图

                documentDTOs.add(dto);
            }

            // 9. 构建分页信息
            SearchPaginationDTO pagination = new SearchPaginationDTO(
                    request.getPage(),
                    request.getPageSize(),
                    total
            );

            SearchData data = new SearchData(documentDTOs, pagination);
            SearchResponse response = new SearchResponse(true, "搜索文档成功", data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("搜索文档过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SearchResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 格式化文件大小
    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }

    // 格式化日期
    private String formatDate(java.sql.Timestamp timestamp) {
        if (timestamp == null) return "";
        return timestamp.toLocalDateTime().toString();
    }
}