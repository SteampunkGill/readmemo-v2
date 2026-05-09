package com.vue.readingapp.reader;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderLookupWord {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到查询单词请求 ===");
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
    // 辅助方法：不区分大小写地从Map中获取字符串
    private String getCaseInsensitive(Map<String, Object> map, String key) {
        if (map == null) return null;
        if (map.containsKey(key)) return (String) map.get(key);
        if (map.containsKey(key.toUpperCase())) return (String) map.get(key.toUpperCase());
        if (map.containsKey(key.toLowerCase())) return (String) map.get(key.toLowerCase());
        return null;
    }

    // 辅助方法：不区分大小写地从Map中获取对象
    private Object getCaseInsensitiveObject(Map<String, Object> map, String key) {
        if (map == null) return null;
        if (map.containsKey(key)) return map.get(key);
        if (map.containsKey(key.toUpperCase())) return map.get(key.toUpperCase());
        if (map.containsKey(key.toLowerCase())) return map.get(key.toLowerCase());
        return null;
    }


    // 响应DTO
    public static class LookupWordResponse {
        private boolean success;
        private String message;
        private LookupData data;

        public LookupWordResponse(boolean success, String message, LookupData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public LookupData getData() { return data; }
        public void setData(LookupData data) { this.data = data; }
    }

    public static class LookupData {
        private String word;
        private String language;
        private List<Definition> definitions;
        private List<Example> examples;
        private List<RelatedWord> relatedWords;

        public LookupData() {
            this.definitions = new ArrayList<>();
            this.examples = new ArrayList<>();
            this.relatedWords = new ArrayList<>();
        }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public List<Definition> getDefinitions() { return definitions; }
        public void setDefinitions(List<Definition> definitions) { this.definitions = definitions; }

        public List<Example> getExamples() { return examples; }
        public void setExamples(List<Example> examples) { this.examples = examples; }

        public List<RelatedWord> getRelatedWords() { return relatedWords; }
        public void setRelatedWords(List<RelatedWord> relatedWords) { this.relatedWords = relatedWords; }
    }

    public static class Definition {
        private int id;
        private String partOfSpeech;
        private String definition;
        private String example;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getPartOfSpeech() { return partOfSpeech; }
        public void setPartOfSpeech(String partOfSpeech) { this.partOfSpeech = partOfSpeech; }

        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }

        public String getExample() { return example; }
        public void setExample(String example) { this.example = example; }
    }

    public static class Example {
        private int id;
        private String example;
        private String translation;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getExample() { return example; }
        public void setExample(String example) { this.example = example; }

        public String getTranslation() { return translation; }
        public void setTranslation(String translation) { this.translation = translation; }
    }

    public static class RelatedWord {
        private int id;
        private String word;
        private String relationship;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getRelationship() { return relationship; }
        public void setRelationship(String relationship) { this.relationship = relationship; }
    }

    @GetMapping("/dictionary/lookup")
    public ResponseEntity<LookupWordResponse> lookupWord(
            @RequestParam("word") String word,
            @RequestParam(value = "language", defaultValue = "en") String language,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("word", word);
        requestInfo.put("language", language);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new LookupWordResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new LookupWordResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 验证请求数据
            if (word == null || word.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new LookupWordResponse(false, "查询的单词不能为空", null)
                );
            }

            // 3. 查询单词基本信息
            String wordSql = "SELECT * FROM words WHERE word = ? AND language = ?";
            List<Map<String, Object>> words = jdbcTemplate.queryForList(wordSql, word.trim().toLowerCase(), language);

            if (words.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new LookupWordResponse(false, "单词未找到", null)
                );
            }

            Map<String, Object> wordInfo = words.get(0);
            int wordId = ((Number) wordInfo.get("word_id")).intValue();

            // 4. 查询单词释义
            String definitionSql = "SELECT * FROM word_definitions WHERE word_id = ? ORDER BY order_index";
            List<Map<String, Object>> definitions = jdbcTemplate.queryForList(definitionSql, wordId);

            // 5. 查询单词例句
            String exampleSql = "SELECT * FROM word_examples WHERE word_id = ? ORDER BY order_index";
            List<Map<String, Object>> examples = jdbcTemplate.queryForList(exampleSql, wordId);

            // 6. 查询相关单词
            String relatedSql = "SELECT wr.*, w.word as related_word FROM word_relations wr " +
                    "JOIN words w ON wr.related_word_id = w.word_id " +
                    "WHERE wr.word_id = ?";
            List<Map<String, Object>> relatedWords = jdbcTemplate.queryForList(relatedSql, wordId);

            printQueryResult("单词ID: " + wordId + ", 释义数: " + definitions.size() +
                    ", 例句数: " + examples.size() + ", 相关词数: " + relatedWords.size());

            // 7. 构建响应数据
            LookupData lookupData = new LookupData();
            lookupData.setWord(word);
            lookupData.setLanguage(language);

            // 设置释义
            if (definitions.isEmpty()) {
                // 强健的字段读取逻辑，处理可能的大小写问题并尝试多个备选字段
                String translation = getCaseInsensitive(wordInfo, "translation_pos");
                if (translation == null || translation.isEmpty()) translation = getCaseInsensitive(wordInfo, "detail");
                if (translation == null || translation.isEmpty()) translation = getCaseInsensitive(wordInfo, "collins");
                
                if (translation != null && !translation.isEmpty()) {
                    Definition def = new Definition();
                    def.setId(0);
                    def.setPartOfSpeech(getCaseInsensitive(wordInfo, "part_of_speech"));
                    def.setDefinition(translation);
                    lookupData.getDefinitions().add(def);
                }
            } else {
                for (Map<String, Object> definition : definitions) {
                    Definition def = new Definition();
                    def.setId(((Number) getCaseInsensitiveObject(definition, "definition_id")).intValue());
                    def.setPartOfSpeech(getCaseInsensitive(wordInfo, "part_of_speech"));
                    def.setDefinition(getCaseInsensitive(definition, "definition"));
                    def.setExample(getCaseInsensitive(definition, "example"));
                    lookupData.getDefinitions().add(def);
                }
            }

            // 设置例句
            for (Map<String, Object> example : examples) {
                Example ex = new Example();
                ex.setId(((Number) example.get("example_id")).intValue());
                // 数据库字段名是 example_sentence
                String exampleText = (String) example.get("example_sentence");
                if (exampleText == null) exampleText = (String) example.get("example");
                ex.setExample(exampleText);
                ex.setTranslation((String) example.get("translation"));
                lookupData.getExamples().add(ex);
            }

            // 设置相关单词
            for (Map<String, Object> relatedWord : relatedWords) {
                RelatedWord rw = new RelatedWord();
                rw.setId(((Number) relatedWord.get("relation_id")).intValue());
                rw.setWord((String) relatedWord.get("related_word"));
                rw.setRelationship((String) relatedWord.get("relationship_type"));
                lookupData.getRelatedWords().add(rw);
            }

            // 8. 记录查询历史
            String historySql = "INSERT INTO word_lookup_history (user_id, word_id, created_at) VALUES (?, ?, NOW())";
            jdbcTemplate.update(historySql, userId, wordId);

            LookupWordResponse response = new LookupWordResponse(true, "单词查询成功", lookupData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("查询单词过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new LookupWordResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}