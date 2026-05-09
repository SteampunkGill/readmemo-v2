package com.vue.readingapp.tags;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/tags/vocabulary")
public class TagGetVocabularyTagById {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取生词标签详情请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=============================");
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

    // 词汇DTO
    public static class Vocabulary {
        private String id;
        private String word;
        private String createdAt;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    // 标签响应DTO
    public static class Tag {
        private String id;
        private String name;
        private String color;
        private String description;
        private String type;
        private int vocabularyCount;
        private String createdAt;
        private String updatedAt;
        private List<Vocabulary> vocabularies;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public int getVocabularyCount() { return vocabularyCount; }
        public void setVocabularyCount(int vocabularyCount) { this.vocabularyCount = vocabularyCount; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public List<Vocabulary> getVocabularies() { return vocabularies; }
        public void setVocabularies(List<Vocabulary> vocabularies) { this.vocabularies = vocabularies; }
    }

    // 响应类
    public static class VocabularyTagDetailResponse {
        private boolean success;
        private Tag data;

        public VocabularyTagDetailResponse(boolean success, Tag data) {
            this.success = success;
            this.data = data;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public Tag getData() { return data; }
        public void setData(Tag data) { this.data = data; }
    }

    @GetMapping("/{tagId}")
    public ResponseEntity<VocabularyTagDetailResponse> getVocabularyTagById(@PathVariable("tagId") String tagId) {
        printRequest(tagId);

        try {
            // 查询生词标签
            String sql = "SELECT * FROM vocabulary_tags WHERE tag_id = ?";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, Integer.parseInt(tagId));

            if (results.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new VocabularyTagDetailResponse(false, null)
                );
            }

            Map<String, Object> tagMap = results.get(0);
            printQueryResult(tagMap);

            // 构建标签对象
            Tag tag = new Tag();
            tag.setId(String.valueOf(tagMap.get("tag_id")));
            tag.setName((String) tagMap.get("tag_name"));
            tag.setColor((String) tagMap.get("color"));
            tag.setDescription((String) tagMap.get("description"));
            tag.setType("vocabulary");
            tag.setCreatedAt(tagMap.get("created_at").toString());
            tag.setUpdatedAt(tagMap.get("updated_at").toString());

            // 查询关联的词汇数量
            String countSql = "SELECT COUNT(*) FROM user_vocabulary_tags WHERE tag_id = ?";
            int vocabCount = jdbcTemplate.queryForObject(countSql, Integer.class, Integer.parseInt(tagId));
            tag.setVocabularyCount(vocabCount);

            // 查询关联的词汇详情
            String vocabSql = "SELECT uv.user_vocabulary_id, w.word, uv.created_at " +
                    "FROM user_vocabulary uv " +
                    "JOIN words w ON uv.word_id = w.word_id " +
                    "JOIN user_vocabulary_tags uvt ON uv.user_vocabulary_id = uvt.user_vocabulary_id " +
                    "WHERE uvt.tag_id = ? " +
                    "ORDER BY uv.created_at DESC " +
                    "LIMIT 10";

            List<Map<String, Object>> vocabResults = jdbcTemplate.queryForList(vocabSql, Integer.parseInt(tagId));
            List<Vocabulary> vocabularies = new ArrayList<>();

            for (Map<String, Object> vocabMap : vocabResults) {
                Vocabulary vocab = new Vocabulary();
                vocab.setId(String.valueOf(vocabMap.get("user_vocabulary_id")));
                vocab.setWord((String) vocabMap.get("word"));
                vocab.setCreatedAt(vocabMap.get("created_at").toString());
                vocabularies.add(vocab);
            }

            tag.setVocabularies(vocabularies);

            // 创建响应
            VocabularyTagDetailResponse response = new VocabularyTagDetailResponse(true, tag);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            System.err.println("标签ID格式错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(
                    new VocabularyTagDetailResponse(false, null)
            );
        } catch (Exception e) {
            System.err.println("获取生词标签详情过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new VocabularyTagDetailResponse(false, null)
            );
        }
    }
}
