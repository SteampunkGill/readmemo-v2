
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
public class VocabularyLookupWord {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到单词查询请求 ===");
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

    // 请求DTO（内部类）
    public static class LookupRequest {
        private String word;
        private String language;

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }

    // 响应DTO（内部类）
    public static class LookupResponse {
        private boolean success;
        private String message;
        private WordData data;

        public LookupResponse(boolean success, String message, WordData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public WordData getData() { return data; }
        public void setData(WordData data) { this.data = data; }
    }

    public static class WordData {
        private String word;
        private String language;
        private String phonetic;
        private List<String> definitions;
        private List<String> examples;
        private List<String> synonyms;
        private List<String> antonyms;
        private String partOfSpeech;
        private int frequency;
        private String difficulty;
        private String audioUrl;
        private String source;
        private Map<String, Object> metadata;

        public WordData() {
            this.definitions = new ArrayList<>();
            this.examples = new ArrayList<>();
            this.synonyms = new ArrayList<>();
            this.antonyms = new ArrayList<>();
            this.metadata = new HashMap<>();
        }

        // Getters and Setters
        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getPhonetic() { return phonetic; }
        public void setPhonetic(String phonetic) { this.phonetic = phonetic; }

        public List<String> getDefinitions() { return definitions; }
        public void setDefinitions(List<String> definitions) { this.definitions = definitions; }

        public List<String> getExamples() { return examples; }
        public void setExamples(List<String> examples) { this.examples = examples; }

        public List<String> getSynonyms() { return synonyms; }
        public void setSynonyms(List<String> synonyms) { this.synonyms = synonyms; }

        public List<String> getAntonyms() { return antonyms; }
        public void setAntonyms(List<String> antonyms) { this.antonyms = antonyms; }

        public String getPartOfSpeech() { return partOfSpeech; }
        public void setPartOfSpeech(String partOfSpeech) { this.partOfSpeech = partOfSpeech; }

        public int getFrequency() { return frequency; }
        public void setFrequency(int frequency) { this.frequency = frequency; }

        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

        public String getAudioUrl() { return audioUrl; }
        public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    @GetMapping("/lookup")
    public ResponseEntity<LookupResponse> lookupWord(
            @RequestParam String word,
            @RequestParam(required = false, defaultValue = "en") String language) {

        // 创建请求对象用于打印
        Map<String, String> requestData = new HashMap<>();
        requestData.put("word", word);
        requestData.put("language", language);
        printRequest(requestData);

        try {
            // 1. 验证请求数据
            if (word == null || word.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new LookupResponse(false, "单词不能为空", null)
                );
            }

            String normalizedWord = word.trim().toLowerCase();

            // 2. 查询单词基本信息
            String wordSql = "SELECT word_id, word, language, phonetic, part_of_speech, frequency, difficulty, audio_url, created_at " +
                    "FROM words WHERE word = ? AND language = ?";

            List<Map<String, Object>> words = jdbcTemplate.queryForList(wordSql, normalizedWord, language);
            printQueryResult(words);

            if (words.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new LookupResponse(false, "未找到单词: " + normalizedWord, null)
                );
            }

            Map<String, Object> wordInfo = words.get(0);
            Long wordId = ((Number) wordInfo.get("word_id")).longValue();

            // 3. 查询单词释义
            String definitionsSql = "SELECT definition_id, definition, example FROM word_definitions WHERE word_id = ? ORDER BY definition_id";
            List<Map<String, Object>> definitionsList = jdbcTemplate.queryForList(definitionsSql, wordId);

            // 4. 查询单词例句
            String examplesSql = "SELECT example_id, example, translation FROM word_examples WHERE word_id = ? ORDER BY example_id LIMIT 10";
            List<Map<String, Object>> examplesList = jdbcTemplate.queryForList(examplesSql, wordId);

            // 5. 查询单词关系（同义词和反义词）
            String relationsSql = "SELECT wr.relation_type, w.word, w.language " +
                    "FROM word_relations wr " +
                    "JOIN words w ON wr.related_word_id = w.word_id " +
                    "WHERE wr.word_id = ? AND wr.relation_type IN ('synonym', 'antonym')";
            List<Map<String, Object>> relationsList = jdbcTemplate.queryForList(relationsSql, wordId);

            // 6. 组装响应数据
            WordData wordData = new WordData();
            wordData.setWord((String) wordInfo.get("word"));
            wordData.setLanguage((String) wordInfo.get("language"));
            wordData.setPhonetic((String) wordInfo.get("phonetic"));
            wordData.setPartOfSpeech((String) wordInfo.get("part_of_speech"));
            wordData.setFrequency(wordInfo.get("frequency") != null ? ((Number) wordInfo.get("frequency")).intValue() : 0);
            wordData.setDifficulty((String) wordInfo.get("difficulty"));
            wordData.setAudioUrl((String) wordInfo.get("audio_url"));
            wordData.setSource("dictionary");

            // 添加释义
            List<String> definitions = new ArrayList<>();
            for (Map<String, Object> def : definitionsList) {
                String definition = (String) def.get("definition");
                if (definition != null && !definition.trim().isEmpty()) {
                    definitions.add(definition);
                }
            }
            wordData.setDefinitions(definitions);

            // 添加例句（从释义和例句表中合并）
            Set<String> exampleSet = new LinkedHashSet<>();

            // 从释义表中获取例句
            for (Map<String, Object> def : definitionsList) {
                String example = (String) def.get("example");
                if (example != null && !example.trim().isEmpty()) {
                    exampleSet.add(example);
                }
            }

            // 从例句表中获取例句
            for (Map<String, Object> ex : examplesList) {
                String example = (String) ex.get("example");
                if (example != null && !example.trim().isEmpty()) {
                    exampleSet.add(example);
                }
            }

            wordData.setExamples(new ArrayList<>(exampleSet));

            // 添加同义词和反义词
            List<String> synonyms = new ArrayList<>();
            List<String> antonyms = new ArrayList<>();

            for (Map<String, Object> relation : relationsList) {
                String relationType = (String) relation.get("relation_type");
                String relatedWord = (String) relation.get("word");

                if ("synonym".equals(relationType) && relatedWord != null) {
                    synonyms.add(relatedWord);
                } else if ("antonym".equals(relationType) && relatedWord != null) {
                    antonyms.add(relatedWord);
                }
            }

            wordData.setSynonyms(synonyms);
            wordData.setAntonyms(antonyms);

            // 添加元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("word_id", wordId);
            metadata.put("created_at", wordInfo.get("created_at"));
            metadata.put("definition_count", definitions.size());
            metadata.put("example_count", exampleSet.size());
            metadata.put("synonym_count", synonyms.size());
            metadata.put("antonym_count", antonyms.size());
            wordData.setMetadata(metadata);

            // 7. 创建响应
            LookupResponse response = new LookupResponse(true, "单词查询成功", wordData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("单词查询过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new LookupResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}