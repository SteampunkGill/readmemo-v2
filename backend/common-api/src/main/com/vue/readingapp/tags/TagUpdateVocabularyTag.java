package com.vue.readingapp.tags;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tags/vocabulary")
public class TagUpdateVocabularyTag {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到更新生词标签请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=========================");
    }

    // 打印返回数据
    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 请求DTO
    public static class UpdateVocabularyTagRequest {
        private String name;
        private String color;
        private String description;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    // 标签响应DTO
    public static class Tag {
        private String id;
        private String name;
        private String color;
        private String description;
        private String type;
        private int vocabularyCount;
        private String updatedAt;

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

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    // 响应类
    public static class UpdateVocabularyTagResponse {
        private boolean success;
        private Tag data;

        public UpdateVocabularyTagResponse(boolean success, Tag data) {
            this.success = success;
            this.data = data;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public Tag getData() { return data; }
        public void setData(Tag data) { this.data = data; }
    }

    @PutMapping("/{tagId}")
    public ResponseEntity<UpdateVocabularyTagResponse> updateVocabularyTag(
            @PathVariable("tagId") String tagId,
            @RequestBody UpdateVocabularyTagRequest request) {

        Map<String, Object> requestData = new java.util.HashMap<>();
        requestData.put("tagId", tagId);
        requestData.put("request", request);
        printRequest(requestData);

        try {
            // 验证请求数据
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UpdateVocabularyTagResponse(false, null)
                );
            }

            // 检查标签是否存在
            String checkSql = "SELECT COUNT(*) FROM vocabulary_tags WHERE tag_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, Integer.parseInt(tagId));
            if (count == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdateVocabularyTagResponse(false, null)
                );
            }

            // 检查新名称是否与其他标签冲突
            String nameCheckSql = "SELECT COUNT(*) FROM vocabulary_tags WHERE tag_name = ? AND tag_id != ?";
            int nameCount = jdbcTemplate.queryForObject(nameCheckSql, Integer.class,
                    request.getName(), Integer.parseInt(tagId));

            if (nameCount > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        new UpdateVocabularyTagResponse(false, null)
                );
            }

            // 更新生词标签
            LocalDateTime now = LocalDateTime.now();
            Timestamp timestamp = Timestamp.valueOf(now);

            String updateSql = "UPDATE vocabulary_tags SET tag_name = ?, color = ?, description = ?, updated_at = ? WHERE tag_id = ?";
            int updated = jdbcTemplate.update(updateSql,
                    request.getName(),
                    request.getColor(),
                    request.getDescription(),
                    timestamp,
                    Integer.parseInt(tagId)
            );

            if (updated == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdateVocabularyTagResponse(false, null)
                );
            }

            // 查询更新后的标签
            String selectSql = "SELECT * FROM vocabulary_tags WHERE tag_id = ?";
            Map<String, Object> tagMap = jdbcTemplate.queryForMap(selectSql, Integer.parseInt(tagId));

            // 查询词汇数量
            String countSql = "SELECT COUNT(*) FROM user_vocabulary_tags WHERE tag_id = ?";
            int vocabCount = jdbcTemplate.queryForObject(countSql, Integer.class, Integer.parseInt(tagId));

            // 构建返回的标签对象
            Tag tag = new Tag();
            tag.setId(String.valueOf(tagMap.get("tag_id")));
            tag.setName((String) tagMap.get("tag_name"));
            tag.setColor((String) tagMap.get("color"));
            tag.setDescription((String) tagMap.get("description"));
            tag.setType("vocabulary");
            tag.setVocabularyCount(vocabCount);
            tag.setUpdatedAt(tagMap.get("updated_at").toString());

            // 创建响应
            UpdateVocabularyTagResponse response = new UpdateVocabularyTagResponse(true, tag);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            System.err.println("标签ID格式错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(
                    new UpdateVocabularyTagResponse(false, null)
            );
        } catch (Exception e) {
            System.err.println("更新生词标签过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateVocabularyTagResponse(false, null)
            );
        }
    }
}
