package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchGetSuggestions {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取搜索建议请求 ===");
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
    public static class GetSuggestionsResponse {
        private boolean success;
        private String message;
        private List<SuggestionItem> data;

        public GetSuggestionsResponse(boolean success, String message, List<SuggestionItem> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<SuggestionItem> getData() { return data; }
        public void setData(List<SuggestionItem> data) { this.data = data; }
    }

    public static class SuggestionItem {
        private String id;
        private String keyword;
        private String type;
        private int count;
        private double relevance;
        private String icon;

        public SuggestionItem(String id, String keyword, String type, int count, double relevance, String icon) {
            this.id = id;
            this.keyword = keyword;
            this.type = type;
            this.count = count;
            this.relevance = relevance;
            this.icon = icon;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }

        public double getRelevance() { return relevance; }
        public void setRelevance(double relevance) { this.relevance = relevance; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
    }

    @GetMapping("/suggestions")
    public ResponseEntity<GetSuggestionsResponse> getSearchSuggestions(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("query", query);
        requestParams.put("type", type);
        requestParams.put("limit", limit);
        printRequest(requestParams);

        try {
            // 1. 验证请求数据
            if (query == null || query.trim().isEmpty()) {
                // 返回默认建议
                List<SuggestionItem> defaultSuggestions = getDefaultSuggestions();
                GetSuggestionsResponse response = new GetSuggestionsResponse(true, "获取默认建议成功", defaultSuggestions);

                printQueryResult("返回默认建议: " + defaultSuggestions.size() + " 条");
                printResponse(response);

                return ResponseEntity.ok(response);
            }

            // 2. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);

            // 3. 构建SQL查询获取搜索建议
            List<SuggestionItem> suggestions = new ArrayList<>();

            // 从搜索历史获取建议
            suggestions.addAll(getSuggestionsFromHistory(query, userId, type, limit));

            // 从文档标题获取建议
            if ("all".equals(type) || "documents".equals(type)) {
                suggestions.addAll(getSuggestionsFromDocuments(query, userId, limit));
            }

            // 从词汇获取建议
            if ("all".equals(type) || "vocabulary".equals(type)) {
                suggestions.addAll(getSuggestionsFromVocabulary(query, userId, limit));
            }

            // 4. 去重和排序
            suggestions = deduplicateAndSort(suggestions, limit);

            // 5. 如果建议太少，补充默认建议
            if (suggestions.size() < limit) {
                suggestions.addAll(getDefaultSuggestions());
                suggestions = suggestions.subList(0, Math.min(suggestions.size(), limit));
            }

            // 6. 准备响应数据
            GetSuggestionsResponse response = new GetSuggestionsResponse(true, "获取搜索建议成功", suggestions);

            printQueryResult("找到 " + suggestions.size() + " 条搜索建议");
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取搜索建议过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetSuggestionsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 从搜索历史获取建议
    private List<SuggestionItem> getSuggestionsFromHistory(String query, Long userId, String type, int limit) {
        List<SuggestionItem> suggestions = new ArrayList<>();

        try {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT keyword, search_type, COUNT(*) as count, MAX(timestamp) as last_time ");
            sqlBuilder.append("FROM search_history ");
            sqlBuilder.append("WHERE keyword LIKE ? ");

            List<Object> params = new ArrayList<>();
            params.add(query + "%");

            if (userId != null) {
                sqlBuilder.append("AND user_id = ? ");
                params.add(userId);
            }

            if (type != null && !"all".equals(type)) {
                sqlBuilder.append("AND search_type = ? ");
                params.add(type);
            }

            sqlBuilder.append("GROUP BY keyword, search_type ");
            sqlBuilder.append("ORDER BY count DESC, last_time DESC ");
            sqlBuilder.append("LIMIT ?");
            params.add(limit);

            List<Map<String, Object>> history = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());

            for (Map<String, Object> record : history) {
                String keyword = (String) record.get("keyword");
                String searchType = (String) record.get("search_type");
                int count = ((Number) record.get("count")).intValue();

                // 计算相关性
                double relevance = calculateRelevance(query, keyword);

                // 获取图标
                String icon = getSuggestionIcon(searchType);

                SuggestionItem item = new SuggestionItem(
                        "history_" + keyword.hashCode(),
                        keyword,
                        searchType,
                        count,
                        relevance,
                        icon
                );
                suggestions.add(item);
            }

        } catch (Exception e) {
            System.err.println("从搜索历史获取建议失败: " + e.getMessage());
        }

        return suggestions;
    }

    // 从文档获取建议
    private List<SuggestionItem> getSuggestionsFromDocuments(String query, Long userId, int limit) {
        List<SuggestionItem> suggestions = new ArrayList<>();

        try {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT title FROM documents ");
            sqlBuilder.append("WHERE title LIKE ? AND status = 'processed' ");

            List<Object> params = new ArrayList<>();
            params.add(query + "%");

            if (userId != null) {
                sqlBuilder.append("AND user_id = ? ");
                params.add(userId);
            }

            sqlBuilder.append("ORDER BY created_at DESC LIMIT ?");
            params.add(limit);

            List<Map<String, Object>> documents = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());

            for (Map<String, Object> doc : documents) {
                String title = (String) doc.get("title");

                // 计算相关性
                double relevance = calculateRelevance(query, title);

                SuggestionItem item = new SuggestionItem(
                        "doc_" + title.hashCode(),
                        title,
                        "documents",
                        1,
                        relevance,
                        "document-text"
                );
                suggestions.add(item);
            }

        } catch (Exception e) {
            System.err.println("从文档获取建议失败: " + e.getMessage());
        }

        return suggestions;
    }

    // 从词汇获取建议
    private List<SuggestionItem> getSuggestionsFromVocabulary(String query, Long userId, int limit) {
        List<SuggestionItem> suggestions = new ArrayList<>();

        try {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT word FROM words ");
            sqlBuilder.append("WHERE word LIKE ? ");

            List<Object> params = new ArrayList<>();
            params.add(query + "%");

            sqlBuilder.append("ORDER BY word ASC LIMIT ?");
            params.add(limit);

            List<Map<String, Object>> words = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());

            for (Map<String, Object> word : words) {
                String wordText = (String) word.get("word");

                // 计算相关性
                double relevance = calculateRelevance(query, wordText);

                SuggestionItem item = new SuggestionItem(
                        "word_" + wordText.hashCode(),
                        wordText,
                        "vocabulary",
                        1,
                        relevance,
                        "book-open"
                );
                suggestions.add(item);
            }

        } catch (Exception e) {
            System.err.println("从词汇获取建议失败: " + e.getMessage());
        }

        return suggestions;
    }

    // 计算相关性
    private double calculateRelevance(String query, String text) {
        if (text == null) text = "";

        String lowerQuery = query.toLowerCase();
        String lowerText = text.toLowerCase();

        double score = 0.0;

        // 文本匹配
        if (lowerText.startsWith(lowerQuery)) {
            score += 0.8;
        } else if (lowerText.contains(lowerQuery)) {
            score += 0.5;
        }

        // 精确匹配加分
        if (text.contains(query)) {
            score += 0.2;
        }

        return Math.min(score, 1.0);
    }

    // 获取建议图标
    private String getSuggestionIcon(String type) {
        Map<String, String> iconMap = new HashMap<>();
        iconMap.put("documents", "document-text");
        iconMap.put("vocabulary", "book-open");
        iconMap.put("notes", "pencil");
        iconMap.put("highlights", "highlighter");
        iconMap.put("global", "search");
        iconMap.put("all", "search");

        return iconMap.getOrDefault(type, "search");
    }

    // 去重和排序
    private List<SuggestionItem> deduplicateAndSort(List<SuggestionItem> suggestions, int limit) {
        // 使用Map去重，以keyword为键
        Map<String, SuggestionItem> uniqueMap = new HashMap<>();

        for (SuggestionItem item : suggestions) {
            String key = item.getKeyword() + "_" + item.getType();
            if (!uniqueMap.containsKey(key) || item.getRelevance() > uniqueMap.get(key).getRelevance()) {
                uniqueMap.put(key, item);
            }
        }

        // 转换为列表并排序
        List<SuggestionItem> result = new ArrayList<>(uniqueMap.values());
        result.sort((a, b) -> Double.compare(b.getRelevance(), a.getRelevance()));

        // 限制数量
        if (result.size() > limit) {
            result = result.subList(0, limit);
        }

        return result;
    }

    // 获取默认建议
    private List<SuggestionItem> getDefaultSuggestions() {
        List<SuggestionItem> suggestions = new ArrayList<>();

        suggestions.add(new SuggestionItem("1", "英语学习", "all", 100, 0.9, "book-open"));
        suggestions.add(new SuggestionItem("2", "技术文档", "documents", 50, 0.8, "document-text"));
        suggestions.add(new SuggestionItem("3", "商务英语", "vocabulary", 30, 0.7, "briefcase"));
        suggestions.add(new SuggestionItem("4", "阅读技巧", "all", 25, 0.6, "academic-cap"));
        suggestions.add(new SuggestionItem("5", "单词记忆", "vocabulary", 20, 0.5, "light-bulb"));

        return suggestions;
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