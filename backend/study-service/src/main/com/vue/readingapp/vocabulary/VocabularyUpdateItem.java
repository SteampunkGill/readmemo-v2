package com.vue.readingapp.vocabulary;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/vocabulary")
public class VocabularyUpdateItem {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到更新生词请求 ===");
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

    // 请求DTO
    public static class UpdateVocabularyRequest {
        private String status;
        private Integer masteryLevel;
        private List<String> tags;
        private String notes;
        private String definition;
        private String example;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Integer getMasteryLevel() { return masteryLevel; }
        public void setMasteryLevel(Integer masteryLevel) { this.masteryLevel = masteryLevel; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }

        public String getExample() { return example; }
        public void setExample(String example) { this.example = example; }
    }

    // 响应DTO
    public static class UpdateVocabularyResponse {
        private boolean success;
        private String message;
        private VocabularyItemData data;

        public UpdateVocabularyResponse(boolean success, String message, VocabularyItemData data) {
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
    }

    @PutMapping("/{userVocabId}")
    public ResponseEntity<UpdateVocabularyResponse> updateVocabularyItem(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long userVocabId,
            @RequestBody UpdateVocabularyRequest request) {

        printRequest("userVocabId: " + userVocabId + ", request: " + request);

        try {
            // 1. 验证用户身份
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateVocabularyResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > ?";
            LocalDateTime now = LocalDateTime.now();
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(userSql, token, now);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateVocabularyResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Long userId = ((Number) sessions.get(0).get("user_id")).longValue();

            // 2. 检查生词是否存在且属于当前用户
            String checkSql = "SELECT user_vocab_id FROM user_vocabulary WHERE user_vocab_id = ? AND user_id = ?";
            List<Map<String, Object>> existingItems = jdbcTemplate.queryForList(checkSql, userVocabId, userId);

            if (existingItems.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdateVocabularyResponse(false, "生词不存在或没有权限更新", null)
                );
            }

            // 3. 构建更新字段
            Map<String, Object> updateFields = new HashMap<>();
            List<Object> params = new ArrayList<>();
            List<String> setClauses = new ArrayList<>();

            if (request.getStatus() != null) {
                setClauses.add("status = ?");
                params.add(request.getStatus());
            }

            if (request.getMasteryLevel() != null) {
                setClauses.add("mastery_level = ?");
                params.add(request.getMasteryLevel());
            }

            if (request.getNotes() != null) {
                setClauses.add("notes = ?");
                params.add(request.getNotes());
            }

            if (request.getDefinition() != null) {
                setClauses.add("definition = ?");
                params.add(request.getDefinition());
            }

            if (request.getExample() != null) {
                setClauses.add("example = ?");
                params.add(request.getExample());
            }

            // 添加更新时间
            setClauses.add("updated_at = ?");
            params.add(LocalDateTime.now());

            // 如果没有要更新的字段
            if (setClauses.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UpdateVocabularyResponse(false, "没有提供更新数据", null)
                );
            }

            // 4. 执行更新
            String updateSql = "UPDATE user_vocabulary SET " + String.join(", ", setClauses) +
                    " WHERE user_vocab_id = ? AND user_id = ?";
            params.add(userVocabId);
            params.add(userId);

            int updatedRows = jdbcTemplate.update(updateSql, params.toArray());
            printQueryResult("更新行数: " + updatedRows);

            if (updatedRows == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new UpdateVocabularyResponse(false, "更新失败", null)
                );
            }

