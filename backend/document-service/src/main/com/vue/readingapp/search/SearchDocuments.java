package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/search")
public class SearchDocuments {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到文档搜索请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=====================");
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
    public static class DocumentSearchRequest {
        private String query;
        private Integer page = 1;
        private Integer pageSize = 20;
        private String sortBy = "relevance";
        private String sortOrder = "desc";
        private Map<String, Object> filters = new HashMap<>();

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }

        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }

        public String getSortOrder() { return sortOrder; }
        public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }

        public Map<String, Object> getFilters() { return filters; }
        public void setFilters(Map<String, Object> filters) { this.filters = filters; }
    }

    // 响应DTO
    public static class DocumentSearchResponse {
        private boolean success;
        private String message;
        private DocumentSearchData data;

        public DocumentSearchResponse(boolean success, String message, DocumentSearchData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public DocumentSearchData getData() { return data; }
        public void setData(DocumentSearchData data) { this.data = data; }
    }

    public static class DocumentSearchData {
        private int total;
        private int page;
        private int pageSize;
        private int totalPages;
        private List<DocumentItem> items;
        private Map<String, Object> facets;
        private long queryTime;

        public DocumentSearchData(int total, int page, int pageSize, int totalPages,
                                  List<DocumentItem> items, Map<String, Object> facets,
                                  long queryTime) {
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
            this.totalPages = totalPages;
            this.items = items;
            this.facets = facets;
            this.queryTime = queryTime;
        }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

        public List<DocumentItem> getItems() { return items; }
        public void setItems(List<DocumentItem> items) { this.items = items; }

        public Map<String, Object> getFacets() { return facets; }
        public void setFacets(Map<String, Object> facets) { this.facets = facets; }

        public long getQueryTime() { return queryTime; }
        public void setQueryTime(long queryTime) { this.queryTime = queryTime; }
    }

    public static class DocumentItem {
        private String id;
        private String title;
        private String content;
        private String excerpt;
        private String author;
        private List<String> tags;
        private String language;
        private int wordCount;
        private String difficulty;
        private double relevance;
        private Map<String, Object> highlight;
        private String createdAt;
        private String updatedAt;
        private String thumbnail;
        private String url;
        private String status;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getExcerpt() { return excerpt; }
        public void setExcerpt(String excerpt) { this.excerpt = excerpt; }

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public int getWordCount() { return wordCount; }
        public void setWordCount(int wordCount) { this.wordCount = wordCount; }

        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

        public double getRelevance() { return relevance; }
        public void setRelevance(double relevance) { this.relevance = relevance; }

        public Map<String, Object> getHighlight() { return highlight; }
        public void setHighlight(Map<String, Object> highlight) { this.highlight = highlight; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public String getThumbnail() { return thumbnail; }
        public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    @GetMapping("/documents")
    public ResponseEntity<DocumentSearchResponse> searchDocuments(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false, defaultValue = "relevance") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Integer minWordCount,
            @RequestParam(required = false) Integer maxWordCount,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        long startTime = System.currentTimeMillis();

        // 构建请求对象
        DocumentSearchRequest request = new DocumentSearchRequest();
        request.setQuery(query);
        request.setPage(page);
        request.setPageSize(pageSize);
        request.setSortBy(sortBy);
        request.setSortOrder(sortOrder);

        Map<String, Object> filters = new HashMap<>();
        if (language != null) filters.put("language", language);
        if (difficulty != null) filters.put("difficulty", difficulty);
        if (tag != null) filters.put("tag", tag);
        if (minWordCount != null) filters.put("minWordCount", minWordCount);
        if (maxWordCount != null) filters.put("maxWordCount", maxWordCount);
        if (dateFrom != null) filters.put("dateFrom", dateFrom);
        if (dateTo != null) filters.put("dateTo", dateTo);
        request.setFilters(filters);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证请求数据
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new DocumentSearchResponse(false, "搜索关键词不能为空", null)
                );
            }

            // 2. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);

            // 3. 构建SQL查询
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT d.document_id, d.title, d.content, d.author, d.language, ");
            sqlBuilder.append("d.word_count, d.difficulty, d.created_at, d.updated_at, ");
            sqlBuilder.append("d.thumbnail_url, d.status, d.user_id ");
            sqlBuilder.append("FROM documents d ");
            sqlBuilder.append("WHERE (d.title LIKE ? OR d.content LIKE ?) ");
            sqlBuilder.append("AND d.status = 'processed' ");

            List<Object> params = new ArrayList<>();
            params.add("%" + query + "%");
            params.add("%" + query + "%");

            // 添加用户过滤（只能搜索自己的文档）
            if (userId != null) {
                sqlBuilder.append("AND d.user_id = ? ");
                params.add(userId);
            }

            // 添加语言过滤
            if (language != null && !language.isEmpty()) {
                sqlBuilder.append("AND d.language = ? ");
                params.add(language);
            }

            // 添加难度过滤
            if (difficulty != null && !difficulty.isEmpty()) {
                sqlBuilder.append("AND d.difficulty = ? ");
                params.add(difficulty);
            }

            // 添加标签过滤
            if (tag != null && !tag.isEmpty()) {
                sqlBuilder.append("AND EXISTS (SELECT 1 FROM document_tag_relations r ");
                sqlBuilder.append("INNER JOIN document_tags t ON r.tag_id = t.tag_id ");
                sqlBuilder.append("WHERE r.document_id = d.document_id AND t.tag_name = ?) ");
                params.add(tag);
            }

            // 添加单词数过滤
            if (minWordCount != null) {
                sqlBuilder.append("AND d.word_count >= ? ");
                params.add(minWordCount);
            }

            if (maxWordCount != null) {
                sqlBuilder.append("AND d.word_count <= ? ");
                params.add(maxWordCount);
            }

            // 添加日期过滤
            if (dateFrom != null && !dateFrom.isEmpty()) {
                sqlBuilder.append("AND DATE(d.created_at) >= ? ");
                params.add(dateFrom);
            }

            if (dateTo != null && !dateTo.isEmpty()) {
                sqlBuilder.append("AND DATE(d.created_at) <= ? ");
                params.add(dateTo);
            }

            // 4. 获取总数
            String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder.toString() + ") AS count_table";
            int total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

            // 5. 添加排序和分页
            if ("relevance".equals(sortBy)) {
                // 简单相关性排序：标题匹配优先
                sqlBuilder.append("ORDER BY CASE WHEN d.title LIKE ? THEN 1 ELSE 0 END DESC, ");
                sqlBuilder.append("d.created_at DESC ");
                params.add("%" + query + "%");
            } else if ("createdAt".equals(sortBy)) {
                sqlBuilder.append("ORDER BY d.created_at ");
                sqlBuilder.append("desc".equalsIgnoreCase(sortOrder) ? "DESC " : "ASC ");
            } else if ("wordCount".equals(sortBy)) {
                sqlBuilder.append("ORDER BY d.word_count ");
                sqlBuilder.append("desc".equalsIgnoreCase(sortOrder) ? "DESC " : "ASC ");
            } else {
                sqlBuilder.append("ORDER BY d.created_at DESC ");
            }

            sqlBuilder.append("LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            // 6. 执行查询
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
            printQueryResult("找到 " + total + " 个文档，返回 " + documents.size() + " 个");

            // 7. 处理结果
            List<DocumentItem> items = new ArrayList<>();
            for (Map<String, Object> doc : documents) {
                DocumentItem item = new DocumentItem();
                item.setId("doc_" + doc.get("document_id"));
                item.setTitle((String) doc.get("title"));
                item.setContent((String) doc.get("content"));

                // 生成摘要
                String content = (String) doc.get("content");
                if (content != null && content.length() > 200) {
                    item.setExcerpt(content.substring(0, 200) + "...");
                } else {
                    item.setExcerpt(content);
                }

                item.setAuthor((String) doc.get("author"));
                item.setLanguage((String) doc.get("language"));
                item.setWordCount(doc.get("word_count") != null ? ((Number) doc.get("word_count")).intValue() : 0);
                item.setDifficulty((String) doc.get("difficulty"));
                item.setStatus((String) doc.get("status"));

                // 计算相关性
                double relevance = calculateRelevance(query, (String) doc.get("title"), content);
                item.setRelevance(relevance);

                // 构建高亮信息
                Map<String, Object> highlight = new HashMap<>();
                highlight.put("title", highlightText(query, (String) doc.get("title")));
                highlight.put("content", highlightText(query, content));
                item.setHighlight(highlight);

                item.setCreatedAt(doc.get("created_at") != null ? doc.get("created_at").toString() : null);
                item.setUpdatedAt(doc.get("updated_at") != null ? doc.get("updated_at").toString() : null);
                item.setThumbnail((String) doc.get("thumbnail_url"));
                item.setUrl("/documents/" + doc.get("document_id"));

                // 获取标签
                List<String> tags = getDocumentTags(((Number) doc.get("document_id")).longValue());
                item.setTags(tags);

                items.add(item);
            }

            // 8. 构建facet数据
            Map<String, Object> facets = buildDocumentFacets(query, userId, filters);

            // 9. 记录搜索历史
            if (userId != null) {
                saveSearchHistory(userId, query, "documents", total);
            }

            // 10. 计算查询时间
            long queryTime = System.currentTimeMillis() - startTime;

            // 11. 计算总页数
            int totalPages = (int) Math.ceil((double) total / pageSize);

            // 12. 准备响应数据
            DocumentSearchData searchData = new DocumentSearchData(
                    total, page, pageSize, totalPages, items, facets, queryTime
            );

            DocumentSearchResponse response = new DocumentSearchResponse(true, "文档搜索成功", searchData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("文档搜索过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DocumentSearchResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 从token获取用户ID
    private Long getUserIdFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String sql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
                List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sql, token);
                if (!sessions.isEmpty()) {
                    return ((Number) sessions.get(0).get("user_id")).longValue();
                }
            } catch (Exception e) {
                System.out.println("Token解析失败: " + e.getMessage());
            }
        }
        return null;
    }

    // 计算相关性
    private double calculateRelevance(String query, String title, String content) {
        if (title == null) title = "";
        if (content == null) content = "";

        String lowerQuery = query.toLowerCase();
        String lowerTitle = title.toLowerCase();
        String lowerContent = content.toLowerCase();

        double score = 0.0;

        // 标题匹配权重更高
        if (lowerTitle.contains(lowerQuery)) {
            score += 0.7;
        }

        // 内容匹配
        if (lowerContent.contains(lowerQuery)) {
            score += 0.3;
        }

        // 精确匹配加分
        if (title.contains(query) || content.contains(query)) {
            score += 0.2;
        }

        return Math.min(score, 1.0);
    }

    // 高亮文本
    private String highlightText(String query, String text) {
        if (text == null || query == null || query.isEmpty()) {
            return text;
        }

        return text.replaceAll("(?i)(" + query + ")", "<mark>$1</mark>");
    }

    // 获取文档标签
    private List<String> getDocumentTags(Long documentId) {
        List<String> tags = new ArrayList<>();
        try {
            String sql = "SELECT t.tag_name FROM document_tags t " +
                    "INNER JOIN document_tag_relations r ON t.tag_id = r.tag_id " +
                    "WHERE r.document_id = ?";
            List<Map<String, Object>> tagList = jdbcTemplate.queryForList(sql, documentId);
            for (Map<String, Object> tag : tagList) {
                tags.add((String) tag.get("tag_name"));
            }
        } catch (Exception e) {
            System.err.println("获取文档标签失败: " + e.getMessage());
        }
        return tags;
    }

    // 构建文档facet数据
    private Map<String, Object> buildDocumentFacets(String query, Long userId, Map<String, Object> filters) {
        Map<String, Object> facets = new HashMap<>();

        try {
            // 语言facet
            String languageSql = "SELECT d.language, COUNT(*) as count FROM documents d " +
                    "WHERE (d.title LIKE ? OR d.content LIKE ?) AND d.status = 'processed' ";

            List<Object> params = new ArrayList<>();
            params.add("%" + query + "%");
            params.add("%" + query + "%");

            if (userId != null) {
                languageSql += "AND d.user_id = ? ";
                params.add(userId);
            }

            languageSql += "GROUP BY d.language ORDER BY count DESC";

            List<Map<String, Object>> languageFacet = jdbcTemplate.queryForList(languageSql, params.toArray());
            facets.put("languages", languageFacet);

            // 难度facet
            String difficultySql = "SELECT d.difficulty, COUNT(*) as count FROM documents d " +
                    "WHERE (d.title LIKE ? OR d.content LIKE ?) AND d.status = 'processed' ";

            List<Object> difficultyParams = new ArrayList<>();
            difficultyParams.add("%" + query + "%");
            difficultyParams.add("%" + query + "%");

            if (userId != null) {
                difficultySql += "AND d.user_id = ? ";
                difficultyParams.add(userId);
            }

            difficultySql += "GROUP BY d.difficulty ORDER BY count DESC";

            List<Map<String, Object>> difficultyFacet = jdbcTemplate.queryForList(difficultySql, difficultyParams.toArray());
            facets.put("difficulties", difficultyFacet);

            // 标签facet
            String tagSql = "SELECT t.tag_name, COUNT(*) as count FROM document_tags t " +
                    "INNER JOIN document_tag_relations r ON t.tag_id = r.tag_id " +
                    "INNER JOIN documents d ON r.document_id = d.document_id " +
                    "WHERE (d.title LIKE ? OR d.content LIKE ?) AND d.status = 'processed' ";

            List<Object> tagParams = new ArrayList<>();
            tagParams.add("%" + query + "%");
            tagParams.add("%" + query + "%");

            if (userId != null) {
                tagSql += "AND d.user_id = ? ";
                tagParams.add(userId);
            }

            tagSql += "GROUP BY t.tag_id, t.tag_name ORDER BY count DESC LIMIT 20";

            List<Map<String, Object>> tagFacet = jdbcTemplate.queryForList(tagSql, tagParams.toArray());
            facets.put("tags", tagFacet);

        } catch (Exception e) {
            System.err.println("构建文档facet失败: " + e.getMessage());
        }

        return facets;
    }

    // 保存搜索历史
    private void saveSearchHistory(Long userId, String keyword, String searchType, int resultCount) {
        try {
            String sql = "INSERT INTO search_history (user_id, keyword, search_type, result_count, timestamp) " +
                    "VALUES (?, ?, ?, ?, NOW())";
            jdbcTemplate.update(sql, userId, keyword, searchType, resultCount);
            System.out.println("已保存文档搜索历史: 用户=" + userId + ", 关键词=" + keyword);
        } catch (Exception e) {
            System.err.println("保存搜索历史失败: " + e.getMessage());
        }
    }
}
