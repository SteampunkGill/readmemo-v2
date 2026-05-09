
package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchNotes {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到笔记搜索请求 ===");
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
    public static class NoteSearchRequest {
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
    public static class NoteSearchResponse {
        private boolean success;
        private String message;
        private NoteSearchData data;

        public NoteSearchResponse(boolean success, String message, NoteSearchData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public NoteSearchData getData() { return data; }
        public void setData(NoteSearchData data) { this.data = data; }
    }

    public static class NoteSearchData {
        private int total;
        private int page;
        private int pageSize;
        private int totalPages;
        private List<NoteItem> items;
        private Map<String, Object> facets;
        private long queryTime;

        public NoteSearchData(int total, int page, int pageSize, int totalPages,
                              List<NoteItem> items, Map<String, Object> facets,
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

        public List<NoteItem> getItems() { return items; }
        public void setItems(List<NoteItem> items) { this.items = items; }

        public Map<String, Object> getFacets() { return facets; }
        public void setFacets(Map<String, Object> facets) { this.facets = facets; }

        public long getQueryTime() { return queryTime; }
        public void setQueryTime(long queryTime) { this.queryTime = queryTime; }
    }

    public static class NoteItem {
        private String id;
        private String title;
        private String content;
        private String excerpt;
        private String documentTitle;
        private Long documentId;
        private double relevance;
        private Map<String, Object> highlight;
        private String createdAt;
        private String updatedAt;
        private String url;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getExcerpt() { return excerpt; }
        public void setExcerpt(String excerpt) { this.excerpt = excerpt; }

        public String getDocumentTitle() { return documentTitle; }
        public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }

        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }

        public double getRelevance() { return relevance; }
        public void setRelevance(double relevance) { this.relevance = relevance; }

        public Map<String, Object> getHighlight() { return highlight; }
        public void setHighlight(Map<String, Object> highlight) { this.highlight = highlight; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    @GetMapping("/notes")
    public ResponseEntity<NoteSearchResponse> searchNotes(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false, defaultValue = "relevance") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String documentId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        long startTime = System.currentTimeMillis();

        // 构建请求对象
        NoteSearchRequest request = new NoteSearchRequest();
        request.setQuery(query);
        request.setPage(page);
        request.setPageSize(pageSize);
        request.setSortBy(sortBy);
        request.setSortOrder(sortOrder);

        Map<String, Object> filters = new HashMap<>();
        if (documentId != null) filters.put("documentId", documentId);
        if (dateFrom != null) filters.put("dateFrom", dateFrom);
        if (dateTo != null) filters.put("dateTo", dateTo);
        request.setFilters(filters);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证请求数据
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new NoteSearchResponse(false, "搜索关键词不能为空", null)
                );
            }

            // 2. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new NoteSearchResponse(false, "请先登录", null)
                );
            }

            // 3. 构建SQL查询
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT n.note_id, n.content, n.created_at, n.updated_at, ");
            sqlBuilder.append("d.document_id, d.title as document_title ");
            sqlBuilder.append("FROM document_notes n ");
            sqlBuilder.append("INNER JOIN documents d ON n.document_id = d.document_id ");
            sqlBuilder.append("WHERE n.content LIKE ? AND n.user_id = ? ");

            List<Object> params = new ArrayList<>();
            params.add("%" + query + "%");
            params.add(userId);

            // 添加文档过滤
            if (documentId != null && !documentId.isEmpty()) {
                sqlBuilder.append("AND n.document_id = ? ");
                params.add(Long.parseLong(documentId));
            }

            // 添加日期过滤
            if (dateFrom != null && !dateFrom.isEmpty()) {
                sqlBuilder.append("AND DATE(n.created_at) >= ? ");
                params.add(dateFrom);
            }

            if (dateTo != null && !dateTo.isEmpty()) {
                sqlBuilder.append("AND DATE(n.created_at) <= ? ");
                params.add(dateTo);
            }

            // 4. 获取总数
            String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder.toString() + ") AS count_table";
            int total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

            // 5. 添加排序和分页
            if ("relevance".equals(sortBy)) {
                // 相关性排序：内容匹配程度
                sqlBuilder.append("ORDER BY CASE WHEN n.content LIKE ? THEN 1 ELSE 0 END DESC, ");
                sqlBuilder.append("n.created_at DESC ");
                params.add("%" + query + "%");
            } else if ("createdAt".equals(sortBy)) {
                sqlBuilder.append("ORDER BY n.created_at ");
                sqlBuilder.append("desc".equalsIgnoreCase(sortOrder) ? "DESC " : "ASC ");
            } else if ("updatedAt".equals(sortBy)) {
                sqlBuilder.append("ORDER BY n.updated_at ");
                sqlBuilder.append("desc".equalsIgnoreCase(sortOrder) ? "DESC " : "ASC ");
            } else {
                sqlBuilder.append("ORDER BY n.created_at DESC ");
            }

            sqlBuilder.append("LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            // 6. 执行查询
            List<Map<String, Object>> notes = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
            printQueryResult("找到 " + total + " 条笔记，返回 " + notes.size() + " 条");

            // 7. 处理结果
            List<NoteItem> items = new ArrayList<>();
            for (Map<String, Object> note : notes) {
                NoteItem item = new NoteItem();
                Long noteId = ((Number) note.get("note_id")).longValue();
                Long docId = ((Number) note.get("document_id")).longValue();

                item.setId("note_" + noteId);
                item.setContent((String) note.get("content"));
                item.setDocumentTitle((String) note.get("document_title"));
                item.setDocumentId(docId);

                // 生成标题和摘要
                String content = (String) note.get("content");
                if (content != null) {
                    // 取前50个字符作为标题
                    if (content.length() > 50) {
                        item.setTitle(content.substring(0, 50) + "...");
                    } else {
                        item.setTitle(content);
                    }

                    // 取前150个字符作为摘要
                    if (content.length() > 150) {
                        item.setExcerpt(content.substring(0, 150) + "...");
                    } else {
                        item.setExcerpt(content);
                    }
                }

                // 计算相关性
                double relevance = calculateRelevance(query, content);
                item.setRelevance(relevance);

                // 构建高亮信息
                Map<String, Object> highlight = new HashMap<>();
                highlight.put("content", highlightText(query, content));
                item.setHighlight(highlight);

                item.setCreatedAt(note.get("created_at") != null ? note.get("created_at").toString() : null);
                item.setUpdatedAt(note.get("updated_at") != null ? note.get("updated_at").toString() : null);
                item.setUrl("/reader/note/" + noteId + "?document=" + docId);

                items.add(item);
            }

            // 8. 构建facet数据
            Map<String, Object> facets = buildNoteFacets(query, userId, filters);

            // 9. 记录搜索历史
            saveSearchHistory(userId, query, "notes", total);

            // 10. 计算查询时间
            long queryTime = System.currentTimeMillis() - startTime;

            // 11. 计算总页数
            int totalPages = (int) Math.ceil((double) total / pageSize);

            // 12. 准备响应数据
            NoteSearchData searchData = new NoteSearchData(
                    total, page, pageSize, totalPages, items, facets, queryTime
            );

            NoteSearchResponse response = new NoteSearchResponse(true, "笔记搜索成功", searchData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("笔记搜索过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new NoteSearchResponse(false, "服务器内部错误: " + e.getMessage(), null)
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
    private double calculateRelevance(String query, String content) {
        if (content == null) content = "";

        String lowerQuery = query.toLowerCase();
        String lowerContent = content.toLowerCase();

        double score = 0.0;

        // 内容匹配
        if (lowerContent.contains(lowerQuery)) {
            score += 1.0;
        }

        // 精确匹配加分
        if (content.contains(query)) {
            score += 0.3;
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

    // 构建笔记facet数据
    private Map<String, Object> buildNoteFacets(String query, Long userId, Map<String, Object> filters) {
        Map<String, Object> facets = new HashMap<>();

        try {
            // 文档facet
            String documentSql = "SELECT d.document_id, d.title, COUNT(*) as count " +
                    "FROM document_notes n " +
                    "INNER JOIN documents d ON n.document_id = d.document_id " +
                    "WHERE n.content LIKE ? AND n.user_id = ? " +
                    "GROUP BY d.document_id, d.title ORDER BY count DESC LIMIT 10";

            List<Map<String, Object>> documentFacet = jdbcTemplate.queryForList(documentSql,
                    "%" + query + "%", userId);
            facets.put("documents", documentFacet);

            // 日期facet（按月统计）
            String dateSql = "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COUNT(*) as count " +
                    "FROM document_notes " +
                    "WHERE content LIKE ? AND user_id = ? " +
                    "GROUP BY DATE_FORMAT(created_at, '%Y-%m') " +
                    "ORDER BY month DESC LIMIT 12";

            List<Map<String, Object>> dateFacet = jdbcTemplate.queryForList(dateSql,
                    "%" + query + "%", userId);
            facets.put("months", dateFacet);

        } catch (Exception e) {
            System.err.println("构建笔记facet失败: " + e.getMessage());
        }

        return facets;
    }

    // 保存搜索历史
    private void saveSearchHistory(Long userId, String keyword, String searchType, int resultCount) {
        try {
            String sql = "INSERT INTO search_history (user_id, keyword, search_type, result_count, timestamp) " +
                    "VALUES (?, ?, ?, ?, NOW())";
            jdbcTemplate.update(sql, userId, keyword, searchType, resultCount);
            System.out.println("已保存笔记搜索历史: 用户=" + userId + ", 关键词=" + keyword);
        } catch (Exception e) {
            System.err.println("保存搜索历史失败: " + e.getMessage());
        }
    }
}