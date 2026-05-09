
package com.vue.readingapp.review;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/review")
public class ReviewGetSmartWords {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到获取智能复习单词请求 ===");
        System.out.println("请求参数: " + request);
        System.out.println("时间: " + LocalDateTime.now());
        System.out.println("==========================");
    }

    private void printQueryResult(Object result) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("查询结果: " + result);
        System.out.println("===================");
    }

    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    public static class SmartWordsResponse {
        private boolean success;
        private String message;
        private SmartWordsData data;

        public SmartWordsResponse(boolean success, String message, SmartWordsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SmartWordsData getData() { return data; }
        public void setData(SmartWordsData data) { this.data = data; }
    }

    public static class SmartWordsData {
        private List<SmartWord> words;
        private String algorithm;
        private int total_recommended;

        public SmartWordsData(List<SmartWord> words, String algorithm, int total_recommended) {
            this.words = words;
            this.algorithm = algorithm;
            this.total_recommended = total_recommended;
        }

        public List<SmartWord> getWords() { return words; }
        public void setWords(List<SmartWord> words) { this.words = words; }

        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

        public int getTotal_recommended() { return total_recommended; }
        public void setTotal_recommended(int total_recommended) { this.total_recommended = total_recommended; }
    }

    public static class SmartWord {
        private int id;
        private String word;
        private String language;
        private String definition;
        private String example;
        private String phonetic;
        private String part_of_speech;
        private int mastery_level;
        private int review_count;
        private String last_reviewed_at;
        private String next_review_at;
        private String difficulty;
        private List<String> tags;
        private String source;
        private String algorithm;
        private double confidence;
        private int recommended_order;

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

        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public int getRecommended_order() { return recommended_order; }
        public void setRecommended_order(int recommended_order) { this.recommended_order = recommended_order; }
    }

    @GetMapping("/smart-words")
    public ResponseEntity<SmartWordsResponse> getSmartWords(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "algorithm", defaultValue = "spaced_repetition") String algorithm) {

        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("limit", limit);
        requestParams.put("language", language);
        requestParams.put("algorithm", algorithm);
        printRequest(requestParams);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SmartWordsResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SmartWordsResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) users.get(0).get("user_id");
            System.out.println("当前用户ID: " + userId);

            // 2. 构建查询条件
            StringBuilder sqlBuilder = new StringBuilder();
            List<Object> params = new ArrayList<>();

            sqlBuilder.append("SELECT uv.user_vocab_id, w.word, w.language, ");
            sqlBuilder.append("wd.definition, we.example, w.phonetic, w.part_of_speech, ");
            sqlBuilder.append("uv.mastery_level, uv.review_count, uv.last_reviewed_at, ");
            sqlBuilder.append("uv.next_review_date, w.difficulty, uv.source, ");
            sqlBuilder.append("CASE ");
            sqlBuilder.append("  WHEN uv.mastery_level <= 2 THEN 0.9 ");
            sqlBuilder.append("  WHEN uv.mastery_level <= 4 THEN 0.7 ");
            sqlBuilder.append("  ELSE 0.5 ");
            sqlBuilder.append("END as confidence, ");
            sqlBuilder.append("ROW_NUMBER() OVER (ORDER BY ");

            // 根据算法选择排序方式
            switch (algorithm) {
                case "adaptive":
                    sqlBuilder.append("uv.mastery_level ASC, uv.review_count ASC, uv.next_review_date ASC");
                    break;
                case "random":
                    sqlBuilder.append("RAND()");
                    break;
                case "spaced_repetition":
                default:
                    sqlBuilder.append("uv.next_review_date ASC, uv.mastery_level ASC");
                    break;
            }

            sqlBuilder.append(") as recommended_order ");
            sqlBuilder.append("FROM user_vocabulary uv ");
            sqlBuilder.append("JOIN words w ON uv.word_id = w.word_id ");
            sqlBuilder.append("LEFT JOIN word_definitions wd ON w.word_id = wd.word_id ");
            sqlBuilder.append("LEFT JOIN word_examples we ON w.word_id = we.word_id ");
            sqlBuilder.append("WHERE uv.user_id = ? ");
            // 修改为只复习今天收藏的单词
            sqlBuilder.append("AND DATE(uv.created_at) = CURDATE() ");
            sqlBuilder.append("AND uv.status != 'mastered' ");
            sqlBuilder.append("AND uv.source IS NOT NULL AND uv.source != '' "); // 只复习收藏的词

            params.add(userId);

            if (language != null && !language.trim().isEmpty()) {
                sqlBuilder.append("AND w.language = ? ");
                params.add(language);
            }

            sqlBuilder.append("ORDER BY recommended_order ");
            sqlBuilder.append("LIMIT ?");
            params.add(Math.min(limit, 50));

            // 3. 执行查询
            System.out.println("执行SQL: " + sqlBuilder.toString());
            System.out.println("参数: " + params);

            List<Map<String, Object>> smartWordsList = jdbcTemplate.queryForList(
                    sqlBuilder.toString(), params.toArray());

            printQueryResult("查询到 " + smartWordsList.size() + " 个智能复习单词");

            // 4. 处理查询结果
            List<SmartWord> smartWords = new ArrayList<>();
            int order = 1;

            for (Map<String, Object> row : smartWordsList) {
                SmartWord smartWord = new SmartWord();
                smartWord.setId((int) row.get("user_vocab_id"));
                smartWord.setWord((String) row.get("word"));
                smartWord.setLanguage((String) row.get("language"));
                smartWord.setDefinition((String) row.get("definition"));
                smartWord.setExample((String) row.get("example"));
                smartWord.setPhonetic((String) row.get("phonetic"));
                smartWord.setPart_of_speech((String) row.get("part_of_speech"));
                smartWord.setMastery_level((int) row.get("mastery_level"));
                smartWord.setReview_count((int) row.get("review_count"));

                if (row.get("last_reviewed_at") != null) {
                    smartWord.setLast_reviewed_at(row.get("last_reviewed_at").toString());
                }

                if (row.get("next_review_date") != null) {
                    smartWord.setNext_review_at(row.get("next_review_date").toString());
                }

                smartWord.setDifficulty((String) row.get("difficulty"));
                smartWord.setSource((String) row.get("source"));
                smartWord.setAlgorithm(algorithm);
                smartWord.setConfidence((double) row.get("confidence"));
                smartWord.setRecommended_order(order++);

                // 获取标签
                List<String> tags = getTagsForUserVocabulary((int) row.get("user_vocab_id"));
                smartWord.setTags(tags);

                smartWords.add(smartWord);
            }

            // 5. 构建响应数据
            SmartWordsData data = new SmartWordsData(smartWords, algorithm, smartWords.size());
            SmartWordsResponse response = new SmartWordsResponse(true, "获取智能复习单词成功", data);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取智能复习单词过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SmartWordsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

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
            System.err.println("获取标签失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}