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
public class VocabularyGetList {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取生词本列表请求 ===");
        System.out.println("请求参数: " + request);
        System.out.println("==========================");
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
    public static class VocabularyListResponse {
        private boolean success;
        private String message;
        private VocabularyListData data;

        public VocabularyListResponse(boolean success, String message, VocabularyListData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public VocabularyListData getData() { return data; }
        public void setData(VocabularyListData data) { this.data = data; }
    }

    public static class VocabularyListData {
        private List<VocabularyItemData> items;
        private PaginationData pagination;

        public VocabularyListData(List<VocabularyItemData> items, PaginationData pagination) {
            this.items = items;
            this.pagination = pagination;
        }

        public List<VocabularyItemData> getItems() { return items; }
        public void setItems(List<VocabularyItemData> items) { this.items = items; }

        public PaginationData getPagination() { return pagination; }
        public void setPagination(PaginationData pagination) { this.pagination = pagination; }
    }

    public static class VocabularyItemData {
        private Long id;
        private String word;
        private String phonetic;
        private String definition;
        private String exampleSentence;
        private String partOfSpeech;
        private String difficulty;
        private List<String> tags;
        private String status;
        private String lastReviewedAt;

        public VocabularyItemData() {
            this.tags = new ArrayList<>();
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getPhonetic() { return phonetic; }
        public void setPhonetic(String phonetic) { this.phonetic = phonetic; }

        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }

        public String getExampleSentence() { return exampleSentence; }
        public void setExampleSentence(String exampleSentence) { this.exampleSentence = exampleSentence; }

        public String getPartOfSpeech() { return partOfSpeech; }
        public void setPartOfSpeech(String partOfSpeech) { this.partOfSpeech = partOfSpeech; }

        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getLastReviewedAt() { return lastReviewedAt; }
        public void setLastReviewedAt(String lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }
    }

    public static class PaginationData {
        private int page;
        private int pageSize;
        private long total;
        private int totalPages;

        public PaginationData(int page, int pageSize, long total) {
            this.page = page;
            this.pageSize = pageSize;
            this.total = total;
            this.totalPages = (int) Math.ceil((double) total / pageSize);
        }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }

    @GetMapping("")
    public ResponseEntity<VocabularyListResponse> getVocabularyList(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "50") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String language,
            @RequestParam(required = false, defaultValue = "created_at") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String search) {

        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("page", page);
        requestParams.put("pageSize", pageSize);
        requestParams.put("status", status);
        requestParams.put("tags", tags);
        requestParams.put("language", language);
        requestParams.put("sortBy", sortBy);
        requestParams.put("sortOrder", sortOrder);
        requestParams.put("search", search);
        printRequest(requestParams);

        try {
            // 1. 验证用户身份
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new VocabularyListResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > ?";
            LocalDateTime now = LocalDateTime.now();
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(userSql, token, now);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new VocabularyListResponse(false, "登录已过期，请重新登录", null)
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

            if (status != null && !status.trim().isEmpty()) {
                whereClause.append(" AND uv.status = ?");
                params.add(status.trim());
            }

            if (language != null && !language.trim().isEmpty()) {
                whereClause.append(" AND uv.language = ?");
                params.add(language.trim());
            }

            if (search != null && !search.trim().isEmpty()) {
                whereClause.append(" AND (uv.word LIKE ? OR uv.definition LIKE ? OR uv.notes LIKE ?)");
                String searchPattern = "%" + search.trim() + "%";
                params.add(searchPattern);
                params.add(searchPattern);
                params.add(searchPattern);
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
            String dataSql = "SELECT uv.user_vocab_id, uv.word, w.phonetic, uv.definition, uv.example as example_sentence, " +
                    "w.part_of_speech, w.difficulty, uv.status, uv.last_reviewed_at, " +
                    "GROUP_CONCAT(DISTINCT vt.tag_name) as tags " +
                    "FROM user_vocabulary uv " +
                    "LEFT JOIN words w ON uv.word_id = w.word_id " +
                    "LEFT JOIN user_vocabulary_tags uvt ON uv.user_vocab_id = uvt.user_vocab_id " +
                    "LEFT JOIN vocabulary_tags vt ON uvt.tag_id = vt.tag_id " +
                    whereClause.toString();

            if (tags != null && !tags.isEmpty()) {
                dataSql += " AND vt.tag_name IN (" + String.join(",", Collections.nCopies(tags.size(), "?")) + ")";
            }

            dataSql += " GROUP BY uv.user_vocab_id";

            // 添加排序
            String validSortBy = Arrays.asList("created_at", "updated_at", "word", "status", "mastery_level")
                    .contains(sortBy) ? "uv." + sortBy : "uv.created_at";
            dataSql += " ORDER BY " + validSortBy + " " + sortOrder;

            // 添加分页
            int offset = (page - 1) * pageSize;
            dataSql += " LIMIT ? OFFSET ?";
            params.add(pageSize);
            params.add(offset);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(dataSql, params.toArray());
            printQueryResult("查询到 " + results.size() + " 条记录，总数: " + total);

            // 6. 组装数据
            List<VocabularyItemData> items = new ArrayList<>();
            for (Map<String, Object> row : results) {
                VocabularyItemData item = new VocabularyItemData();
                item.setId(((Number) row.get("user_vocab_id")).longValue());
                item.setWord((String) row.get("word"));
                item.setPhonetic((String) row.get("phonetic"));
                item.setDefinition((String) row.get("definition"));
                item.setExampleSentence((String) row.get("example_sentence"));
                item.setPartOfSpeech((String) row.get("part_of_speech"));
                item.setDifficulty((String) row.get("difficulty"));
                item.setStatus((String) row.get("status"));
                item.setLastReviewedAt(row.get("last_reviewed_at") != null ? row.get("last_reviewed_at").toString() : null);

                // 处理标签
                String tagsStr = (String) row.get("tags");
                if (tagsStr != null && !tagsStr.isEmpty()) {
                    item.setTags(Arrays.asList(tagsStr.split(",")));
                }

                items.add(item);
            }

            // 7. 创建分页数据
            PaginationData pagination = new PaginationData(page, pageSize, total);

            // 8. 创建响应
            VocabularyListData listData = new VocabularyListData(items, pagination);
            VocabularyListResponse response = new VocabularyListResponse(true, "获取生词本列表成功", listData);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取生词本列表过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new VocabularyListResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}