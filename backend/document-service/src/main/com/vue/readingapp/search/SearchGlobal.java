package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/search")
public class SearchGlobal {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到全局搜索请求 ===");
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
    public static class SearchRequest {
        private String query;
        private String type = "all";
        private Integer page = 1;
        private Integer pageSize = 20;
        private String sortBy = "relevance";
        private String sortOrder = "desc";
        private Map<String, Object> filters = new HashMap<>();

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

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
        private int total;
        private int page;
        private int pageSize;
        private int totalPages;
        private List<SearchItem> items;
        private Map<String, Object> facets;
        private long queryTime;
        private String type;

        public SearchData(int total, int page, int pageSize, int totalPages,
                          List<SearchItem> items, Map<String, Object> facets,
                          long queryTime, String type) {
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
            this.totalPages = totalPages;
            this.items = items;
            this.facets = facets;
            this.queryTime = queryTime;
            this.type = type;
        }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

        public List<SearchItem> getItems() { return items; }
        public void setItems(List<SearchItem> items) { this.items = items; }

        public Map<String, Object> getFacets() { return facets; }
        public void setFacets(Map<String, Object> facets) { this.facets = facets; }

        public long getQueryTime() { return queryTime; }
        public void setQueryTime(long queryTime) { this.queryTime = queryTime; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    // 搜索项DTO - 合并所有字段
    public static class SearchItem {
        private String id;
        private String type;
        private String title;
        private String content;
        private String excerpt;
        private String author;
        private List<String> tags = new ArrayList<>();
        private String language;
        private int wordCount;
        private String difficulty;
        private double relevance;
        private Map<String, Object> highlight = new HashMap<>();
        private String createdAt;
        private String updatedAt;
        private String thumbnail;
        private String url;

        // 词汇专用字段
        private String word;
        private String phonetic;
        private String definition;
        private String translation;
        private List<String> examples = new ArrayList<>();
        private String audioUrl;

        // 笔记和高亮专用字段
        private String documentTitle;
        private Long documentId;
        private int pageNumber;
        private String selectedText;
        private String note;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

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

        public String getAudioUrl() { return audioUrl; }
        public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

        public String getDocumentTitle() { return documentTitle; }
        public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }

        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }

        public int getPageNumber() { return pageNumber; }
        public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }

        public String getSelectedText() { return selectedText; }
        public void setSelectedText(String selectedText) { this.selectedText = selectedText; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    // 内部实体类
    public static class SearchHistory {
        private Long searchId;
        private Long userId;
        private String keyword;
        private String searchType;
        private Integer resultCount;
        private Map<String, Object> filters;
        private Boolean success;
        private LocalDateTime timestamp;

        // Getters and Setters
        public Long getSearchId() { return searchId; }
        public void setSearchId(Long searchId) { this.searchId = searchId; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }

        public String getSearchType() { return searchType; }
        public void setSearchType(String searchType) { this.searchType = searchType; }

        public Integer getResultCount() { return resultCount; }
        public void setResultCount(Integer resultCount) { this.resultCount = resultCount; }

        public Map<String, Object> getFilters() { return filters; }
        public void setFilters(Map<String, Object> filters) { this.filters = filters; }

        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    @GetMapping("")
    public ResponseEntity<SearchResponse> globalSearch(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false, defaultValue = "relevance") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        long startTime = System.currentTimeMillis();

        // 构建请求对象
        SearchRequest request = new SearchRequest();
        request.setQuery(query);
        request.setType(type);
        request.setPage(page);
        request.setPageSize(pageSize);
        request.setSortBy(sortBy);
        request.setSortOrder(sortOrder);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证请求数据
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SearchResponse(false, "搜索关键词不能为空", null)
                );
            }

            if (query.length() > 200) {
                return ResponseEntity.badRequest().body(
                        new SearchResponse(false, "搜索关键词不能超过200个字符", null)
                );
            }

            // 验证搜索类型
            List<String> validTypes = Arrays.asList("all", "documents", "vocabulary", "notes", "highlights");
            if (!validTypes.contains(type)) {
                return ResponseEntity.badRequest().body(
                        new SearchResponse(false, "无效的搜索类型，必须是: all, documents, vocabulary, notes, highlights", null)
                );
            }

