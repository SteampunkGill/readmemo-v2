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
public class VocabularyGetWordExamples {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取单词例句请求 ===");
        System.out.println("请求数据: " + request);
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
    public static class WordExamplesResponse {
        private boolean success;
        private String message;
        private WordExamplesData data;

        public WordExamplesResponse(boolean success, String message, WordExamplesData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public WordExamplesData getData() { return data; }
        public void setData(WordExamplesData data) { this.data = data; }
    }

    public static class WordExamplesData {
        private String word;
        private String language;
        private List<Example> examples;

        public WordExamplesData(String word, String language, List<Example> examples) {
            this.word = word;
            this.language = language;
            this.examples = examples;
        }

        // Getters and Setters
        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public List<Example> getExamples() { return examples; }
        public void setExamples(List<Example> examples) { this.examples = examples; }
    }

    public static class Example {
        private Long exampleId;
        private String example;
        private String translation;
        private String source;

        public Example(Long exampleId, String example, String translation, String source) {
            this.exampleId = exampleId;
            this.example = example;
            this.translation = translation;
            this.source = source;
        }

        // Getters and Setters
        public Long getExampleId() { return exampleId; }
        public void setExampleId(Long exampleId) { this.exampleId = exampleId; }

        public String getExample() { return example; }
        public void setExample(String example) { this.example = example; }

        public String getTranslation() { return translation; }
        public void setTranslation(String translation) { this.translation = translation; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }

    @GetMapping("/examples")
    public ResponseEntity<WordExamplesResponse> getWordExamples(
            @RequestParam String word,
            @RequestParam(required = false, defaultValue = "en") String language,
            @RequestParam(required = false, defaultValue = "10") int limit) {

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("word", word);
        requestData.put("language", language);
        requestData.put("limit", limit);
        printRequest(requestData);

        try {
            // 1. 验证请求数据
            if (word == null || word.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new WordExamplesResponse(false, "单词不能为空", null)
                );
            }

            if (limit <= 0 || limit > 50) {
                limit = 10;
            }

            String normalizedWord = word.trim().toLowerCase();

            // 2. 查询单词ID
            String wordSql = "SELECT word_id FROM words WHERE word = ? AND language = ?";
            List<Map<String, Object>> words = jdbcTemplate.queryForList(wordSql, normalizedWord, language);

            if (words.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new WordExamplesResponse(false, "未找到单词: " + normalizedWord, null)
                );
            }

            Long wordId = ((Number) words.get(0).get("word_id")).longValue();

            // 3. 查询例句
            String examplesSql = "SELECT example_id, example, translation, source " +
                    "FROM word_examples WHERE word_id = ? " +
                    "ORDER BY example_id " +
                    "LIMIT ?";

            List<Map<String, Object>> exampleResults = jdbcTemplate.queryForList(examplesSql, wordId, limit);
            printQueryResult(exampleResults);

            // 4. 组装数据
            List<Example> examples = new ArrayList<>();
            for (Map<String, Object> row : exampleResults) {
                Long exampleId = ((Number) row.get("example_id")).longValue();
                String exampleText = (String) row.get("example");
                String translation = (String) row.get("translation");
                String source = (String) row.get("source");

                examples.add(new Example(exampleId, exampleText, translation, source));
            }

            // 5. 创建响应数据
            WordExamplesData data = new WordExamplesData(normalizedWord, language, examples);
            WordExamplesResponse response = new WordExamplesResponse(true, "获取单词例句成功", data);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取单词例句过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new WordExamplesResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
