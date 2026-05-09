package com.vue.readingapp.vocabulary;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/words")
public class VocabularyGetSimilarWords {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取相似单词请求 ===");
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
    public static class SimilarWordsResponse {
        private boolean success;
        private String message;
        private SimilarWordsData data;

        public SimilarWordsResponse(boolean success, String message, SimilarWordsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SimilarWordsData getData() { return data; }
        public void setData(SimilarWordsData data) { this.data = data; }
    }

    public static class SimilarWordsData {
        private String word;
        private String language;
        private List<SimilarWord> similarWords;

        public SimilarWordsData(String word, String language, List<SimilarWord> similarWords) {
            this.word = word;
            this.language = language;
            this.similarWords = similarWords;
        }

        // Getters and Setters
        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public List<SimilarWord> getSimilarWords() { return similarWords; }
        public void setSimilarWords(List<SimilarWord> similarWords) { this.similarWords = similarWords; }
    }

    public static class SimilarWord {
        private String word;
        private String language;
        private String relationType;
        private String definition;

        public SimilarWord(String word, String language, String relationType, String definition) {
            this.word = word;
            this.language = language;
            this.relationType = relationType;
            this.definition = definition;
        }

        // Getters and Setters
        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getRelationType() { return relationType; }
        public void setRelationType(String relationType) { this.relationType = relationType; }

        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }
    }

    @GetMapping("/similar")
    public ResponseEntity<SimilarWordsResponse> getSimilarWords(
            @RequestParam String word,
            @RequestParam(required = false, defaultValue = "en") String language) {

        Map<String, String> requestData = new HashMap<>();
        requestData.put("word", word);
        requestData.put("language", language);
        printRequest(requestData);

        try {
            // 1. 验证请求数据
            if (word == null || word.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SimilarWordsResponse(false, "单词不能为空", null)
                );
            }

            String normalizedWord = word.trim().toLowerCase();

            // 2. 查询单词ID
            String wordSql = "SELECT word_id FROM words WHERE word = ? AND language = ?";
            List<Map<String, Object>> words = jdbcTemplate.queryForList(wordSql, normalizedWord, language);

            if (words.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new SimilarWordsResponse(false, "未找到单词: " + normalizedWord, null)
                );
            }

            Long wordId = ((Number) words.get(0).get("word_id")).longValue();

            // 3. 查询相似单词（同义词、近义词等）
            String similarSql = "SELECT w.word, w.language, wr.relation_type, wd.definition " +
                    "FROM word_relations wr " +
                    "JOIN words w ON wr.related_word_id = w.word_id " +
                    "LEFT JOIN word_definitions wd ON w.word_id = wd.word_id AND wd.definition_id = (SELECT MIN(definition_id) FROM word_definitions WHERE word_id = w.word_id) " +
                    "WHERE wr.word_id = ? AND wr.relation_type IN ('synonym', 'related') " +
                    "ORDER BY wr.relation_type, w.word " +
                    "LIMIT 20";

            List<Map<String, Object>> similarResults = jdbcTemplate.queryForList(similarSql, wordId);
            printQueryResult(similarResults);

            // 4. 组装数据
            List<SimilarWord> similarWords = new ArrayList<>();
            for (Map<String, Object> row : similarResults) {
                String similarWord = (String) row.get("word");
                String similarLanguage = (String) row.get("language");
                String relationType = (String) row.get("relation_type");
                String definition = (String) row.get("definition");

                similarWords.add(new SimilarWord(similarWord, similarLanguage, relationType, definition));
            }

            // 5. 创建响应数据
            SimilarWordsData data = new SimilarWordsData(normalizedWord, language, similarWords);
            SimilarWordsResponse response = new SimilarWordsResponse(true, "获取相似单词成功", data);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取相似单词过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SimilarWordsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