            // 2. 获取当前用户ID（从token中解析）
            Long userId = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                // 简单解析token获取用户ID（课设简化处理）
                try {
                    String sql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
                    List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sql, token);
                    if (!sessions.isEmpty()) {
                        userId = ((Number) sessions.get(0).get("user_id")).longValue();
                    }
                } catch (Exception e) {
                    System.out.println("Token解析失败: " + e.getMessage());
                }
            }

            // 3. 根据类型执行搜索
            List<SearchItem> allResults = new ArrayList<>();
            int totalResults = 0;

            if ("all".equals(type) || "documents".equals(type)) {
                // 搜索文档
                List<SearchItem> documentResults = searchDocuments(query, userId, page, pageSize);
                allResults.addAll(documentResults);
                totalResults += documentResults.size();
            }

            if ("all".equals(type) || "vocabulary".equals(type)) {
                // 搜索词汇
                List<SearchItem> vocabularyResults = searchVocabulary(query, userId, page, pageSize);
                allResults.addAll(vocabularyResults);
                totalResults += vocabularyResults.size();
            }

            if ("all".equals(type) || "notes".equals(type)) {
                // 搜索笔记
                List<SearchItem> noteResults = searchNotes(query, userId, page, pageSize);
                allResults.addAll(noteResults);
                totalResults += noteResults.size();
            }

            if ("all".equals(type) || "highlights".equals(type)) {
                // 搜索高亮
                List<SearchItem> highlightResults = searchHighlights(query, userId, page, pageSize);
                allResults.addAll(highlightResults);
                totalResults += highlightResults.size();
            }

            // 4. 排序结果（根据相关性）
            if ("relevance".equals(sortBy)) {
                allResults.sort((a, b) -> {
                    if ("desc".equals(sortOrder)) {
                        return Double.compare(b.getRelevance(), a.getRelevance());
                    } else {
                        return Double.compare(a.getRelevance(), b.getRelevance());
                    }
                });
            } else if ("createdAt".equals(sortBy)) {
                allResults.sort((a, b) -> {
                    if ("desc".equals(sortOrder)) {
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    } else {
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    }
                });
            }

            // 5. 分页处理
            int startIndex = (page - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, allResults.size());
            List<SearchItem> pagedResults = allResults.subList(startIndex, endIndex);

            // 6. 计算分页信息
            int totalPages = (int) Math.ceil((double) allResults.size() / pageSize);

            // 7. 构建facet数据
            Map<String, Object> facets = new HashMap<>();
            Map<String, Long> typeFacet = new HashMap<>();

            typeFacet.put("documents", allResults.stream().filter(item -> "document".equals(item.getType())).count());
            typeFacet.put("vocabulary", allResults.stream().filter(item -> "vocabulary".equals(item.getType())).count());
            typeFacet.put("notes", allResults.stream().filter(item -> "note".equals(item.getType())).count());
            typeFacet.put("highlights", allResults.stream().filter(item -> "highlight".equals(item.getType())).count());

            facets.put("type", typeFacet);

            // 8. 记录搜索历史
            if (userId != null) {
                saveSearchHistory(userId, query, type, totalResults);
            }

            // 9. 计算查询时间
            long queryTime = System.currentTimeMillis() - startTime;

            // 10. 准备响应数据
            SearchData searchData = new SearchData(
                    allResults.size(),
                    page,
                    pageSize,
                    totalPages,
                    pagedResults,
                    facets,
                    queryTime,
                    type
            );

            SearchResponse response = new SearchResponse(true, "搜索成功", searchData);

            // 打印查询结果
            printQueryResult("找到 " + allResults.size() + " 个结果，返回 " + pagedResults.size() + " 个");

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("全局搜索过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SearchResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 搜索文档
    private List<SearchItem> searchDocuments(String query, Long userId, int page, int pageSize) {
        List<SearchItem> results = new ArrayList<>();

        try {
            // 修正：documents 表没有 content 字段，内容在 description 中，或者需要关联查询
            String sql = "SELECT d.document_id, d.title, d.description as content, d.author, d.language, " +
                    "d.created_at, d.updated_at, d.status " +
                    "FROM documents d " +
                    "WHERE (d.title LIKE ? OR d.description LIKE ?) ";

            if (userId != null) {
                sql += "AND d.user_id = ? ";
            }

            sql += "ORDER BY d.created_at DESC LIMIT ? OFFSET ?";

            List<Object> params = new ArrayList<>();
            params.add("%" + query + "%");
            params.add("%" + query + "%");
            if (userId != null) {
                params.add(userId);
            }
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            List<Map<String, Object>> documents = jdbcTemplate.queryForList(sql, params.toArray());

            for (Map<String, Object> doc : documents) {
                SearchItem item = new SearchItem();
                item.setId("doc_" + doc.get("document_id"));
                item.setType("document");
                item.setTitle((String) doc.get("title"));
                item.setContent((String) doc.get("content"));

                // 生成摘要
                String content = (String) doc.get("content");
                if (content != null && content.length() > 200) {
                    item.setExcerpt(content.substring(0, 200) + "...");
                } else {
                    item.setExcerpt(content != null ? content : "");
                }

                item.setAuthor((String) doc.get("author"));
                item.setLanguage((String) doc.get("language"));
                item.setWordCount(doc.get("word_count") != null ? ((Number) doc.get("word_count")).intValue() : 0);
                item.setDifficulty((String) doc.get("difficulty"));

                // 计算相关性（简单实现）
                double relevance = calculateRelevance(query, (String) doc.get("title"), (String) doc.get("content"));
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

                results.add(item);
            }

        } catch (Exception e) {
            System.err.println("搜索文档时发生错误: " + e.getMessage());
        }

        return results;
    }

    // 搜索词汇
    private List<SearchItem> searchVocabulary(String query, Long userId, int page, int pageSize) {
        List<SearchItem> results = new ArrayList<>();

        try {
            String sql = "SELECT w.word_id, w.word, w.phonetic, wd.definition, w.translation_pos AS translation, " +
                    "w.created_at, w.updated_at, w.audio_url " +
                    "FROM words w " +
                    "LEFT JOIN word_definitions wd ON w.word_id = wd.word_id " +
                    "WHERE w.word LIKE ? OR wd.definition LIKE ? OR w.translation_pos LIKE ? " +
                    "ORDER BY w.word ASC LIMIT ? OFFSET ?";

            List<Object> params = new ArrayList<>();
            params.add("%" + query + "%");
            params.add("%" + query + "%");
            params.add("%" + query + "%");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            List<Map<String, Object>> words = jdbcTemplate.queryForList(sql, params.toArray());

            for (Map<String, Object> word : words) {
                SearchItem item = new SearchItem();
                Long wordId = ((Number) word.get("word_id")).longValue();

                item.setId("word_" + wordId);
                item.setType("vocabulary");
                item.setTitle((String) word.get("word"));
                item.setWord((String) word.get("word"));
                item.setPhonetic((String) word.get("phonetic"));
                item.setDefinition((String) word.get("definition"));
                item.setTranslation((String) word.get("translation"));

                // 生成摘要
                String definition = (String) word.get("definition");
                if (definition != null && definition.length() > 100) {
                    item.setExcerpt(definition.substring(0, 100) + "...");
                } else {
                    item.setExcerpt(definition);
                }

                // 计算相关性
                double relevance = calculateRelevance(query, (String) word.get("word"), definition);
                item.setRelevance(relevance);

                // 构建高亮信息
                Map<String, Object> highlight = new HashMap<>();
                highlight.put("word", highlightText(query, (String) word.get("word")));
                highlight.put("definition", highlightText(query, definition));
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

                results.add(item);
            }

        } catch (Exception e) {
            System.err.println("搜索词汇时发生错误: " + e.getMessage());
        }

        return results;
    }

    // 搜索笔记
    private List<SearchItem> searchNotes(String query, Long userId, int page, int pageSize) {
        List<SearchItem> results = new ArrayList<>();

        try {
            String sql = "SELECT n.note_id, n.document_id, n.content, n.created_at, n.updated_at, " +
                    "d.title as document_title " +
                    "FROM document_notes n " +
                    "LEFT JOIN documents d ON n.document_id = d.document_id " +
                    "WHERE n.content LIKE ? ";

            if (userId != null) {
                sql += "AND n.user_id = ? ";
            }

            sql += "ORDER BY n.created_at DESC LIMIT ? OFFSET ?";

            List<Object> params = new ArrayList<>();
            params.add("%" + query + "%");
            if (userId != null) {
                params.add(userId);
            }
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            List<Map<String, Object>> notes = jdbcTemplate.queryForList(sql, params.toArray());

            for (Map<String, Object> note : notes) {
                SearchItem item = new SearchItem();
                Long noteId = ((Number) note.get("note_id")).longValue();
                Long docId = ((Number) note.get("document_id")).longValue();

                item.setId("note_" + noteId);
                item.setType("note");
                item.setTitle("笔记 - " + note.get("document_title"));
                item.setContent((String) note.get("content"));
                item.setDocumentTitle((String) note.get("document_title"));
                item.setDocumentId(docId);

                // 生成摘要
                String content = (String) note.get("content");
                if (content != null && content.length() > 150) {
                    item.setExcerpt(content.substring(0, 150) + "...");
                } else {
                    item.setExcerpt(content);
                }

                // 计算相关性
                double relevance = calculateRelevance(query, content, content);
                item.setRelevance(relevance);

                // 构建高亮信息
                Map<String, Object> highlight = new HashMap<>();
                highlight.put("content", highlightText(query, content));
                item.setHighlight(highlight);

                item.setCreatedAt(note.get("created_at") != null ? note.get("created_at").toString() : null);
                item.setUpdatedAt(note.get("updated_at") != null ? note.get("updated_at").toString() : null);
                item.setUrl("/reader/note/" + noteId);

                results.add(item);
            }

        } catch (Exception e) {
            System.err.println("搜索笔记时发生错误: " + e.getMessage());
        }

        return results;
    }

    // 搜索高亮
    private List<SearchItem> searchHighlights(String query, Long userId, int page, int pageSize) {
        List<SearchItem> results = new ArrayList<>();

        try {
            String sql = "SELECT h.highlight_id, h.document_id, h.text, h.note, h.created_at, " +
                    "d.title as document_title " +
                    "FROM document_highlights h " +
                    "LEFT JOIN documents d ON h.document_id = d.document_id " +
                    "WHERE h.text LIKE ? OR h.note LIKE ? ";

            if (userId != null) {
                sql += "AND h.user_id = ? ";
            }

            sql += "ORDER BY h.created_at DESC LIMIT ? OFFSET ?";

            List<Object> params = new ArrayList<>();
            params.add("%" + query + "%");
            params.add("%" + query + "%");
            if (userId != null) {
                params.add(userId);
            }
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            List<Map<String, Object>> highlights = jdbcTemplate.queryForList(sql, params.toArray());

            for (Map<String, Object> highlight : highlights) {
                SearchItem item = new SearchItem();
                Long highlightId = ((Number) highlight.get("highlight_id")).longValue();
                Long docId = ((Number) highlight.get("document_id")).longValue();

                item.setId("highlight_" + highlightId);
                item.setType("highlight");
                item.setTitle("高亮 - " + highlight.get("document_title"));
                item.setSelectedText((String) highlight.get("text"));
                item.setNote((String) highlight.get("note"));
                item.setDocumentTitle((String) highlight.get("document_title"));
                item.setDocumentId(docId);

                // 生成摘要
                String text = (String) highlight.get("text");
                if (text != null && text.length() > 100) {
                    item.setExcerpt(text.substring(0, 100) + "...");
                } else {
                    item.setExcerpt(text);
                }

                // 计算相关性
                double relevance = calculateRelevance(query, text, text);
                item.setRelevance(relevance);

                // 构建高亮信息
                Map<String, Object> hl = new HashMap<>();
                hl.put("text", highlightText(query, text));
                item.setHighlight(hl);

                item.setCreatedAt(highlight.get("created_at") != null ? highlight.get("created_at").toString() : null);
                item.setUrl("/reader/highlight/" + highlightId);

                results.add(item);
            }

        } catch (Exception e) {
            System.err.println("搜索高亮时发生错误: " + e.getMessage());
        }

        return results;
    }

    // 保存搜索历史
    private void saveSearchHistory(Long userId, String keyword, String searchType, int resultCount) {
        try {
            String sql = "INSERT INTO search_history (user_id, keyword, search_type, result_count, timestamp) " +
                    "VALUES (?, ?, ?, ?, NOW())";
            jdbcTemplate.update(sql, userId, keyword, searchType, resultCount);
            System.out.println("已保存搜索历史: 用户=" + userId + ", 关键词=" + keyword);
        } catch (Exception e) {
            System.err.println("保存搜索历史失败: " + e.getMessage());
        }
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
            String sql = "SELECT t.tag_name FROM vocabulary_tags t " +
                    "INNER JOIN user_vocabulary_tags r ON t.tag_id = r.tag_id " +
                    "WHERE r.word_id = ?";
            List<Map<String, Object>> tagList = jdbcTemplate.queryForList(sql, wordId);
            for (Map<String, Object> tag : tagList) {
                tags.add((String) tag.get("tag_name"));
            }
        } catch (Exception e) {
            System.err.println("获取单词标签失败: " + e.getMessage());
        }
        return tags;
    }

    // 计算相关性（简单实现）
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

        // 简单高亮实现
        return text.replaceAll("(?i)(" + query + ")", "<mark>$1</mark>");
    }
}