            // 5. 处理标签更新
            if (request.getTags() != null) {
                // 删除旧的标签关联
                String deleteTagsSql = "DELETE FROM user_vocabulary_tags WHERE user_vocab_id = ?";
                jdbcTemplate.update(deleteTagsSql, userVocabId);

                // 添加新的标签关联
                for (String tagName : request.getTags()) {
                    if (tagName != null && !tagName.trim().isEmpty()) {
                        // 查找或创建标签
                        String tagSql = "SELECT tag_id FROM vocabulary_tags WHERE tag_name = ?";
                        List<Map<String, Object>> tags = jdbcTemplate.queryForList(tagSql, tagName.trim());

                        Long tagId;
                        if (tags.isEmpty()) {
                            // 创建新标签
                            String insertTagSql = "INSERT INTO vocabulary_tags (tag_name, created_at) VALUES (?, ?)";
                            KeyHolder tagKeyHolder = new GeneratedKeyHolder();

                            jdbcTemplate.update(connection -> {
                                PreparedStatement ps = connection.prepareStatement(insertTagSql, Statement.RETURN_GENERATED_KEYS);
                                ps.setString(1, tagName.trim());
                                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                                return ps;
                            }, tagKeyHolder);

                            tagId = tagKeyHolder.getKey().longValue();
                        } else {
                            tagId = ((Number) tags.get(0).get("tag_id")).longValue();
                        }

                        // 关联标签
                        String relationSql = "INSERT INTO user_vocabulary_tags (user_vocab_id, tag_id, created_at) VALUES (?, ?, ?)";
                        jdbcTemplate.update(relationSql, userVocabId, tagId, LocalDateTime.now());
                    }
                }
            }

            // 6. 查询更新后的生词详情
            String detailSql = "SELECT uv.user_vocab_id, uv.word, uv.language, uv.definition, uv.example, uv.notes, " +
                    "uv.status, uv.mastery_level, uv.review_count, uv.last_reviewed_at, uv.next_review_at, " +
                    "uv.source, uv.source_page, uv.created_at, uv.updated_at, " +
                    "GROUP_CONCAT(DISTINCT vt.tag_name) as tags " +
                    "FROM user_vocabulary uv " +
                    "LEFT JOIN user_vocabulary_tags uvt ON uv.user_vocab_id = uvt.user_vocab_id " +
                    "LEFT JOIN vocabulary_tags vt ON uvt.tag_id = vt.tag_id " +
                    "WHERE uv.user_vocab_id = ? AND uv.user_id = ? " +
                    "GROUP BY uv.user_vocab_id";

            List<Map<String, Object>> details = jdbcTemplate.queryForList(detailSql, userVocabId, userId);

            if (details.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new UpdateVocabularyResponse(false, "更新成功但查询详情失败", null)
                );
            }

            Map<String, Object> detail = details.get(0);

            // 7. 组装响应数据
            VocabularyItemData itemData = new VocabularyItemData();
            itemData.setId(userVocabId);
            itemData.setWord((String) detail.get("word"));
            itemData.setLanguage((String) detail.get("language"));
            itemData.setDefinition((String) detail.get("definition"));
            itemData.setExample((String) detail.get("example"));
            itemData.setNotes((String) detail.get("notes"));
            itemData.setStatus((String) detail.get("status"));
            itemData.setMasteryLevel(detail.get("mastery_level") != null ? ((Number) detail.get("mastery_level")).intValue() : 0);
            itemData.setReviewCount(detail.get("review_count") != null ? ((Number) detail.get("review_count")).intValue() : 0);
            itemData.setLastReviewedAt(detail.get("last_reviewed_at") != null ? detail.get("last_reviewed_at").toString() : null);
            itemData.setNextReviewAt(detail.get("next_review_at") != null ? detail.get("next_review_at").toString() : null);
            itemData.setSource((String) detail.get("source"));
            itemData.setSourcePage(detail.get("source_page") != null ? ((Number) detail.get("source_page")).intValue() : null);
            itemData.setCreatedAt(detail.get("created_at").toString());
            itemData.setUpdatedAt(detail.get("updated_at").toString());

            // 处理标签字符串
            String tagsStr = (String) detail.get("tags");
            if (tagsStr != null && !tagsStr.isEmpty()) {
                itemData.setTags(Arrays.asList(tagsStr.split(",")));
            }

            // 8. 创建响应
            UpdateVocabularyResponse response = new UpdateVocabularyResponse(true, "生词更新成功", itemData);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新生词过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateVocabularyResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}