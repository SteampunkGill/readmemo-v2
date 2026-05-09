package com.vue.readingapp.reader;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderSearchDocumentContent {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到搜索文档内容请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=========================");
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
        private String query;
        private int total;
        private List<SearchMatch> matches;

        public SearchData() {
            this.matches = new ArrayList<>();
        }

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public List<SearchMatch> getMatches() { return matches; }
        public void setMatches(List<SearchMatch> matches) { this.matches = matches; }
    }

    public static class SearchMatch {
        private int page;
        private String text;
        private String context;
        private List<String> highlights;

        public SearchMatch() {
            this.highlights = new ArrayList<>();
        }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }

        public List<String> getHighlights() { return highlights; }
        public void setHighlights(List<String> highlights) { this.highlights = highlights; }
    }

    @GetMapping("/documents/{documentId}/search")
    public ResponseEntity<SearchResponse> searchDocumentContent(
            @PathVariable("documentId") int documentId,
            @RequestParam("query") String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "caseSensitive", defaultValue = "false") boolean caseSensitive,
            @RequestParam(value = "wholeWord", defaultValue = "false") boolean wholeWord,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("query", query);
        requestInfo.put("page", page);
        requestInfo.put("pageSize", pageSize);
        requestInfo.put("caseSensitive", caseSensitive);
        requestInfo.put("wholeWord", wholeWord);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SearchResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SearchResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 验证文档权限
            String docSql = "SELECT * FROM documents WHERE document_id = ? AND (user_id = ? OR is_public = true)";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(docSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new SearchResponse(false, "没有权限在此文档搜索", null)
                );
            }

            // 3. 验证查询参数
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SearchResponse(false, "搜索关键词不能为空", null)
                );
            }

            // 4. 获取文档的所有页面
            String pagesSql = "SELECT * FROM document_pages WHERE document_id = ? ORDER BY page_number";
            List<Map<String, Object>> pages = jdbcTemplate.queryForList(pagesSql, documentId);

            // 5. 执行搜索
            List<SearchMatch> matches = new ArrayList<>();
            String searchQuery = query.trim();

            if (!caseSensitive) {
                searchQuery = searchQuery.toLowerCase();
            }

            for (Map<String, Object> pageData : pages) {
                int pageNumber = ((Number) pageData.get("page_number")).intValue();
                String content = (String) pageData.get("content");

                if (content == null) {
                    continue;
                }

                String searchContent = content;
                if (!caseSensitive) {
                    searchContent = content.toLowerCase();
                }

                // 简单的字符串搜索（实际项目中可能需要全文搜索）
                int index = 0;
                while ((index = searchContent.indexOf(searchQuery, index)) != -1) {
                    SearchMatch match = new SearchMatch();
                    match.setPage(pageNumber);

                    // 提取匹配的文本
                    int start = Math.max(0, index - 50);
                    int end = Math.min(content.length(), index + searchQuery.length() + 50);
                    String matchedText = content.substring(index, index + searchQuery.length());
                    String contextText = content.substring(start, end);

                    match.setText(matchedText);
                    match.setContext(contextText);

                    // 添加高亮标记
                    List<String> highlights = new ArrayList<>();
                    highlights.add(matchedText);
                    match.setHighlights(highlights);

                    matches.add(match);

                    // 移动到下一个位置
                    index += searchQuery.length();
                }
            }

            // 6. 分页处理
            int total = matches.size();
            int totalPages = (int) Math.ceil((double) total / pageSize);
            int offset = (page - 1) * pageSize;
            int endIndex = Math.min(offset + pageSize, total);

            List<SearchMatch> pagedMatches = new ArrayList<>();
            if (offset < total) {
                pagedMatches = matches.subList(offset, endIndex);
            }

            // 7. 构建响应数据
            SearchData searchData = new SearchData();
            searchData.setQuery(query);
            searchData.setTotal(total);
            searchData.setMatches(pagedMatches);

            SearchResponse response = new SearchResponse(true, "搜索完成", searchData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("搜索文档内容过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SearchResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
