
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
public class VocabularyGetItem {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取生词详情请求 ===");
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
    public static class VocabularyItemResponse {
        private boolean success;
        private String message;
        private VocabularyItemData data;

        public VocabularyItemResponse(boolean success, String message, VocabularyItemData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public VocabularyItemData getData() { return data; }
        public void setData(VocabularyItemData data) { this.data = data; }
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
        private WordDetailData wordDetail;

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

        public WordDetailData getWordDetail() { return wordDetail; }
        public void setWordDetail(WordDetailData wordDetail) { this.wordDetail = wordDetail; }
    }

    public static class WordDetailData {
        private String phonetic;
        private List<String> definitions;
        private List<String> examples;
        private List<String> synonyms;
        private List<String> antonyms;
        private String partOfSpeech;
        private Integer frequency;
        private String difficulty;
        private String audioUrl;

        public WordDetailData() {
            this.definitions = new ArrayList<>();
            this.examples = new ArrayList<>();
            this.synonyms = new ArrayList<>();
            this.antonyms = new ArrayList<>();
        }

        // Getters and Setters
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

        public Integer getFrequency() { return frequency; }
        public void setFrequency(Integer frequency) { this.frequency = frequency; }

        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

        public String getAudioUrl() { return audioUrl; }
        public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    }

    @GetMapping("/{userVocabId}")
    public ResponseEntity<VocabularyItemResponse> getVocabularyItem(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long userVocabId) {

        printRequest("userVocabId: " + userVocabId);

        try {
            // 1. 验证用户身份
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new VocabularyItemResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > ?";
            LocalDateTime now = LocalDateTime.now();
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(userSql, token, now);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new VocabularyItemResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Long userId = ((Number) sessions.get(0).get("user_id")).longValue();

            // 2. 查询生词详情
            String sql = "SELECT uv.user_vocab_id, uv.word, uv.language, uv.definition, uv.example, uv.notes, " +
                    "uv.status, uv.mastery_level, uv.review_count, uv.last_reviewed_at, uv.next_review_at, " +
                    "uv.source, uv.source_page, uv.created_at, uv.updated_at, " +
                    "GROUP_CONCAT(DISTINCT vt.tag_name) as tags " +
                    "FROM user_vocabulary uv " +
                    "LEFT JOIN user_vocabulary_tags uvt ON uv.user_vocab_id = uvt.user_vocab_id " +
                    "LEFT JOIN vocabulary_tags vt ON uvt.tag_id = vt.tag_id " +
                    "WHERE uv.user_vocab_id = ? AND uv.user_id = ? " +
                    "GROUP BY uv.user_vocab_id";

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userVocabId, userId);
            printQueryResult(results);

            if (results.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new VocabularyItemResponse(false, "生词不存在或没有权限访问", null)
                );
            }

            Map<String, Object> row = results.get(0);

            // 3. 组装生词数据
            VocabularyItemData itemData = new VocabularyItemData();
            itemData.setId(((Number) row.get("user_vocab_id")).longValue());
            itemData.setWord((String) row.get("word"));
            itemData.setLanguage((String) row.get("language"));
            itemData.setDefinition((String) row.get("definition"));
            itemData.setExample((String) row.get("example"));
            itemData.setNotes((String) row.get("notes"));
            itemData.setStatus((String) row.get("status"));
            itemData.setMasteryLevel(row.get("mastery_level") != null ? ((Number) row.get("mastery_level")).intValue() : 0);
            itemData.setReviewCount(row.get("review_count") != null ? ((Number) row.get("review_count")).intValue() : 0);
            itemData.setLastReviewedAt(row.get("last_reviewed_at") != null ? row.get("last_reviewed_at").toString() : null);
            itemData.setNextReviewAt(row.get("next_review_at") != null ? row.get("next_review_at").toString() : null);
            itemData.setSource((String) row.get("source"));
            itemData.setSourcePage(row.get("source_page") != null ? ((Number) row.get("source_page")).intValue() : null);
            itemData.setCreatedAt(row.get("created_at").toString());
            itemData.setUpdatedAt(row.get("updated_at").toString());

            // 处理标签
            String tagsStr = (String) row.get("tags");
            if (tagsStr != null && !tagsStr.isEmpty()) {
                itemData.setTags(Arrays.asList(tagsStr.split(",")));
            }

            // 4. 查询单词的详细信息（从words表）
            String word = (String) row.get("word");
            String language = (String) row.get("language");

            String wordDetailSql = "SELECT w.word_id, w.phonetic, w.part_of_speech, w.frequency, w.difficulty, w.audio_url " +
                    "FROM words w WHERE w.word = ? AND w.language = ?";

            List<Map<String, Object>> wordDetails = jdbcTemplate.queryForList(wordDetailSql, word, language);

            if (!wordDetails.isEmpty()) {
                Map<String, Object> wordDetailRow = wordDetails.get(0);
                Long wordId = ((Number) wordDetailRow.get("word_id")).longValue();

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

                // 组装单词详情
                WordDetailData wordDetail = new WordDetailData();
                wordDetail.setPhonetic((String) wordDetailRow.get("phonetic"));
                wordDetail.setDefinitions(definitions);
                wordDetail.setExamples(examples);
                wordDetail.setSynonyms(synonyms);
                wordDetail.setAntonyms(antonyms);
                wordDetail.setPartOfSpeech((String) wordDetailRow.get("part_of_speech"));
                wordDetail.setFrequency(wordDetailRow.get("frequency") != null ? ((Number) wordDetailRow.get("frequency")).intValue() : 0);
                wordDetail.setDifficulty((String) wordDetailRow.get("difficulty"));
                wordDetail.setAudioUrl((String) wordDetailRow.get("audio_url"));

                itemData.setWordDetail(wordDetail);
            }

            // 5. 创建响应
            VocabularyItemResponse response = new VocabularyItemResponse(true, "获取生词详情成功", itemData);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取生词详情过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new VocabularyItemResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}