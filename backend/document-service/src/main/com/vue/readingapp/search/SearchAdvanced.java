package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchAdvanced {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到高级搜索请求 ===");
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
    public static class AdvancedSearchRequest {
        private Map<String, Object> filters;
        private String type = "all";
        private Integer page = 1;
        private Integer pageSize = 20;
        private String sortBy = "relevance";
        private String sortOrder = "desc";

        public Map<String, Object> getFilters() { return filters; }
        public void setFilters(Map<String, Object> filters) { this.filters = filters; }

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
    }

    // 响应DTO
    public static class AdvancedSearchResponse {
        private boolean success;
        private String message;
        private AdvancedSearchData data;

        public AdvancedSearchResponse(boolean success, String message, AdvancedSearchData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public AdvancedSearchData getData() { return data; }
        public void setData(AdvancedSearchData data) { this.data = data; }
    }

    public static class AdvancedSearchData {
        private int total;
        private int page;
        private int pageSize;
        private int totalPages;
        private List<AdvancedSearchItem> items;
        private Map<String, Object> facets;
        private long queryTime;
        private String type;

        public AdvancedSearchData(int total, int page, int pageSize, int totalPages,
                                  List<AdvancedSearchItem> items, Map<String, Object> facets,
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

        public List<AdvancedSearchItem> getItems() { return items; }
        public void setItems(List<AdvancedSearchItem> items) { this.items = items; }

        public Map<String, Object> getFacets() { return facets; }
        public void setFacets(Map<String, Object> facets) { this.facets = facets; }

        public long getQueryTime() { return queryTime; }
        public void setQueryTime(long queryTime) { this.queryTime = queryTime; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class AdvancedSearchItem {
        private String id;
        private String type;
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
        private String word;
        private String phonetic;
        private String definition;
        private String translation;
        private List<String> examples;
        private String audioUrl;
        private boolean inVocabulary;
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

        public boolean isInVocabulary() { return inVocabulary; }
        public void setInVocabulary(boolean inVocabulary) { this.inVocabulary = inVocabulary; }

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

    @PostMapping("/advanced")
    public ResponseEntity<AdvancedSearchResponse> advancedSearch(
            @RequestBody AdvancedSearchRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        long startTime = System.currentTimeMillis();

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证请求数据
            if (request.getFilters() == null || request.getFilters().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new AdvancedSearchResponse(false, "搜索过滤器不能为空", null)
                );
            }

            // 验证搜索类型
            List<String> validTypes = Arrays.asList("all", "documents", "vocabulary", "notes", "highlights");
            if (!validTypes.contains(request.getType())) {
                return ResponseEntity.badRequest().body(
                        new AdvancedSearchResponse(false, "无效的搜索类型，必须是: all, documents, vocabulary, notes, highlights", null)
                );
            }

            // 2. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);

            // 3. 根据类型执行搜索
            List<AdvancedSearchItem> allResults = new ArrayList<>();

            if ("all".equals(request.getType()) || "documents".equals(request.getType())) {
                List<AdvancedSearchItem> documentResults = searchDocumentsAdvanced(request.getFilters(), userId,
                        request.getPage(), request.getPageSize());
                allResults.addAll(documentResults);
            }

            if ("all".equals(request.getType()) || "vocabulary".equals(request.getType())) {
                List<AdvancedSearchItem> vocabularyResults = searchVocabularyAdvanced(request.getFilters(), userId,
                        request.getPage(), request.getPageSize());
                allResults.addAll(vocabularyResults);
            }

            if ("all".equals(request.getType()) || "notes".equals(request.getType())) {
                List<AdvancedSearchItem> noteResults = searchNotesAdvanced(request.getFilters(), userId,
                        request.getPage(), request.getPageSize());
                allResults.addAll(noteResults);
            }

            if ("all".equals(request.getType()) || "highlights".equals(request.getType())) {
                List<AdvancedSearchItem> highlightResults = searchHighlightsAdvanced(request.getFilters(), userId,
                        request.getPage(), request.getPageSize());
                allResults.addAll(highlightResults);
            }

            // 4. 排序结果
            if ("relevance".equals(request.getSortBy())) {
                allResults.sort((a, b) -> {
                    if ("desc".equalsIgnoreCase(request.getSortOrder())) {
                        return Double.compare(b.getRelevance(), a.getRelevance());
                    } else {
                        return Double.compare(a.getRelevance(), b.getRelevance());
                    }
                });
            } else if ("createdAt".equals(request.getSortBy())) {
                allResults.sort((a, b) -> {
                    if ("desc".equalsIgnoreCase(request.getSortOrder())) {
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    } else {
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    }
                });
            }

            // 5. 分页处理
            int startIndex = (request.getPage() - 1) * request.getPageSize();
            int endIndex = Math.min(startIndex + request.getPageSize(), allResults.size());
            List<AdvancedSearchItem> pagedResults = allResults.subList(startIndex, endIndex);

            // 6. 计算分页信息
            int totalPages = (int) Math.ceil((double) allResults.size() / request.getPageSize());

            // 7. 构建facet数据
            Map<String, Object> facets = new HashMap<>();

            // 8. 记录搜索历史
            if (userId != null) {
                String keyword = "高级搜索: " + request.getFilters().toString();
                saveSearchHistory(userId, keyword, "advanced", allResults.size());
            }

            // 9. 计算查询时间
            long queryTime = System.currentTimeMillis() - startTime;

            // 10. 准备响应数据
            AdvancedSearchData searchData = new AdvancedSearchData(
                    allResults.size(),
                    request.getPage(),
                    request.getPageSize(),
                    totalPages,
                    pagedResults,
                    facets,
                    queryTime,
                    request.getType()
            );

            AdvancedSearchResponse response = new AdvancedSearchResponse(true, "高级搜索成功", searchData);

            // 打印查询结果
            printQueryResult("找到 " + allResults.size() + " 个结果，返回 " + pagedResults.size() + " 个");

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("高级搜索过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new AdvancedSearchResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 高级文档搜索
    private List<AdvancedSearchItem> searchDocumentsAdvanced(Map<String, Object> filters, Long userId,
                                                             int page, int pageSize) {
        List<AdvancedSearchItem> results = new ArrayList<>();

        try {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT d.document_id, d.title, d.content, d.author, d.language, ");
            sqlBuilder.append("d.word_count, d.difficulty, d.created_at, d.updated_at, ");
            sqlBuilder.append("d.thumbnail_url, d.status ");
            sqlBuilder.append("FROM documents d ");
            sqlBuilder.append("WHERE d.status = 'processed' ");

            List<Object> params = new ArrayList<>();

            // 添加用户过滤
            if (userId != null) {
                sqlBuilder.append("AND d.user_id = ? ");
                params.add(userId);
            }

            // 处理各种过滤器
            if (filters.containsKey("query") && filters.get("query") != null) {
                String query = (String) filters.get("query");
                sqlBuilder.append("AND (d.title LIKE ? OR d.content LIKE ?) ");
                params.add("%" + query + "%");
                params.add("%" + query + "%");
            }

            if (filters.containsKey("language") && filters.get("language") != null) {
                String language = (String) filters.get("language");
                sqlBuilder.append("AND d.language = ? ");
                params.add(language);
            }

            if (filters.containsKey("difficulty") && filters.get("difficulty") != null) {
                String difficulty = (String) filters.get("difficulty");
                sqlBuilder.append("AND d.difficulty = ? ");
                params.add(difficulty);
            }

            if (filters.containsKey("minWordCount") && filters.get("minWordCount") != null) {
                Integer minWordCount = (Integer) filters.get("minWordCount");
                sqlBuilder.append("AND d.word_count >= ? ");
                params.add(minWordCount);
            }

            if (filters.containsKey("maxWordCount") && filters.get("maxWordCount") != null) {
                Integer maxWordCount = (Integer) filters.get("maxWordCount");
                sqlBuilder.append("AND d.word_count <= ? ");
                params.add(maxWordCount);
            }

            if (filters.containsKey("dateFrom") && filters.get("dateFrom") != null) {
                String dateFrom = (String) filters.get("dateFrom");
                sqlBuilder.append("AND DATE(d.created_at) >= ? ");
                params.add(dateFrom);
            }

            if (filters.containsKey("dateTo") && filters.get("dateTo") != null) {
                String dateTo = (String) filters.get("dateTo");
                sqlBuilder.append("AND DATE(d.created_at) <= ? ");
                params.add(dateTo);
            }

            if (filters.containsKey("tags") && filters.get("tags") != null) {
                List<String> tags = (List<String>) filters.get("tags");
                if (!tags.isEmpty()) {
                    sqlBuilder.append("AND EXISTS (SELECT 1 FROM document_tag_relations r ");
                    sqlBuilder.append("INNER JOIN document_tags t ON r.tag_id = t.tag_id ");
                    sqlBuilder.append("WHERE r.document_id = d.document_id AND t.tag_name IN (");

                    for (int i = 0; i < tags.size(); i++) {
                        if (i > 0) sqlBuilder.append(", ");
                        sqlBuilder.append("?");
                        params.add(tags.get(i));
                    }
                    sqlBuilder.append(")) ");
                }
            }

            // 添加排序和分页
            sqlBuilder.append("ORDER BY d.created_at DESC LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            List<Map<String, Object>> documents = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());

            for (Map<String, Object> doc : documents) {
                AdvancedSearchItem item = new AdvancedSearchItem();
                item.setId("doc_" + doc.get("document_id"));
                item.setType("document");
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

                // 计算相关性
                String query = filters.containsKey("query") ? (String) filters.get("query") : "";
                double relevance = calculateDocumentRelevance(query, (String) doc.get("title"), content);
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
            System.err.println("高级文档搜索时发生错误: " + e.getMessage());
        }

        return results;
    }

    // 高级词汇搜索
    private List<AdvancedSearchItem> searchVocabularyAdvanced(Map<String, Object> filters, Long userId,
                                                              int page, int pageSize) {
        List<AdvancedSearchItem> results = new ArrayList<>();

        try {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT w.word_id, w.word, w.phonetic, wd.definition, wd.translation, ");
            sqlBuilder.append("w.created_at, w.updated_at, w.audio_url, w.difficulty, w.language ");
            sqlBuilder.append("FROM words w ");
            sqlBuilder.append("LEFT JOIN word_definitions wd ON w.word_id = wd.word_id ");
            sqlBuilder.append("WHERE 1=1 ");

            List<Object> params = new ArrayList<>();

            // 处理各种过滤器
            if (filters.containsKey("query") && filters.get("query") != null) {
                String query = (String) filters.get("query");
                sqlBuilder.append("AND (w.word LIKE ? OR wd.definition LIKE ? OR wd.translation LIKE ?) ");
                params.add("%" + query + "%");
                params.add("%" + query + "%");
                params.add("%" + query + "%");
            }

            if (filters.containsKey("language") && filters.get("language") != null) {
                String language = (String) filters.get("language");
                sqlBuilder.append("AND w.language = ? ");
                params.add(language);
            }

            if (filters.containsKey("difficulty") && filters.get("difficulty") != null) {
                String difficulty = (String) filters.get("difficulty");
                sqlBuilder.append("AND w.difficulty = ? ");
                params.add(difficulty);
            }

            if (filters.containsKey("tags") && filters.get("tags") != null) {
                List<String> tags = (List<String>) filters.get("tags");
                if (!tags.isEmpty()) {
                    sqlBuilder.append("AND EXISTS (SELECT 1 FROM user_vocabulary_tags ut ");
                    sqlBuilder.append("INNER JOIN vocabulary_tags vt ON ut.tag_id = vt.tag_id ");
                    sqlBuilder.append("WHERE ut.word_id = w.word_id AND vt.tag_name IN (");

                    for (int i = 0; i < tags.size(); i++) {
                        if (i > 0) sqlBuilder.append(", ");
                        sqlBuilder.append("?");
                        params.add(tags.get(i));
                    }
                    sqlBuilder.append(")) ");
                }
            }

            // 添加排序和分页
            sqlBuilder.append("ORDER BY w.word ASC LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            List<Map<String, Object>> words = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());

            for (Map<String, Object> word : words) {
                AdvancedSearchItem item = new AdvancedSearchItem();
                Long wordId = ((Number) word.get("word_id")).longValue();

                item.setId("word_" + wordId);
                item.setType("vocabulary");
                item.setWord((String) word.get("word"));
                item.setPhonetic((String) word.get("phonetic"));
                item.setDefinition((String) word.get("definition"));
                item.setTranslation((String) word.get("translation"));
                item.setDifficulty((String) word.get("difficulty"));

                // 计算相关性
                String query = filters.containsKey("query") ? (String) filters.get("query") : "";
                double relevance = calculateVocabularyRelevance(query, (String) word.get("word"),
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

                results.add(item);
            }

        } catch (Exception e) {
            System.err.println("高级词汇搜索时发生错误: " + e.getMessage());
        }

        return results;
    }

    // 高级笔记搜索
    private List<AdvancedSearchItem> searchNotesAdvanced(Map<String, Object> filters, Long userId,
                                                         int page, int pageSize) {
        List<AdvancedSearchItem> results = new ArrayList<>();

        try {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT n.note_id, n.content, n.created_at, n.updated_at, ");
            sqlBuilder.append("d.document_id, d.title as document_title ");
            sqlBuilder.append("FROM document_notes n ");
            sqlBuilder.append("INNER JOIN documents d ON n.document_id = d.document_id ");
            sqlBuilder.append("WHERE n.user_id = ? ");

            List<Object> params = new ArrayList<>();
            params.add(userId);

            // 处理各种过滤器
            if (filters.containsKey("query") && filters.get("query") != null) {
                String query = (String) filters.get("query");
                sqlBuilder.append("AND n.content LIKE ? ");
                params.add("%" + query + "%");
            }

            if (filters.containsKey("documentId") && filters.get("documentId") != null) {
                Long documentId = Long.parseLong((String) filters.get("documentId"));
                sqlBuilder.append("AND n.document_id = ? ");
                params.add(documentId);
            }

            if (filters.containsKey("dateFrom") && filters.get("dateFrom") != null) {
                String dateFrom = (String) filters.get("dateFrom");
                sqlBuilder.append("AND DATE(n.created_at) >= ? ");
                params.add(dateFrom);
            }

            if (filters.containsKey("dateTo") && filters.get("dateTo") != null) {
                String dateTo = (String) filters.get("dateTo");
                sqlBuilder.append("AND DATE(n.created_at) <= ? ");
                params.add(dateTo);
            }

            // 添加排序和分页
            sqlBuilder.append("ORDER BY n.created_at DESC LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            List<Map<String, Object>> notes = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());

            for (Map<String, Object> note : notes) {
                AdvancedSearchItem item = new AdvancedSearchItem();
                Long noteId = ((Number) note.get("note_id")).longValue();
                Long docId = ((Number) note.get("document_id")).longValue();

                item.setId("note_" + noteId);
                item.setType("note");
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
                String query = filters.containsKey("query") ? (String) filters.get("query") : "";
                double relevance = calculateNoteRelevance(query, content);
                item.setRelevance(relevance);

                // 构建高亮信息
                Map<String, Object> highlight = new HashMap<>();
                highlight.put("content", highlightText(query, content));
                item.setHighlight(highlight);

                item.setCreatedAt(note.get("created_at") != null ? note.get("created_at").toString() : null);
                item.setUpdatedAt(note.get("updated_at") != null ? note.get("updated_at").toString() : null);
                item.setUrl("/reader/note/" + noteId + "?document=" + docId);

                results.add(item);
            }

        } catch (Exception e) {
            System.err.println("高级笔记搜索时发生错误: " + e.getMessage());
        }

        return results;
    }

    // 高级高亮搜索
    private List<AdvancedSearchItem> searchHighlightsAdvanced(Map<String, Object> filters, Long userId,
                                                              int page, int pageSize) {
        List<AdvancedSearchItem> results = new ArrayList<>();

        try {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT h.highlight_id, h.selected_text, h.note, h.created_at, ");
            sqlBuilder.append("h.page_number, d.document_id, d.title as document_title ");
            sqlBuilder.append("FROM document_highlights h ");
            sqlBuilder.append("INNER JOIN documents d ON h.document_id = d.document_id ");
            sqlBuilder.append("WHERE h.user_id = ? ");

            List<Object> params = new ArrayList<>();
            params.add(userId);

            // 处理各种过滤器
            if (filters.containsKey("query") && filters.get("query") != null) {
                String query = (String) filters.get("query");
                sqlBuilder.append("AND (h.selected_text LIKE ? OR h.note LIKE ?) ");
                params.add("%" + query + "%");
                params.add("%" + query + "%");
            }

            if (filters.containsKey("documentId") && filters.get("documentId") != null) {
                Long documentId = Long.parseLong((String) filters.get("documentId"));
                sqlBuilder.append("AND h.document_id = ? ");
                params.add(documentId);
            }

            if (filters.containsKey("dateFrom") && filters.get("dateFrom") != null) {
                String dateFrom = (String) filters.get("dateFrom");
                sqlBuilder.append("AND DATE(h.created_at) >= ? ");
                params.add(dateFrom);
            }

            if (filters.containsKey("dateTo") && filters.get("dateTo") != null) {
                String dateTo = (String) filters.get("dateTo");
                sqlBuilder.append("AND DATE(h.created_at) <= ? ");
                params.add(dateTo);
            }

            // 添加排序和分页
            sqlBuilder.append("ORDER BY h.created_at DESC LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            List<Map<String, Object>> highlights = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());

            for (Map<String, Object> highlight : highlights) {
                AdvancedSearchItem item = new AdvancedSearchItem();
                Long highlightId = ((Number) highlight.get("highlight_id")).longValue();
                Long docId = ((Number) highlight.get("document_id")).longValue();

                item.setId("highlight_" + highlightId);
                item.setType("highlight");
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
                String query = filters.containsKey("query") ? (String) filters.get("query") : "";
                double relevance = calculateHighlightRelevance(query, selectedText, noteText);
                item.setRelevance(relevance);

                // 构建高亮信息
                Map<String, Object> hl = new HashMap<>();
                hl.put("selectedText", highlightText(query, selectedText));
                hl.put("note", highlightText(query, noteText));
                item.setHighlight(hl);

                item.setCreatedAt(highlight.get("created_at") != null ?
                        highlight.get("created_at").toString() : null);
                item.setUrl("/reader/highlight/" + highlightId + "?document=" + docId);

                results.add(item);
            }

        } catch (Exception e) {
            System.err.println("高级高亮搜索时发生错误: " + e.getMessage());
        }

        return results;
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

    // 计算文档相关性
    private double calculateDocumentRelevance(String query, String title, String content) {
        if (title == null) title = "";
        if (content == null) content = "";

        String lowerQuery = query.toLowerCase();
        String lowerTitle = title.toLowerCase();
        String lowerContent = content.toLowerCase();

        double score = 0.0;

        if (lowerTitle.contains(lowerQuery)) {
            score += 0.7;
        }

        if (lowerContent.contains(lowerQuery)) {
            score += 0.3;
        }

        if (title.contains(query) || content.contains(query)) {
            score += 0.2;
        }

        return Math.min(score, 1.0);
    }

    // 计算词汇相关性
    private double calculateVocabularyRelevance(String query, String word, String definition) {
        if (word == null) word = "";
        if (definition == null) definition = "";

        String lowerQuery = query.toLowerCase();
        String lowerWord = word.toLowerCase();
        String lowerDefinition = definition.toLowerCase();

        double score = 0.0;

        if (lowerWord.equals(lowerQuery)) {
            score += 0.8;
        } else if (lowerWord.startsWith(lowerQuery)) {
            score += 0.6;
        } else if (lowerWord.contains(lowerQuery)) {
            score += 0.4;
        }

        if (lowerDefinition.contains(lowerQuery)) {
            score += 0.2;
        }

        return Math.min(score, 1.0);
    }

    // 计算笔记相关性
    private double calculateNoteRelevance(String query, String content) {
        if (content == null) content = "";

        String lowerQuery = query.toLowerCase();
        String lowerContent = content.toLowerCase();

        double score = 0.0;

        if (lowerContent.contains(lowerQuery)) {
            score += 1.0;
        }

        if (content.contains(query)) {
            score += 0.3;
        }

        return Math.min(score, 1.0);
    }

    // 计算高亮相关性
    private double calculateHighlightRelevance(String query, String selectedText, String note) {
        if (selectedText == null) selectedText = "";
        if (note == null) note = "";

        String lowerQuery = query.toLowerCase();
        String lowerSelectedText = selectedText.toLowerCase();
        String lowerNote = note.toLowerCase();

        double score = 0.0;

        if (lowerSelectedText.contains(lowerQuery)) {
            score += 0.7;
        }

        if (lowerNote.contains(lowerQuery)) {
            score += 0.3;
        }

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
            String sql = "SELECT example_text FROM word_examples WHERE word_id = ? LIMIT 3";
            List<Map<String, Object>> exampleList = jdbcTemplate.queryForList(sql, wordId);
            for (Map<String, Object> example : exampleList) {
                examples.add((String) example.get("example_text"));
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
                    "WHERE ut.word_id = ?";
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

    // 保存搜索历史
    private void saveSearchHistory(Long userId, String keyword, String searchType, int resultCount) {
        try {
            String sql = "INSERT INTO search_history (user_id, keyword, search_type, result_count, timestamp) " +
                    "VALUES (?, ?, ?, ?, NOW())";
            jdbcTemplate.update(sql, userId, keyword, searchType, resultCount);
            System.out.println("已保存高级搜索历史: 用户=" + userId + ", 关键词=" + keyword);
        } catch (Exception e) {
            System.err.println("保存搜索历史失败: " + e.getMessage());
        }
    }
}