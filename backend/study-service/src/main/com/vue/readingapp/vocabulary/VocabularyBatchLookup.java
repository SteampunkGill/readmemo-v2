
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
public class VocabularyBatchLookup {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到批量单词查询请求 ===");
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

    // 请求DTO
    public static class BatchLookupRequest {
        private List<String> words;
        private String language;

        public List<String> getWords() { return words; }
        public void setWords(List<String> words) { this.words = words; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }

    // 响应DTO
    public static class BatchLookupResponse {
        private boolean success;
        private String message;
        private List<WordData> data;

        public BatchLookupResponse(boolean success, String message, List<WordData> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<WordData> getData() { return data; }
        public void setData(List<WordData> data) { this.data = data; }
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

    @PostMapping("/batch-lookup")
    public ResponseEntity<BatchLookupResponse> batchLookupWords(@RequestBody BatchLookupRequest request) {
        printRequest(request);

        try {
            // 1. 验证请求数据
            if (request.getWords() == null || request.getWords().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchLookupResponse(false, "单词列表不能为空", null)
                );
            }

            if (request.getWords().size() > 50) {
                return ResponseEntity.badRequest().body(
                        new BatchLookupResponse(false, "批量查询最多支持50个单词", null)
                );
            }

            String language = request.getLanguage() != null ? request.getLanguage() : "en";

            // 2. 去重和规范化单词
            Set<String> uniqueWords = new LinkedHashSet<>();
            for (String word : request.getWords()) {
                if (word != null && !word.trim().isEmpty()) {
                    uniqueWords.add(word.trim().toLowerCase());
                }
            }

            if (uniqueWords.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchLookupResponse(false, "没有有效的单词", null)
                );
            }

            // 3. 批量查询单词
            List<WordData> result = new ArrayList<>();

            for (String word : uniqueWords) {
                try {
                    // 查询单词基本信息
                    String wordSql = "SELECT word_id, word, language, phonetic, part_of_speech, frequency, difficulty, audio_url " +
                            "FROM words WHERE word = ? AND language = ?";

                    List<Map<String, Object>> words = jdbcTemplate.queryForList(wordSql, word, language);

                    if (!words.isEmpty()) {
                        Map<String, Object> wordInfo = words.get(0);
                        Long wordId = ((Number) wordInfo.get("word_id")).longValue();

                        // 查询单词释义
                        String definitionsSql = "SELECT definition FROM word_definitions WHERE word_id = ? ORDER BY definition_id";
                        List<String> definitions = jdbcTemplate.queryForList(definitionsSql, String.class, wordId);

                        // 查询单词例句
                        String examplesSql = "SELECT example FROM word_examples WHERE word_id = ? ORDER BY example_id LIMIT 5";
                        List<String> examples = jdbcTemplate.queryForList(examplesSql, String.class, wordId);

                        // 查询同义词
                        String synonymsSql = "SELECT w.word FROM word_relations wr " +
                                "JOIN words w ON wr.related_word_id = w.word_id " +
                                "WHERE wr.word_id = ? AND wr.relation_type = 'synonym' LIMIT 5";
                        List<String> synonyms = jdbcTemplate.queryForList(synonymsSql, String.class, wordId);

                        // 查询反义词
                        String antonymsSql = "SELECT w.word FROM word_relations wr " +
                                "JOIN words w ON wr.related_word_id = w.word_id " +
                                "WHERE wr.word_id = ? AND wr.relation_type = 'antonym' LIMIT 5";
                        List<String> antonyms = jdbcTemplate.queryForList(antonymsSql, String.class, wordId);

                        // 组装单词数据
                        WordData wordData = new WordData();
                        wordData.setWord((String) wordInfo.get("word"));
                        wordData.setLanguage((String) wordInfo.get("language"));
                        wordData.setPhonetic((String) wordInfo.get("phonetic"));
                        wordData.setPartOfSpeech((String) wordInfo.get("part_of_speech"));
                        wordData.setFrequency(wordInfo.get("frequency") != null ? ((Number) wordInfo.get("frequency")).intValue() : 0);
                        wordData.setDifficulty((String) wordInfo.get("difficulty"));
                        wordData.setAudioUrl((String) wordInfo.get("audio_url"));
                        wordData.setSource("dictionary");
                        wordData.setDefinitions(definitions);
                        wordData.setExamples(examples);
                        wordData.setSynonyms(synonyms);
                        wordData.setAntonyms(antonyms);

                        // 添加元数据
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("word_id", wordId);
                        metadata.put("definition_count", definitions.size());
                        metadata.put("example_count", examples.size());
                        wordData.setMetadata(metadata);

                        result.add(wordData);
                    } else {
                        // 单词不存在，创建空的单词数据
                        WordData wordData = new WordData();
                        wordData.setWord(word);
                        wordData.setLanguage(language);
                        wordData.setSource("not_found");

                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("found", false);
                        wordData.setMetadata(metadata);

                        result.add(wordData);
                    }
                } catch (Exception e) {
                    System.err.println("查询单词 " + word + " 时发生错误: " + e.getMessage());
                    // 继续处理其他单词
                }
            }

            printQueryResult("共查询 " + result.size() + " 个单词");

            // 4. 创建响应
            BatchLookupResponse response = new BatchLookupResponse(true, "批量单词查询完成", result);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("批量单词查询过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BatchLookupResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}