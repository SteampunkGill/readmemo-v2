package com.vue.readingapp.review;

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

/**
 * 获取待复习单词的控制器
 * 
 * 负责根据艾宾浩斯遗忘曲线（间隔复习算法）筛选出用户当前需要复习的单词。
 */
@RestController
@RequestMapping("/api/v1/review")
public class ReviewGetDueWords {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 辅助方法：打印请求参数
     */
    private void printRequest(Object request) {
        System.out.println("=== 收到获取待复习单词请求 ===");
        System.out.println("请求参数: " + request);
        System.out.println("时间: " + LocalDateTime.now());
        System.out.println("========================");
    }

    /**
     * 辅助方法：打印数据库查询结果
     */
    private void printQueryResult(Object result) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("查询结果: " + result);
        System.out.println("===================");
    }

    /**
     * 辅助方法：打印返回响应
     */
    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    /**
     * 待复习单词列表响应结构
     */
    public static class DueWordsResponse {
        private boolean success;
        private String message;
        private DueWordsData data;

        public DueWordsResponse(boolean success, String message, DueWordsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public DueWordsData getData() { return data; }
        public void setData(DueWordsData data) { this.data = data; }
    }

    /**
     * 待复习单词的数据详情
     */
    public static class DueWordsData {
        private List<DueWord> words; // 单词列表
        private List<Map<String, Object>> distractorPool; // 干扰项池
        private int total;           // 生词本总单词数
        private int due_count;       // 当前到期需复习的单词总数

        public DueWordsData(List<DueWord> words, List<Map<String, Object>> distractorPool, int total, int due_count) {
            this.words = words;
            this.distractorPool = distractorPool;
            this.total = total;
            this.due_count = due_count;
        }

        public List<DueWord> getWords() { return words; }
        public void setWords(List<DueWord> words) { this.words = words; }

        public List<Map<String, Object>> getDistractorPool() { return distractorPool; }
        public void setDistractorPool(List<Map<String, Object>> distractorPool) { this.distractorPool = distractorPool; }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getDue_count() { return due_count; }
        public void setDue_count(int due_count) { this.due_count = due_count; }
    }

    /**
     * 单个待复习单词的详细信息
     */
    public static class DueWord {
        private int id;
        private String word;
        private String language;
        private String definition;
        private String example;
        private String phonetic;
        private String part_of_speech;
        private int mastery_level;    // 掌握程度 (0-5)
        private int review_count;     // 已复习次数
        private String last_reviewed_at;
        private String next_review_at;
        private String difficulty;    // 难度等级
        private List<String> tags;
        private String source;        // 来源
        private String due_reason;    // 到期原因 (如：已过期、今日到期)
        private int priority;         // 复习优先级

        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }

        public String getExample() { return example; }
        public void setExample(String example) { this.example = example; }

        public String getPhonetic() { return phonetic; }
        public void setPhonetic(String phonetic) { this.phonetic = phonetic; }

        public String getPart_of_speech() { return part_of_speech; }
        public void setPart_of_speech(String part_of_speech) { this.part_of_speech = part_of_speech; }

        public int getMastery_level() { return mastery_level; }
        public void setMastery_level(int mastery_level) { this.mastery_level = mastery_level; }

        public int getReview_count() { return review_count; }
        public void setReview_count(int review_count) { this.review_count = review_count; }

        public String getLast_reviewed_at() { return last_reviewed_at; }
        public void setLast_reviewed_at(String last_reviewed_at) { this.last_reviewed_at = last_reviewed_at; }

        public String getNext_review_at() { return next_review_at; }
        public void setNext_review_at(String next_review_at) { this.next_review_at = next_review_at; }

        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public String getDue_reason() { return due_reason; }
        public void setDue_reason(String due_reason) { this.due_reason = due_reason; }

        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
    }

    /**
     * 获取待复习单词列表接口
     * 
     * @param authHeader 认证头
     * @param limit 每次获取的数量限制
     * @param language 语言筛选
     * @param difficulty 难度筛选
     * @return 待复习单词列表及统计信息
     */
    @GetMapping("/due-words")
    public ResponseEntity<DueWordsResponse> getDueWords(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "tags", required = false) String tagsParam,
            @RequestParam(value = "difficulty", required = false) String difficulty,
            @RequestParam(value = "mode", defaultValue = "spaced") String mode,
            @RequestParam(value = "date", required = false) String date) {

        // 记录请求参数
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("limit", limit);
        requestParams.put("language", language);
        requestParams.put("tags", tagsParam);
        requestParams.put("difficulty", difficulty);
        requestParams.put("mode", mode);
        requestParams.put("date", date);
        printRequest(requestParams);

        try {
            // 1. 验证用户身份
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DueWordsResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DueWordsResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = ((Number) users.get(0).get("user_id")).intValue();

            // 2. 动态构建 SQL 查询语句
            StringBuilder sqlBuilder = new StringBuilder();
            List<Object> params = new ArrayList<>();

            sqlBuilder.append("SELECT uv.user_vocab_id, uv.word, uv.language, ");
            sqlBuilder.append("uv.definition, uv.example, uv.phonetic, w.part_of_speech, ");
            sqlBuilder.append("uv.mastery_level, uv.review_count, uv.last_reviewed_at, ");
            sqlBuilder.append("uv.next_review_at, w.difficulty, uv.source, ");
            // 计算到期原因：超过当前时间为“已过期”，24小时内到期为“今日到期”
            sqlBuilder.append("CASE ");
            sqlBuilder.append("  WHEN uv.next_review_at <= NOW() THEN 'overdue' ");
            sqlBuilder.append("  WHEN uv.next_review_at <= DATE_ADD(NOW(), INTERVAL 1 DAY) THEN 'due_today' ");
            sqlBuilder.append("  ELSE 'scheduled' ");
            sqlBuilder.append("END as due_reason, ");
            // 设置优先级：已过期的最优先复习
            sqlBuilder.append("CASE ");
            sqlBuilder.append("  WHEN uv.next_review_at <= NOW() THEN 1 ");
            sqlBuilder.append("  WHEN uv.next_review_at <= DATE_ADD(NOW(), INTERVAL 1 DAY) THEN 2 ");
            sqlBuilder.append("  ELSE 3 ");
            sqlBuilder.append("END as priority ");
            sqlBuilder.append("FROM user_vocabulary uv ");
            sqlBuilder.append("JOIN words w ON uv.word_id = w.word_id ");
            sqlBuilder.append("WHERE uv.user_id = ? ");
            // 修改为只复习今天收藏的单词
            sqlBuilder.append("AND DATE(uv.created_at) = CURDATE() ");
            // 依然排除已完全掌握的单词（可选，但通常今天收藏的不会是 mastered）
            sqlBuilder.append("AND uv.status != 'mastered' ");
            // 只复习有来源的单词（即阅读时收藏的单词）
            sqlBuilder.append("AND uv.source IS NOT NULL AND uv.source != '' ");

            params.add(userId);

            // 动态添加筛选条件
            if (language != null && !language.trim().isEmpty()) {
                sqlBuilder.append("AND uv.language = ? ");
                params.add(language);
            }

            if (difficulty != null && !difficulty.trim().isEmpty()) {
                sqlBuilder.append("AND w.difficulty = ? ");
                params.add(difficulty);
            }

            // 排序：按优先级升序，同优先级按到期时间升序
            sqlBuilder.append("ORDER BY priority ASC, uv.next_review_at ASC ");

            // 限制返回数量
            sqlBuilder.append("LIMIT ?");
            params.add(Math.min(limit, 100));

            // 3. 执行查询
            List<Map<String, Object>> dueWordsList = jdbcTemplate.queryForList(
                    sqlBuilder.toString(), params.toArray());

            // 4. 封装查询结果
            List<DueWord> dueWords = new ArrayList<>();
            for (Map<String, Object> row : dueWordsList) {
                DueWord dueWord = new DueWord();
                dueWord.setId(((Number) row.get("user_vocab_id")).intValue());
                dueWord.setWord((String) row.get("word"));
                dueWord.setLanguage((String) row.get("language"));
                dueWord.setDefinition((String) row.get("definition"));
                dueWord.setExample((String) row.get("example"));
                dueWord.setPhonetic((String) row.get("phonetic"));
                dueWord.setPart_of_speech((String) row.get("part_of_speech"));
                dueWord.setMastery_level(((Number) row.get("mastery_level")).intValue());
                dueWord.setReview_count(((Number) row.get("review_count")).intValue());

                if (row.get("last_reviewed_at") != null) {
                    dueWord.setLast_reviewed_at(row.get("last_reviewed_at").toString());
                }
                if (row.get("next_review_at") != null) {
                    dueWord.setNext_review_at(row.get("next_review_at").toString());
                }

                dueWord.setDifficulty((String) row.get("difficulty"));
                dueWord.setSource((String) row.get("source"));
                dueWord.setDue_reason((String) row.get("due_reason"));
                dueWord.setPriority(((Number) row.get("priority")).intValue());

                // 获取该单词关联的标签
                List<String> tags = getTagsForUserVocabulary(((Number) row.get("user_vocab_id")).intValue());
                dueWord.setTags(tags);

                dueWords.add(dueWord);
            }

            // 5. 获取干扰项池：从全局词典中随机抽取真实单词作为干扰项
            String distractorSql = "SELECT w.word_id as id, w.word, " +
                    "(SELECT definition FROM word_definitions WHERE word_id = w.word_id LIMIT 1) as definition, " +
                    "w.phonetic FROM words w " +
                    "WHERE w.word IS NOT NULL AND w.word != '' " +
                    "AND EXISTS (SELECT 1 FROM word_definitions WHERE word_id = w.word_id) " +
                    "ORDER BY RAND() LIMIT 50";
            List<Map<String, Object>> distractorPool = jdbcTemplate.queryForList(distractorSql);

            // 兜底：如果全局词典为空，从用户已有的生词本中获取干扰项
            if (distractorPool.isEmpty()) {
                String fallbackSql = "SELECT user_vocab_id as id, word, definition, phonetic FROM user_vocabulary " +
                        "WHERE definition IS NOT NULL AND definition != '' " +
                        "ORDER BY RAND() LIMIT 50";
                distractorPool = jdbcTemplate.queryForList(fallbackSql);
            }

            // 6. 获取全局统计数据
            // 统计生词本中今天收藏且未掌握的总数
            String countSql = "SELECT COUNT(*) as total FROM user_vocabulary WHERE user_id = ? AND DATE(created_at) = CURDATE() AND status != 'mastered' AND source IS NOT NULL AND source != ''";
            int total = jdbcTemplate.queryForObject(countSql, Integer.class, userId);

            // 统计当前已经到期（需立即复习）的数量 - 这里也改为今天收藏的数量
            String dueCountSql = "SELECT COUNT(*) as due_count FROM user_vocabulary " +
                    "WHERE user_id = ? AND DATE(created_at) = CURDATE() AND status != 'mastered' AND source IS NOT NULL AND source != ''";
            int dueCount = jdbcTemplate.queryForObject(dueCountSql, Integer.class, userId);

            // 7. 返回响应
            DueWordsData data = new DueWordsData(dueWords, distractorPool, total, dueCount);
            DueWordsResponse response = new DueWordsResponse(true, "获取待复习单词成功", data);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DueWordsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    /**
     * 辅助方法：获取某个生词的所有标签
     */
    private List<String> getTagsForUserVocabulary(int userVocabId) {
        try {
            String sql = "SELECT vt.tag_name " +
                    "FROM user_vocabulary_tags uvt " +
                    "JOIN vocabulary_tags vt ON uvt.tag_id = vt.tag_id " +
                    "WHERE uvt.user_vocab_id = ?";

            List<Map<String, Object>> tagsList = jdbcTemplate.queryForList(sql, userVocabId);

            return tagsList.stream()
                    .map(row -> (String) row.get("tag_name"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
