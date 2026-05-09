package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchAutocomplete {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到搜索联想请求 ===");
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

    // 响应DTO
    public static class AutocompleteResponse {
        private boolean success;
        private String message;
        private List<AutocompleteItem> data;

        public AutocompleteResponse(boolean success, String message, List<AutocompleteItem> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<AutocompleteItem> getData() { return data; }
        public void setData(List<AutocompleteItem> data) { this.data = data; }
    }

    public static class AutocompleteItem {
        private String id;
        private String text;
        private String type;
        private double relevance;
        private String prefix;
        private String suffix;

        public AutocompleteItem(String id, String text, String type, double relevance, String prefix, String suffix) {
            this.id = id;
            this.text = text;
            this.type = type;
            this.relevance = relevance;
            this.prefix = prefix;
            this.suffix = suffix;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public double getRelevance() { return relevance; }
        public void setRelevance(double relevance) { this.relevance = relevance; }

        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }

        public String getSuffix() { return suffix; }
        public void setSuffix(String suffix) { this.suffix = suffix; }
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<AutocompleteResponse> searchAutocomplete(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false, defaultValue = "5") Integer limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("query", query);
        requestParams.put("type", type);
        requestParams.put("limit", limit);
        printRequest(requestParams);

        try {
            // 1. 验证请求数据
            if (query == null || query.trim().isEmpty() || query.length() < 2) {
                return ResponseEntity.ok(new AutocompleteResponse(true, "查询词太短", new ArrayList<>()));
            }

            // 2. 获取当前用户ID（可选）
            Long userId = getUserIdFromToken(authHeader);

            // 3. 构建联想结果
            List<AutocompleteItem> items = new ArrayList<>();

            // 从文档标题联想
            if ("all".equals(type) || "documents".equals(type)) {
                items.addAll(getDocumentSuggestions(query, userId, limit));
            }

            // 从词汇联想
            if ("all".equals(type) || "vocabulary".equals(type)) {
                items.addAll(getVocabularySuggestions(query, userId, limit));
            }

            // 从搜索历史联想
            if ("all".equals(type) || "history".equals(type)) {
                items.addAll(getHistorySuggestions(query, userId, limit));
            }

            // 4. 按相关性排序并限制数量
            items.sort((a, b) -> Double.compare(b.getRelevance(), a.getRelevance()));
            if (items.size() > limit) {
                items = items.subList(0, limit);
            }

            // 5. 准备响应数据
            AutocompleteResponse response = new AutocompleteResponse(true, "搜索联想成功", items);

            // 打印查询结果
            printQueryResult("找到 " + items.size() + " 条联想结果");

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("搜索联想过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new AutocompleteResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 从文档标题获取联想
    private List<AutocompleteItem> getDocumentSuggestions(String query, Long userId, int limit) {
        List<AutocompleteItem> items = new ArrayList<>();

        try {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT document_id, title FROM documents ");
            sqlBuilder.append("WHERE title LIKE ? AND status = 'processed' ");

            List<Object> params = new ArrayList<>();
            params.add("%" + query + "%");

            if (userId != null) {
                sqlBuilder.append("AND user_id = ? ");
                params.add(userId);
            }

            sqlBuilder.append("ORDER BY created_at DESC LIMIT ?");
            params.add(limit);

            List<Map<String, Object>> documents = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());

            for (int i = 0; i < documents.size(); i++) {
                Map<String, Object> doc = documents.get(i);
                String title = (String) doc.get("title");

                // 计算相关性
                double relevance = calculateRelevance(query, title, null);

                // 构建联想项
                AutocompleteItem item = new AutocompleteItem(
                        "doc_" + doc.get("document_id"),
                        title,
                        "document",
                        relevance,
                        "",
                        ""
                );
                items.add(item);
            }

        } catch (Exception e) {
            System.err.println("获取文档联想失败: " + e.getMessage());
        }

        return items;
    }

    // 从词汇获取联想
    private List<AutocompleteItem> getVocabularySuggestions(String query, Long userId, int limit) {
        List<AutocompleteItem> items = new ArrayList<>();

        try {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT word_id, word FROM words ");
            sqlBuilder.append("WHERE word LIKE ? ");

            List<Object> params = new ArrayList<>();
            params.add(query + "%");

            sqlBuilder.append("ORDER BY word ASC LIMIT ?");
            params.add(limit);

            List<Map<String, Object>> words = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());

            for (int i = 0; i < words.size(); i++) {
                Map<String, Object> word = words.get(i);
                String wordText = (String) word.get("word");

                // 计算相关性
                double relevance = calculateRelevance(query, wordText, null);

                // 构建联想项
                AutocompleteItem item = new AutocompleteItem(
                        "word_" + word.get("word_id"),
                        wordText,
                        "vocabulary",
                        relevance,
                        "",
                        ""
                );
                items.add(item);
            }

        } catch (Exception e) {
            System.err.println("获取词汇联想失败: " + e.getMessage());
        }

        return items;
    }

    // 从搜索历史获取联想
    private List<AutocompleteItem> getHistorySuggestions(String query, Long userId, int limit) {
        List<AutocompleteItem> items = new ArrayList<>();

        try {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT DISTINCT keyword FROM search_history ");
            sqlBuilder.append("WHERE keyword LIKE ? ");

            List<Object> params = new ArrayList<>();
            params.add(query + "%");

            if (userId != null) {
                sqlBuilder.append("AND user_id = ? ");
                params.add(userId);
            }

            sqlBuilder.append("ORDER BY MAX(timestamp) DESC LIMIT ?");
            params.add(limit);

            List<Map<String, Object>> history = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());

            for (int i = 0; i < history.size(); i++) {
                Map<String, Object> record = history.get(i);
                String keyword = (String) record.get("keyword");

                // 计算相关性
                double relevance = calculateRelevance(query, keyword, null);

                // 构建联想项
                AutocompleteItem item = new AutocompleteItem(
                        "history_" + i,
                        keyword,
                        "history",
                        relevance,
                        "",
                        ""
                );
                items.add(item);
            }

        } catch (Exception e) {
            System.err.println("获取历史联想失败: " + e.getMessage());
        }

        return items;
    }

    // 计算相关性
    private double calculateRelevance(String query, String text, String content) {
        if (text == null) text = "";
        if (content == null) content = "";

        String lowerQuery = query.toLowerCase();
        String lowerText = text.toLowerCase();
        String lowerContent = content.toLowerCase();

        double score = 0.0;

        // 文本匹配权重更高
        if (lowerText.contains(lowerQuery)) {
            score += 0.7;
        }

        // 内容匹配
        if (lowerContent.contains(lowerQuery)) {
            score += 0.3;
        }

        // 精确匹配加分
        if (text.contains(query) || content.contains(query)) {
            score += 0.2;
        }

        return Math.min(score, 1.0);
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
}
