package com.vue.readingapp.vocabulary;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/vocabulary")
public class VocabularySearch {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到搜索生词本请求 ===");
        System.out.println("请求参数: " + request);
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
    public static class SearchVocabularyResponse {
        private boolean success;
        private String message;
        private SearchResultData data;

        public SearchVocabularyResponse(boolean success, String message, SearchResultData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SearchResultData getData() { return data; }
        public void setData(SearchResultData data) { this.data = data; }
    }

    public static class SearchResultData {
        private List<VocabularyItemData> items;
        private int total;
        private int page;
        private int pageSize;
        private int totalPages;

        public SearchResultData(List<VocabularyItemData> items, int total, int page, int pageSize) {
            this.items = items;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
            this.totalPages = (int) Math.ceil((double) total / pageSize);
        }

        // Getters and Setters
        public List<VocabularyItemData> getItems() { return items; }
        public void setItems(List<VocabularyItemData> items) { this.items = items; }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }

    public static class VocabularyItemData {
        private Long id;
        private String word;
        private String language;
        private String definition;
        private String example;
        private List<String> tags;
        private String notes;
        private String status;
        private Integer masteryLevel;
        private Integer reviewCount;
        private String lastReviewedAt;
        private String nextReviewAt;
        private String source;
        private Integer sourcePage;
        private String createdAt;
        private String updatedAt;

        public VocabularyItemData() {
            this.tags = new ArrayList<>();
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }

