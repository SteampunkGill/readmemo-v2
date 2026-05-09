package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchHighlights {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到高亮搜索请求 ===");
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
    public static class HighlightSearchRequest {
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
    public static class HighlightSearchResponse {
        private boolean success;
        private String message;
        private HighlightSearchData data;

        public HighlightSearchResponse(boolean success, String message, HighlightSearchData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public HighlightSearchData getData() { return data; }
        public void setData(HighlightSearchData data) { this.data = data; }
    }

    public static class HighlightSearchData {
        private int total;
        private int page;
        private int pageSize;
        private int totalPages;
        private List<HighlightItem> items;
        private Map<String, Object> facets;
        private long queryTime;

        public HighlightSearchData(int total, int page, int pageSize, int totalPages,
                                   List<HighlightItem> items, Map<String, Object> facets,
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

        public List<HighlightItem> getItems() { return items; }
        public void setItems(List<HighlightItem> items) { this.items = items; }

        public Map<String, Object> getFacets() { return facets; }
        public void setFacets(Map<String, Object> facets) { this.facets = facets; }

        public long getQueryTime() { return queryTime; }
        public void setQueryTime(long queryTime) { this.queryTime = queryTime; }
    }

    public static class HighlightItem {
        private String id;
        private String title;
        private String selectedText;
        private String note;
        private String excerpt;
        private String documentTitle;
        private Long documentId;
        private double relevance;
        private Map<String, Object> highlight;
        private String createdAt;
        private String url;
        private int pageNumber;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getSelectedText() { return selectedText; }
        public void setSelectedText(String selectedText) { this.selectedText = selectedText; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }

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

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public int getPageNumber() { return pageNumber; }
        public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
    }

    @GetMapping("/highlights")
    public ResponseEntity<HighlightSearchResponse> searchHighlights(
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
        HighlightSearchRequest request = new HighlightSearchRequest();
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
                        new HighlightSearchResponse(false, "搜索关键词不能为空", null)
                );
            }

            // 2. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new HighlightSearchResponse(false, "请先登录", null)
                );
            }

            // 3. 构建SQL查询
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT h.highlight_id, h.selected_text, h.note, h.created_at, ");
            sqlBuilder.append("h.page_number, d.document_id, d.title as document_title ");
            sqlBuilder.append("FROM document_highlights h ");
            sqlBuilder.append("INNER JOIN documents d ON h.document_id = d.document_id ");
            sqlBuilder.append("WHERE (h.selected_text LIKE ? OR h.note LIKE ?) AND h.user_id = ? ");

            List<Object> params = new ArrayList<>();
            params.add("%" + query + "%");
            params.add("%" + query + "%");
            params.add(userId);

            // 添加文档过滤
            if (documentId != null && !documentId.isEmpty()) {
                sqlBuilder.append("AND h.document_id = ? ");
                params.add(Long.parseLong(documentId));
            }

            // 添加日期过滤
            if (dateFrom != null && !dateFrom.isEmpty()) {
                sqlBuilder.append("AND DATE(h.created_at) >= ? ");
                params.add(dateFrom);
            }

            if (dateTo != null && !dateTo.isEmpty()) {
                sqlBuilder.append("AND DATE(h.created_at) <= ? ");
                params.add(dateTo);
            }

            // 4. 获取总数
            String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder.toString() + ") AS count_table";
            int total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

            // 5. 添加排序和分页
            if ("relevance".equals(sortBy)) {
                // 相关性排序：选中的文本匹配优先
                sqlBuilder.append("ORDER BY CASE WHEN h.selected_text LIKE ? THEN 1 ");
                sqlBuilder.append("WHEN h.note LIKE ? THEN 2 ");
                sqlBuilder.append("ELSE 3 END, ");
                sqlBuilder.append("h.created_at DESC ");
                params.add("%" + query + "%");
                params.add("%" + query + "%");
            } else if ("createdAt".equals(sortBy)) {
                sqlBuilder.append("ORDER BY h.created_at ");
                sqlBuilder.append("desc".equalsIgnoreCase(sortOrder) ? "DESC " : "ASC ");
            } else if ("pageNumber".equals(sortBy)) {
                sqlBuilder.append("ORDER BY h.page_number ");
                sqlBuilder.append("desc".equalsIgnoreCase(sortOrder) ? "DESC " : "ASC ");
            } else {
                sqlBuilder.append("ORDER BY h.created_at DESC ");
            }

            sqlBuilder.append("LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            // 6. 执行查询
            List<Map<String, Object>> highlights = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
            printQueryResult("找到 " + total + " 个高亮，返回 " + highlights.size() + " 个");

            // 7. 处理结果
            List<HighlightItem> items = new ArrayList<>();
            for (Map<String, Object> highlight : highlights) {
                HighlightItem item = new HighlightItem();
                Long highlightId = ((Number) highlight.get("highlight_id")).longValue();
                Long docId = ((Number) highlight.get("document_id")).longValue();

                item.setId("highlight_" + highlightId);
                item.setSelectedText((String) highlight.get("selected_text"));
                item.setNote((String) highlight.get("note"));
                item.setDocumentTitle((String) highlight.get("document_title"));
                item.setDocumentId(docId);
                item.setPageNumber(highlight.get("page_number") != null ?
                        ((Number) highlight.get("page_number")).intValue() : 0);

                // 生成标题和摘要
                String selectedText = (String) highlight.get("selected_text");
                String noteText = (String) highlight.get("note");

                if (selectedText != null) {
                    // 取选中的文本作为标题
                    if (selectedText.length() > 60) {
                        item.setTitle(selectedText.substring(0, 60) + "...");
                    } else {
                        item.setTitle(selectedText);
                    }

                    // 生成摘要：优先使用选中的文本，如果没有则使用笔记
                    String excerptSource = selectedText;
                    if (excerptSource == null || excerptSource.isEmpty()) {
                        excerptSource = noteText;
                    }

                    if (excerptSource != null && excerptSource.length() > 120) {
                        item.setExcerpt(excerptSource.substring(0, 120) + "...");
                    } else {
                        item.setExcerpt(excerptSource);
                    }
                }

                // 计算相关性
                double relevance = calculateRelevance(query, selectedText, noteText);
                item.setRelevance(relevance);

                // 构建高亮信息
                Map<String, Object> hl = new HashMap<>();
                hl.put("selectedText", highlightText(query, selectedText));
                hl.put("note", highlightText(query, noteText));
                item.setHighlight(hl);

                item.setCreatedAt(highlight.get("created_at") != null ?
                        highlight.get("created_at").toString() : null);
                item.setUrl("/reader/highlight/" + highlightId + "?document=" + docId);

                items.add(item);
            }

            // 8. 构建facet数据
            Map<String, Object> facets = buildHighlightFacets(query, userId, filters);

            // 9. 记录搜索历史
            saveSearchHistory(userId, query, "highlights", total);

            // 10. 计算查询时间
            long queryTime = System.currentTimeMillis() - startTime;

            // 11. 计算总页数
            int totalPages = (int) Math.ceil((double) total / pageSize);

            // 12. 准备响应数据
            HighlightSearchData searchData = new HighlightSearchData(
                    total, page, pageSize, totalPages, items, facets, queryTime
            );

            HighlightSearchResponse response = new HighlightSearchResponse(true, "高亮搜索成功", searchData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("高亮搜索过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new HighlightSearchResponse(false, "服务器内部错误: " + e.getMessage(), null)
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
    private double calculateRelevance(String query, String selectedText, String note) {
        if (selectedText == null) selectedText = "";
        if (note == null) note = "";

        String lowerQuery = query.toLowerCase();
        String lowerSelectedText = selectedText.toLowerCase();
        String lowerNote = note.toLowerCase();

        double score = 0.0;

        // 选中的文本匹配权重更高
        if (lowerSelectedText.contains(lowerQuery)) {
            score += 0.7;
        }

        // 笔记匹配
        if (lowerNote.contains(lowerQuery)) {
            score += 0.3;
        }

        // 精确匹配加分
        if (selectedText.contains(query) || note.contains(query)) {
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

    // 构建高亮facet数据
    private Map<String, Object> buildHighlightFacets(String query, Long userId, Map<String, Object> filters) {
        Map<String, Object> facets = new HashMap<>();

        try {
            // 文档facet
            String documentSql = "SELECT d.document_id, d.title, COUNT(*) as count " +
                    "FROM document_highlights h " +
                    "INNER JOIN documents d ON h.document_id = d.document_id " +
                    "WHERE (h.selected_text LIKE ? OR h.note LIKE ?) AND h.user_id = ? " +
                    "GROUP BY d.document_id, d.title ORDER BY count DESC LIMIT 10";

            List<Map<String, Object>> documentFacet = jdbcTemplate.queryForList(documentSql,
                    "%" + query + "%", "%" + query + "%", userId);
            facets.put("documents", documentFacet);

            // 日期facet（按月统计）
            String dateSql = "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COUNT(*) as count " +
                    "FROM document_highlights " +
                    "WHERE (selected_text LIKE ? OR note LIKE ?) AND user_id = ? " +
                    "GROUP BY DATE_FORMAT(created_at, '%Y-%m') " +
                    "ORDER BY month DESC LIMIT 12";

            List<Map<String, Object>> dateFacet = jdbcTemplate.queryForList(dateSql,
                    "%" + query + "%", "%" + query + "%", userId);
            facets.put("months", dateFacet);

        } catch (Exception e) {
            System.err.println("构建高亮facet失败: " + e.getMessage());
        }

        return facets;
    }

    // 保存搜索历史
    private void saveSearchHistory(Long userId, String keyword, String searchType, int resultCount) {
        try {
            String sql = "INSERT INTO search_history (user_id, keyword, search_type, result_count, timestamp) " +
                    "VALUES (?, ?, ?, ?, NOW())";
            jdbcTemplate.update(sql, userId, keyword, searchType, resultCount);
            System.out.println("已保存高亮搜索历史: 用户=" + userId + ", 关键词=" + keyword);
        } catch (Exception e) {
            System.err.println("保存搜索历史失败: " + e.getMessage());
        }
    }
}
