package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchVocabulary {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到词汇搜索请求 ===");
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
    public static class VocabularySearchRequest {
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
    public static class VocabularySearchResponse {
        private boolean success;
        private String message;
        private VocabularySearchData data;

        public VocabularySearchResponse(boolean success, String message, VocabularySearchData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public VocabularySearchData getData() { return data; }
        public void setData(VocabularySearchData data) { this.data = data; }
    }

    public static class VocabularySearchData {
        private int total;
        private int page;
        private int pageSize;
        private int totalPages;
        private List<VocabularyItem> items;
        private Map<String, Object> facets;
        private long queryTime;

        public VocabularySearchData(int total, int page, int pageSize, int totalPages,
                                    List<VocabularyItem> items, Map<String, Object> facets,
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

        public List<VocabularyItem> getItems() { return items; }
        public void setItems(List<VocabularyItem> items) { this.items = items; }

        public Map<String, Object> getFacets() { return facets; }
        public void setFacets(Map<String, Object> facets) { this.facets = facets; }

        public long getQueryTime() { return queryTime; }
        public void setQueryTime(long queryTime) { this.queryTime = queryTime; }
    }

    public static class VocabularyItem {
        private String id;
        private String word;
        private String phonetic;
        private String definition;
        private String translation;
        private List<String> examples;
        private List<String> tags;
        private String difficulty;
        private double relevance;
        private Map<String, Object> highlight;
        private String createdAt;
        private String updatedAt;
        private String audioUrl;
        private String url;
        private boolean inVocabulary;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getPhonetic() { return phonetic; }
        public void setPhonetic(String phonetic) { this.phonetic = phonetic; }

        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }

        public String getTranslation() { return translation; }
        public void setTranslation(String translation) { this.translation = translation; }

        public List<String> getExamples() { return examples; }
        public void setExamples(List<String> examples) { this.examples = examples; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

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

        public String getAudioUrl() { return audioUrl; }
        public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public boolean isInVocabulary() { return inVocabulary; }
        public void setInVocabulary(boolean inVocabulary) { this.inVocabulary = inVocabulary; }
    }

    @GetMapping("/vocabulary")
    public ResponseEntity<VocabularySearchResponse> searchVocabulary(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false, defaultValue = "relevance") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String language,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        long startTime = System.currentTimeMillis();

        // 构建请求对象
        VocabularySearchRequest request = new VocabularySearchRequest();
        request.setQuery(query);
        request.setPage(page);
        request.setPageSize(pageSize);
        request.setSortBy(sortBy);
        request.setSortOrder(sortOrder);

        Map<String, Object> filters = new HashMap<>();
        if (difficulty != null) filters.put("difficulty", difficulty);
        if (tag != null) filters.put("tag", tag);
        if (language != null) filters.put("language", language);
        request.setFilters(filters);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证请求数据
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new VocabularySearchResponse(false, "搜索关键词不能为空", null)
                );
            }

            // 2. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);

            // 3. 构建SQL查询
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT w.word_id, w.word, w.phonetic, wd.definition, w.translation_pos AS translation, ");
            sqlBuilder.append("w.created_at, w.updated_at, w.audio_url, w.difficulty, w.language ");
            sqlBuilder.append("FROM words w ");
            sqlBuilder.append("LEFT JOIN word_definitions wd ON w.word_id = wd.word_id ");
            sqlBuilder.append("WHERE (w.word LIKE ? OR wd.definition LIKE ? OR w.translation_pos LIKE ?) ");

            List<Object> params = new ArrayList<>();
            params.add("%" + query + "%");
            params.add("%" + query + "%");
            params.add("%" + query + "%");

            // 添加难度过滤
            if (difficulty != null && !difficulty.isEmpty()) {
                sqlBuilder.append("AND w.difficulty = ? ");
                params.add(difficulty);
            }

            // 添加语言过滤
            if (language != null && !language.isEmpty()) {
                sqlBuilder.append("AND w.language = ? ");
                params.add(language);
            }

            // 添加标签过滤
            if (tag != null && !tag.isEmpty()) {
                sqlBuilder.append("AND EXISTS (SELECT 1 FROM user_vocabulary_tags ut ");
                sqlBuilder.append("INNER JOIN vocabulary_tags vt ON ut.tag_id = vt.tag_id ");
                sqlBuilder.append("WHERE ut.word_id = w.word_id AND vt.tag_name = ?) ");
                params.add(tag);
            }

            // 4. 获取总数
            String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder.toString() + ") AS count_table";
            int total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

            // 5. 添加排序和分页
            if ("relevance".equals(sortBy)) {
                // 相关性排序：单词完全匹配优先
                sqlBuilder.append("ORDER BY CASE WHEN w.word = ? THEN 1 ");
                sqlBuilder.append("WHEN w.word LIKE ? THEN 2 ");
                sqlBuilder.append("ELSE 3 END, ");
                sqlBuilder.append("w.word ASC ");
                params.add(query);
                params.add(query + "%");
            } else if ("word".equals(sortBy)) {
                sqlBuilder.append("ORDER BY w.word ");
                sqlBuilder.append("desc".equalsIgnoreCase(sortOrder) ? "DESC " : "ASC ");
            } else if ("createdAt".equals(sortBy)) {
                sqlBuilder.append("ORDER BY w.created_at ");
                sqlBuilder.append("desc".equalsIgnoreCase(sortOrder) ? "DESC " : "ASC ");
            } else {
                sqlBuilder.append("ORDER BY w.word ASC ");
            }

            sqlBuilder.append("LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            // 6. 执行查询
            List<Map<String, Object>> words = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
            printQueryResult("找到 " + total + " 个词汇，返回 " + words.size() + " 个");

            // 7. 处理结果
            List<VocabularyItem> items = new ArrayList<>();
            for (Map<String, Object> word : words) {
                VocabularyItem item = new VocabularyItem();
                Long wordId = ((Number) word.get("word_id")).longValue();

                item.setId("word_" + wordId);
                item.setWord((String) word.get("word"));
                item.setPhonetic((String) word.get("phonetic"));
                item.setDefinition((String) word.get("definition"));
                item.setTranslation((String) word.get("translation"));
                item.setDifficulty((String) word.get("difficulty"));

                // 计算相关性
                double relevance = calculateRelevance(query, (String) word.get("word"),
                        (String) word.get("definition"));
                item.setRelevance(relevance);

                // 构建高亮信息
                Map<String, Object> highlight = new HashMap<>();
                highlight.put("word", highlightText(query, (String) word.get("word")));
                highlight.put("definition", highlightText(query, (String) word.get("definition")));
                highlight.put("translation", highlightText(query, (String) word.get("translation")));
                item.setHighlight(highlight);

                item.setCreatedAt(word.get("created_at") != null ? word.get("created_at").toString() : null);
                item.setUpdatedAt(word.get("updated_at") != null ? word.get("updated_at").toString() : null);
                item.setAudioUrl((String) word.get("audio_url"));
                item.setUrl("/vocabulary/" + wordId);

                // 获取例句
                List<String> examples = getWordExamples(wordId);
                item.setExamples(examples);

                // 获取标签
                List<String> tags = getWordTags(wordId);
                item.setTags(tags);

                // 检查是否在用户生词本中
                boolean inVocabulary = checkIfInVocabulary(userId, wordId);
                item.setInVocabulary(inVocabulary);

                items.add(item);
            }

            // 8. 构建facet数据
            Map<String, Object> facets = buildVocabularyFacets(query, filters);

            // 9. 记录搜索历史
            if (userId != null) {
                saveSearchHistory(userId, query, "vocabulary", total);
            }

            // 10. 计算查询时间
            long queryTime = System.currentTimeMillis() - startTime;

            // 11. 计算总页数
            int totalPages = (int) Math.ceil((double) total / pageSize);

            // 12. 准备响应数据
            VocabularySearchData searchData = new VocabularySearchData(
                    total, page, pageSize, totalPages, items, facets, queryTime
            );

            VocabularySearchResponse response = new VocabularySearchResponse(true, "词汇搜索成功", searchData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("词汇搜索过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new VocabularySearchResponse(false, "服务器内部错误: " + e.getMessage(), null)
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
    private double calculateRelevance(String query, String word, String definition) {
        if (word == null) word = "";
        if (definition == null) definition = "";

        String lowerQuery = query.toLowerCase();
        String lowerWord = word.toLowerCase();
        String lowerDefinition = definition.toLowerCase();

        double score = 0.0;

        // 单词完全匹配权重最高
        if (lowerWord.equals(lowerQuery)) {
            score += 0.8;
        } else if (lowerWord.startsWith(lowerQuery)) {
            score += 0.6;
        } else if (lowerWord.contains(lowerQuery)) {
            score += 0.4;
        }

        // 定义匹配
        if (lowerDefinition.contains(lowerQuery)) {
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

    // 获取单词例句
    private List<String> getWordExamples(Long wordId) {
        List<String> examples = new ArrayList<>();
        try {
            String sql = "SELECT example_sentence FROM word_examples WHERE word_id = ? LIMIT 3";
            List<Map<String, Object>> exampleList = jdbcTemplate.queryForList(sql, wordId);
            for (Map<String, Object> example : exampleList) {
                examples.add((String) example.get("example_sentence"));
            }
        } catch (Exception e) {
            System.err.println("获取单词例句失败: " + e.getMessage());
        }
        return examples;
    }

    // 获取单词标签
    private List<String> getWordTags(Long wordId) {
        List<String> tags = new ArrayList<>();
        try {
            String sql = "SELECT vt.tag_name FROM vocabulary_tags vt " +
                    "INNER JOIN user_vocabulary_tags ut ON vt.tag_id = ut.tag_id " +
                    "INNER JOIN user_vocabulary uv ON ut.user_vocab_id = uv.user_vocab_id " +
                    "WHERE uv.word_id = ?";
            List<Map<String, Object>> tagList = jdbcTemplate.queryForList(sql, wordId);
            for (Map<String, Object> tag : tagList) {
                tags.add((String) tag.get("tag_name"));
            }
        } catch (Exception e) {
            System.err.println("获取单词标签失败: " + e.getMessage());
        }
        return tags;
    }

    // 检查是否在用户生词本中
    private boolean checkIfInVocabulary(Long userId, Long wordId) {
        if (userId == null) return false;

        try {
            String sql = "SELECT COUNT(*) FROM user_vocabulary WHERE user_id = ? AND word_id = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, wordId);
            return count != null && count > 0;
        } catch (Exception e) {
            System.err.println("检查生词本失败: " + e.getMessage());
            return false;
        }
    }

    // 构建词汇facet数据
    private Map<String, Object> buildVocabularyFacets(String query, Map<String, Object> filters) {
        Map<String, Object> facets = new HashMap<>();

        try {
            // 难度facet
            String difficultySql = "SELECT w.difficulty, COUNT(*) as count FROM words w " +
                    "LEFT JOIN word_definitions wd ON w.word_id = wd.word_id " +
                    "WHERE (w.word LIKE ? OR wd.definition LIKE ? OR w.translation_pos LIKE ?) " +
                    "GROUP BY w.difficulty ORDER BY count DESC";

            List<Object> params = new ArrayList<>();
            params.add("%" + query + "%");
            params.add("%" + query + "%");
            params.add("%" + query + "%");

            List<Map<String, Object>> difficultyFacet = jdbcTemplate.queryForList(difficultySql, params.toArray());
            facets.put("difficulties", difficultyFacet);

            // 语言facet
            String languageSql = "SELECT w.language, COUNT(*) as count FROM words w " +
                    "LEFT JOIN word_definitions wd ON w.word_id = wd.word_id " +
                    "WHERE (w.word LIKE ? OR wd.definition LIKE ? OR w.translation_pos LIKE ?) " +
                    "GROUP BY w.language ORDER BY count DESC";

            List<Map<String, Object>> languageFacet = jdbcTemplate.queryForList(languageSql, params.toArray());
            facets.put("languages", languageFacet);

            // 标签facet
            String tagSql = "SELECT vt.tag_name, COUNT(*) as count FROM vocabulary_tags vt " +
                    "INNER JOIN user_vocabulary_tags ut ON vt.tag_id = ut.tag_id " +
                    "INNER JOIN words w ON ut.word_id = w.word_id " +
                    "LEFT JOIN word_definitions wd ON w.word_id = wd.word_id " +
                    "WHERE (w.word LIKE ? OR wd.definition LIKE ? OR w.translation_pos LIKE ?) " +
                    "GROUP BY vt.tag_id, vt.tag_name ORDER BY count DESC LIMIT 20";

            List<Map<String, Object>> tagFacet = jdbcTemplate.queryForList(tagSql, params.toArray());
            facets.put("tags", tagFacet);

        } catch (Exception e) {
            System.err.println("构建词汇facet失败: " + e.getMessage());
        }

        return facets;
    }

    // 保存搜索历史
    private void saveSearchHistory(Long userId, String keyword, String searchType, int resultCount) {
        try {
            String sql = "INSERT INTO search_history (user_id, keyword, search_type, result_count, timestamp) " +
                    "VALUES (?, ?, ?, ?, NOW())";
            jdbcTemplate.update(sql, userId, keyword, searchType, resultCount);
            System.out.println("已保存词汇搜索历史: 用户=" + userId + ", 关键词=" + keyword);
        } catch (Exception e) {
            System.err.println("保存搜索历史失败: " + e.getMessage());
        }
    }
}