        public String getExample() { return example; }
        public void setExample(String example) { this.example = example; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Integer getMasteryLevel() { return masteryLevel; }
        public void setMasteryLevel(Integer masteryLevel) { this.masteryLevel = masteryLevel; }

        public Integer getReviewCount() { return reviewCount; }
        public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

        public String getLastReviewedAt() { return lastReviewedAt; }
        public void setLastReviewedAt(String lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }

        public String getNextReviewAt() { return nextReviewAt; }
        public void setNextReviewAt(String nextReviewAt) { this.nextReviewAt = nextReviewAt; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public Integer getSourcePage() { return sourcePage; }
        public void setSourcePage(Integer sourcePage) { this.sourcePage = sourcePage; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    @GetMapping("/search")
    public ResponseEntity<SearchVocabularyResponse> searchVocabulary(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "50") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false, defaultValue = "created_at") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {

        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("query", query);
        requestParams.put("page", page);
        requestParams.put("pageSize", pageSize);
        requestParams.put("status", status);
        requestParams.put("language", language);
        requestParams.put("tags", tags);
        requestParams.put("sortBy", sortBy);
        requestParams.put("sortOrder", sortOrder);
        printRequest(requestParams);

        try {
            // 1. 验证用户身份
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SearchVocabularyResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > ?";
            LocalDateTime now = LocalDateTime.now();
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(userSql, token, now);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SearchVocabularyResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Long userId = ((Number) sessions.get(0).get("user_id")).longValue();

            // 2. 验证参数
            if (page < 1) page = 1;
            if (pageSize < 1 || pageSize > 200) pageSize = 50;
            if (!Arrays.asList("asc", "desc").contains(sortOrder.toLowerCase())) {
                sortOrder = "desc";
            }

            // 3. 构建查询条件
            StringBuilder whereClause = new StringBuilder("WHERE uv.user_id = ?");
            List<Object> params = new ArrayList<>();
            params.add(userId);

            if (query != null && !query.trim().isEmpty()) {
                whereClause.append(" AND (uv.word LIKE ? OR uv.definition LIKE ? OR uv.notes LIKE ?)");
                String searchPattern = "%" + query.trim() + "%";
                params.add(searchPattern);
                params.add(searchPattern);
                params.add(searchPattern);
            }

            if (status != null && !status.trim().isEmpty()) {
                whereClause.append(" AND uv.status = ?");
                params.add(status.trim());
            }

            if (language != null && !language.trim().isEmpty()) {
                whereClause.append(" AND uv.language = ?");
                params.add(language.trim());
            }

            // 4. 查询总数
            String countSql = "SELECT COUNT(DISTINCT uv.user_vocab_id) as total " +
                    "FROM user_vocabulary uv " +
                    "LEFT JOIN user_vocabulary_tags uvt ON uv.user_vocab_id = uvt.user_vocab_id " +
                    "LEFT JOIN vocabulary_tags vt ON uvt.tag_id = vt.tag_id " +
                    whereClause.toString();

            if (tags != null && !tags.isEmpty()) {
                countSql += " AND vt.tag_name IN (" + String.join(",", Collections.nCopies(tags.size(), "?")) + ")";
                params.addAll(tags);
            }

            Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

            // 5. 查询数据
            String dataSql = "SELECT uv.user_vocab_id, uv.word, uv.language, uv.definition, uv.example, uv.notes, " +
                    "uv.status, uv.mastery_level, uv.review_count, uv.last_reviewed_at, uv.next_review_at, " +
                    "uv.source, uv.source_page, uv.created_at, uv.updated_at, " +
                    "GROUP_CONCAT(DISTINCT vt.tag_name) as tags " +
                    "FROM user_vocabulary uv " +
                    "LEFT JOIN user_vocabulary_tags uvt ON uv.user_vocab_id = uvt.user_vocab_id " +
                    "LEFT JOIN vocabulary_tags vt ON uvt.tag_id = vt.tag_id " +
                    whereClause.toString();

            if (tags != null && !tags.isEmpty()) {
                dataSql += " AND vt.tag_name IN (" + String.join(",", Collections.nCopies(tags.size(), "?")) + ")";
            }

            dataSql += " GROUP BY uv.user_vocab_id";

            // 添加排序
            String validSortBy = Arrays.asList("created_at", "updated_at", "word", "status", "mastery_level", "review_count")
                    .contains(sortBy) ? sortBy : "created_at";
            dataSql += " ORDER BY " + validSortBy + " " + sortOrder;

            // 添加分页
            int offset = (page - 1) * pageSize;
            dataSql += " LIMIT ? OFFSET ?";
            params.add(pageSize);
            params.add(offset);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(dataSql, params.toArray());
            printQueryResult("搜索到 " + results.size() + " 条记录，总数: " + total);

            // 6. 组装数据
            List<VocabularyItemData> items = new ArrayList<>();
            for (Map<String, Object> row : results) {
                VocabularyItemData item = new VocabularyItemData();
                item.setId(((Number) row.get("user_vocab_id")).longValue());
                item.setWord((String) row.get("word"));
                item.setLanguage((String) row.get("language"));
                item.setDefinition((String) row.get("definition"));
                item.setExample((String) row.get("example"));
                item.setNotes((String) row.get("notes"));
                item.setStatus((String) row.get("status"));
                item.setMasteryLevel(row.get("mastery_level") != null ? ((Number) row.get("mastery_level")).intValue() : 0);
                item.setReviewCount(row.get("review_count") != null ? ((Number) row.get("review_count")).intValue() : 0);
                item.setLastReviewedAt(row.get("last_reviewed_at") != null ? row.get("last_reviewed_at").toString() : null);
                item.setNextReviewAt(row.get("next_review_at") != null ? row.get("next_review_at").toString() : null);
                item.setSource((String) row.get("source"));
                item.setSourcePage(row.get("source_page") != null ? ((Number) row.get("source_page")).intValue() : null);
                item.setCreatedAt(row.get("created_at").toString());
                item.setUpdatedAt(row.get("updated_at").toString());

                // 处理标签
                String tagsStr = (String) row.get("tags");
                if (tagsStr != null && !tagsStr.isEmpty()) {
                    item.setTags(Arrays.asList(tagsStr.split(",")));
                }

                items.add(item);
            }

            // 7. 创建结果数据
            SearchResultData resultData = new SearchResultData(items, total.intValue(), page, pageSize);
            SearchVocabularyResponse response = new SearchVocabularyResponse(true, "搜索成功", resultData);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("搜索生词本过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SearchVocabularyResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
